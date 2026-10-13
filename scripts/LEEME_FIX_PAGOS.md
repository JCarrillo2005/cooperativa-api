# Corrección de pagos y liquidaciones · Oracle 10g

La API corregida requiere dos cambios en Oracle:

- `prestamos.monto >= 0`: el campo es el saldo pendiente y llega a cero al pagar.
  La creación de un préstamo sigue exigiendo un monto positivo en el controller.
- `albaranes.liquidado NUMBER(1) DEFAULT 0 NOT NULL`, con `CHECK (liquidado IN (0,1))`:
  `0` significa pendiente y `1`, liquidado. La API debe acreditar el saldo y marcar
  este indicador dentro de la misma transacción y con bloqueo del albarán.

Los endpoints y las quince tablas del proyecto se mantienen. El indicador conserva
el estado entre reinicios; el SQL por sí solo no reemplaza la validación transaccional Java.

## Actualizar un esquema existente

1. Conservar un respaldo, detener la API y cerrar las sesiones que escriben en el esquema.
2. Abrir una sesión nueva de SQL*Plus y conectarse al **propietario de las tablas**.
   No usar `SYS`, `SYSTEM` ni cambiar `CURRENT_SCHEMA` para apuntar a otro propietario.
3. Comprobar el usuario y ejecutar el archivo de migración, desde la carpeta que lo contiene:

   ```sql
   SHOW USER
   @08_fix_pagos_liquidaciones.sql
   ```

   Desde la raíz de `cooperativa-api`, la ruta es `@scripts/08_fix_pagos_liquidaciones.sql`.
4. Revisar `PREFLIGHT OK` y `MIGRACION 08 OK`. Si hay un error, mantener la API detenida,
   revisar el mensaje y corregir su causa. Se puede repetir el mismo script: reconoce
   la restricción definitiva, su guardia temporal y una columna ya agregada; no reinicia
   indicadores existentes ni vuelve a cargar datos.
5. Desplegar conjuntamente el backend corregido y comprobar los pagos antes de reabrir el servicio.

El preflight revisa las 15 tablas y sus 68 columnas originales, el tipo de `MONTO`, los
checks anteriores y los valores de `LIQUIDADO` cuando ya existe. Una estructura inesperada
o una restricción deshabilitada provoca un error antes del primer DDL. La sustitución de
`CK_PRE_MONTO` crea primero una guardia `monto >= 0`: los negativos siguen protegidos durante
el cambio. El script no elimina tablas, usuarios ni filas, no resetea secuencias y no paga albaranes.

