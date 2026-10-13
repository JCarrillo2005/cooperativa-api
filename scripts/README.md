# Scripts de base de datos · Cooperativa API

Todos estos archivos son de **ejecución manual**: la aplicación Spring Boot **no los ejecuta
automáticamente** (no hay Flyway, Liquibase ni `schema.sql`). La API solo se conecta a Oracle y
hace CRUD; las tablas y los datos deben existir antes de arrancarla.

Se ejecutan con **SQL\*Plus** (o SQL Developer), en una **sesión nueva**, conectado al
**esquema propietario** (`COOPERATIVA`), y **con la API detenida** mientras se modifica estructura.

> Para el detalle fino de la corrección de pagos/liquidaciones, ver `LEEME_FIX_PAGOS.md`.

---

## 0. Cómo conectar SQL\*Plus (en esta máquina)

Oracle XE 10g está en `C:\oraclexe\app\oracle\product\10.2.0\server`. La cadena EZCONNECT
funciona así (el formato `host:puerto:SID` del cliente da `ORA-12545`):

```powershell
$env:ORACLE_HOME = "C:\oraclexe\app\oracle\product\10.2.0\server"
& "$env:ORACLE_HOME\BIN\sqlplus.exe" -S "TU_USUARIO/TU_PASSWORD@//127.0.0.1:1521/XE" @scripts\01_schema_oracle.sql
```

En esta máquina el usuario del esquema es `cooperativa` y su credencial está en
`config.properties` de la raíz (no se escribe aquí; ese archivo está excluido de Git).

---

## Receta A · Instalación nueva (esquema vacío)

Apto para: máquina nueva, esquema recreado, o los entornos QA desechables `TF_QA_*`.

| Paso | Script | Qué hace | Cuándo |
|---|---|---|---|
| 1 | `01_schema_oracle.sql` | Crea las 15 tablas, 15 PK, 19 FK, 14 secuencias y checks | Solo sobre esquema **vacío** |
| 2 | `02_initial_data.sql` | Carga las 38 filas demo (ficticias) | Una sola vez; exige tablas vacías |
| 3 (opcional) | `07_verify_oracle.sql` | Verifica integridad: 75 rechazos + 4 casos válidos | Con la carga demo; revierte todo |

Orden:

```powershell
# 1) DDL (requiere esquema vacío)
& "$env:ORACLE_HOME\BIN\sqlplus.exe" -S "TU_USUARIO/TU_PASSWORD@//127.0.0.1:1521/XE" @scripts\01_schema_oracle.sql

# 2) Carga demo
& "$env:ORACLE_HOME\BIN\sqlplus.exe" -S "TU_USUARIO/TU_PASSWORD@//127.0.0.1:1521/XE" @scripts\02_initial_data.sql

# 3) (Opcional) Verificación
& "$env:ORACLE_HOME\BIN\sqlplus.exe" -S "TU_USUARIO/TU_PASSWORD@//127.0.0.1:1521/XE" @scripts\07_verify_oracle.sql
```

Después de esto ya se puede arrancar la API:

```powershell
mvn spring-boot:run
```

> `01` ya incluye `albaranes.liquidado` y `ck_pre_monto CHECK (monto >= 0)`, así que sobre un
> esquema nuevo **no** se usa `08`.

---

## Receta B · Migrar un esquema antiguo (ya con datos)

Apto para: la BD `COOPERATIVA` que existía antes de la corrección de pagos (sin
`albaranes.liquidado`, con `ck_pre_monto CHECK (monto > 0)`).

1. **Detener la API y cerrar las sesiones** que escriban en el esquema.
2. Abrir una sesión nueva de SQL\*Plus y confirmar el propietario:

   ```sql
   SHOW USER   -- debe decir COOPERATIVA (nunca SYS/SYSTEM)
   ```

3. Ejecutar la migración:

   ```powershell
   & "$env:ORACLE_HOME\BIN\sqlplus.exe" -S "TU_USUARIO/TU_PASSWORD@//127.0.0.1:1521/XE" @scripts\08_fix_pagos_liquidaciones.sql
   ```

4. Revisar la salida: debe verse `PREFLIGHT OK`, luego los `APLICADO: ...` y al final
   `MIGRACION 08 OK`. Si hay un error, **la API debe seguir detenida**, corregir la causa y
   volver a ejecutar el mismo script (reconoce estados intermedios; no reinicia indicadores
   ni vuelve a cargar datos).

### Si el script se niega: "concilie primero LIQUIDADO"

Eso significa que el esquema tiene albaranes y **no** es la copia demo intacta (tiene datos
reales o cambiados) → la migración se detiene *a propósito* para no marcar como pendientes
albaranes que quizá ya se pagaron. Procedimiento documentado en `LEEME_FIX_PAGOS.md`:

```sql
-- Paso A: agregar la columna SIN valor automático
ALTER TABLE albaranes ADD (liquidado NUMBER(1));

-- Paso B: asignar POR ID y CON EVIDENCIA
UPDATE albaranes SET liquidado = 0 WHERE id = 1;   -- pendiente
UPDATE albaranes SET liquidado = 1 WHERE id = 2;   -- ya liquidado (con comprobante)
-- (0 = pendiente, 1 = liquidado; no dejar nulos ni valores distintos de 0/1)

COMMIT;
```

Luego volver a ejecutar `08`; él fijará `DEFAULT 0`, `NOT NULL` y el `CHECK`.

---

## Catálogo de scripts

| Archivo | Rol | ¿Crear BD nueva? | ¿Migrar BD vieja? |
|---|---|---|---|
| `01_schema_oracle.sql` | DDL: 15 tablas + restricciones + secuencias | ✅ (esquema vacío) | ❌ |
| `02_initial_data.sql` | 38 filas demo ficticias | ✅ (una vez, tablas vacías) | ❌ |
| `07_verify_oracle.sql` | Verificación de integridad (75 rechazos + 4 válidos) | ✅ opcional | ❌ |
| `08_fix_pagos_liquidaciones.sql` | Migración: `liquidado` + `monto >= 0` | ❌ | ✅ |
| `LEEME_FIX_PAGOS.md` | Manual del responsable (conciliación, QA, pruebas) | manual | manual |

## Reglas de oro

- **Nunca** ejecutar como `SYS`/`SYSTEM` ni con `CURRENT_SCHEMA` apuntando a otro propietario.
- **Detener la API** antes de cualquier cambio de estructura (`01`, `08`).
- `02` solo en tablas vacías: es una carga, no una migración (y no es ejecutable dos veces).
- Oracle confirma el DDL implícitamente → un error **no revierte** el DDL anterior; por eso
  `08` es reanudable y no se borran ni recargan datos al reintentar.
- Guardar los resultados de las ejecuciones reales como evidencia.
- Los scripts son operación de base de datos **manual**; no forman parte del arranque de la
  aplicación.