Oracle confirma DDL implícitamente; **un error no revierte el DDL ya aplicado**. Por eso el
script se puede reanudar y no se presenta como una transacción atómica. Referencia de sintaxis:
[ALTER TABLE de Oracle 10g](https://docs.oracle.com/cd/B19306_01/server.102/b14200/statements_3001.htm).

## Albaranes anteriores: conciliar antes de marcar pendientes

El esquema antiguo no guardaba qué albarán originó un abono. No se puede deducir su
estado únicamente del saldo actual, de la fecha o del importe. Marcar indiscriminadamente
todos los albaranes antiguos como pendientes permitiría pagarlos otra vez.

La única excepción automática es la **copia de desarrollo `COOPERATIVA`, con las 38 filas
demo originales y sin pagos previos**. La migración revisa los conteos por tabla, los DNI
ficticios, los tres saldos, los dos préstamos y el talonario con sus detalles. Los esquemas
temporales nuevos con prefijo **`TF_QA_`**, reservado para las pruebas aisladas de esta
corrección, admiten la misma excepción únicamente si tienen esa misma huella demo. Un
nombre o un conteo de filas por sí solos no autorizan la inicialización. La huella no es
un historial contable ni sustituye la certeza de que esa copia demo nunca recibió pagos.

Si otro esquema tiene albaranes y carece de la columna, la migración **se detiene** antes
de modificar la estructura. Con la API detenida, el responsable debe conciliar contra
los comprobantes reales y preparar la columna sin valor automático:

```sql
ALTER TABLE albaranes ADD (liquidado NUMBER(1));
```

Después debe asignar **por ID y con evidencia** `1` a los ya liquidados y `0` a los realmente
pendientes, y confirmar esa conciliación. No hay un `UPDATE` masivo proporcionado para
dar por pendientes los casos desconocidos. Mientras queden valores nulos o distintos de
0/1, el script 08 se niega a continuar. Cuando todos estén conciliados, 08 establece el
default 0 **para los nuevos registros**, `NOT NULL` y el `CHECK`, conservando los valores
existentes. Una tabla de albaranes vacía no necesita esta conciliación.

## Instalación nueva y verificaciones

- En un esquema vacío, `01_schema_oracle.sql` ya incluye la corrección; no recrearlo
  sobre un esquema existente. `02_initial_data.sql` es una carga ficticia opcional que
  solo se ejecuta una vez en tablas vacías, nunca como migración.
- `07_verify_oracle.sql` necesita la carga demo y una sesión nueva. Ejecuta
  **75 rechazos y 4 aceptaciones**, frente a los 73 rechazos anteriores: mantiene
  PK/FK/UNIQUE y demás checks, rechaza monto negativo y `liquidado=2/NULL`, y acepta
  monto cero, flags 0/1 y el default pendiente. Cada caso usa `SAVEPOINT`/`ROLLBACK`,
  no consume `NEXTVAL` y al final revierte todo. Resultado esperado:
  `VERIFICACION ORACLE OK: ... 75 rechazos y 4 casos validos. Sin cambios.`
- Para comprobar la migración en laboratorio, crear un esquema temporal `TF_QA_*`,
  cargar el DDL anterior y sus 38 filas demo, ejecutar 08 dos veces y luego 07 actualizado.
  No usar el esquema operativo para recrear el estado anterior.
- Las pruebas de negocio deben comprobar pago parcial y total de préstamo, rechazo de
  abono excesivo y negativo, cuenta ajena al socio, cuenta inactiva, doble liquidación
  secuencial y concurrente, y rollback del saldo si falla el marcado de liquidación.

Guardar los resultados de ejecuciones reales. La revisión de estos archivos no demuestra
que la migración ni las pruebas ya se hayan ejecutado en una instancia Oracle.

### Ejecutar la suite de negocio incluida

`FinancialOperationsIntegration` es una clase Java 8 con método `main`, sin JUnit ni
dependencias adicionales. Compilar desde la raíz del repositorio con `mvn test-compile`.
La suite no se ejecuta automáticamente con `mvn test`.

Crear un esquema **nuevo y desechable** `TF_QA_*` con tablas y secuencias propias,
cargar `01_schema_oracle.sql` y `02_initial_data.sql`, y colocar sus credenciales
en un `config.properties` local dentro de una carpeta de pruebas separada. Desde
esa carpeta, ejecutar con rutas absolutas al proyecto; en Windows, por ejemplo:

```powershell
java -cp "C:\ruta\cooperativa-api\target\test-classes;C:\ruta\cooperativa-api\target\classes;C:\ruta\cooperativa-api\lib\ojdbc14.jar" com.cooperativa.FinancialOperationsIntegration
```

La suite rechaza cualquier usuario o esquema que no empiece por `TF_QA_`, verifica
que las tablas sean propias y que existan las 38 filas de ejemplo antes de escribir.
Crea fixtures adicionales solo en ese esquema y comprueba pagos parciales y totales,
solicitudes simultáneas y rollback después de intentar registrar un saldo. El responsable
de las pruebas debe eliminar exclusivamente el esquema desechable que haya creado al
terminar. No ejecutar esta suite sobre la base del grupo.

La prueba del formulario no escribe en Oracle y se ejecuta desde la raíz del repositorio:
`node src/test/frontend/liquidacion.test.cjs`.

El respaldo PostgreSQL y la recuperación del proyecto de escritorio quedan fuera de esta
corrección de la API. Antes de usarlos con el nuevo esquema deben transportar `liquidado`;
restaurar solo las columnas antiguas perdería la información de pagos anteriores.
