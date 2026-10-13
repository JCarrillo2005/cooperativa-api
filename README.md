# 🌾 Cooperativa Agrícola - API REST

API REST desarrollada con **Spring Boot 2.7.18** y **Java 8**, diseñada para migrar la lógica de negocio de una aplicación de escritorio tradicional hacia una arquitectura web moderna conectada a una base de datos **Oracle 10g**.

---

## 🚀 Características Principales

- **Gestión de Personas / Socios**: Alta, baja, actualización, validaciones de DNI y control de estatus.
- **Cuentas y Saldos**: Apertura transaccional de cuentas con titulares, depósitos, retiros y control de saldo histórico.
- **Inventario y Productos**: Control de stock, categorías, precios y alertas de stock bajo.
- **Préstamos**: Gestión de créditos en efectivo y de consumo de productos con descuento automático de inventario.
- **Albaranes (Entregas de Cosecha)**: Registro de entregas de socios, validación por talonario y liquidación directa a cuentas bancarias.
- **Catálogos Dinámicos**: Endpoints para alimentar menús desplegables (Sedes, Estatus, Tipos de Cuenta, etc.).
- **Respaldo en Caliente (Fase IV)**: Copia consistente Oracle → PostgreSQL sin detener la operación, disparable desde la interfaz web o por línea de comandos.

---

## 🛠️ Requisitos del Entorno

- **Java Development Kit (JDK)**: Temurin JDK 1.8 (Java 8).
- **Apache Maven**: Versión 3.x para la gestión de dependencias y ejecución del proyecto.
- **Base de Datos**: Oracle 10g (activo y accesible mediante JDBC).
- **Base secundaria (Fase IV)**: PostgreSQL 18 para el respaldo/migración (opcional para el CRUD).
- **Drivers JDBC**: `ojdbc14.jar` y `postgresql-42.7.13.jar` (alojados localmente en el directorio `/lib`).

---

## ⚙️ Guía de Instalación y Configuración

### 1. Configurar Apache Maven en el PATH de Windows

Si al ejecutar comandos de Maven en tu terminal obtienes un error de que no se reconoce el comando, sigue estos pasos para agregarlo a las variables de entorno de Windows:

1. **Ubica la carpeta de Maven**: Descomprime o localiza la carpeta donde tienes instalado Maven en tu computadora (por ejemplo: `C:\Program Files\Apache\maven\apache-maven-3.10.0` o la ruta donde lo tengas).
2. **Copia la ruta de la carpeta `bin`**: Entra a la carpeta de Maven, abre la carpeta `bin` y copia la ruta completa desde la barra de direcciones (ejemplo: `C:\maven\bin`).
3. **Abrir Variables de Entorno**:
   - Presiona la tecla `Windows`, escribe **"Variables de entorno"** y selecciona la opción _Editar las variables de entorno del sistema_.
   - En la ventana que se abre, haz clic en el botón inferior **Variables de entorno...**.
4. **Editar la variable `Path`**:
   - En la sección de abajo (_Variables del sistema_), busca la variable llamada **`Path`** y selecciónala.
   - Haz clic en el botón **Editar...**.
   - Haz clic en **Nuevo** y pega la ruta de la carpeta `bin` que copiaste en el paso 2.
   - Haz clic en **Aceptar** en todas las ventanas abiertas para guardar los cambios.
5. **Verificar la instalación**:
   - Cierra todas las terminales (PowerShell o CMD) que tengas abiertas para que recarguen los cambios.
   - Abre una nueva terminal y escribe:
     ```bash
     mvn -version
     ```
   - Deberás ver la versión de Maven y el JDK 1.8 configurado correctamente.

---

### 2. Configuración de la Base de Datos y Driver

1. Asegúrate de tener una carpeta llamada **`lib`** en la raíz del proyecto (`/cooperativa-api/lib/`) conteniendo el archivo **`ojdbc14.jar`**.
2. Coloca tu archivo local **`config.properties`** en la raíz del proyecto, desde donde ejecutas la API. `ConexionOracle` lee ese archivo del directorio de ejecución; no hace falta copiarlo a `src/main/resources/` ni incluirlo en el paquete de la aplicación. Está excluido de Git. El archivo debe contener los siguientes parámetros de conexión a Oracle 10g:
   ```properties
   db.url=jdbc:oracle:thin:@localhost:1521:XE
   db.user=tu_usuario
   db.password=tu_contraseña
   ```

## 🏃‍♂️ Ejecución del Proyecto

Abre una terminal en la carpeta raíz del proyecto (cooperativa-api).

**Compila y limpia el proyecto con Maven:**

"mvn clean compile"
Ejecuta la aplicación Spring Boot:

"mvn spring-boot:run"
El servidor web iniciará por defecto en el puerto 8081:
http://localhost:8081

## Interfaz Tierra Fértil y correcciones de pagos

La interfaz HTML, CSS y JavaScript está en `src/main/resources/static/` y se abre en
`http://localhost:8081/` con el mismo servidor Spring Boot. Consume los endpoints
existentes. La vista de diseño contiene datos ficticios; al conectar Oracle, muestra
los registros que devuelve la API. Las fotografías y fuentes incluyen sus créditos
y licencias junto a los recursos.

Esta versión permite pagar un préstamo hasta saldo cero y registra cada albarán
como pendiente o liquidado. Los abonos bloquean y releen el saldo para evitar
sobrescrituras concurrentes. Las liquidaciones acreditan y marcan el albarán en
una sola transacción, impidiendo volver a liquidarlo.

**Antes de ejecutar este backend sobre una base existente**, aplicar
[`scripts/08_fix_pagos_liquidaciones.sql`](scripts/08_fix_pagos_liquidaciones.sql)
siguiendo [`scripts/LEEME_FIX_PAGOS.md`](scripts/LEEME_FIX_PAGOS.md). Para una base nueva,
`scripts/01_schema_oracle.sql` ya contiene la estructura corregida. Si existen
entregas antiguas con pagos, conciliar su estado antes de migrar; no volver a cargar
los datos de ejemplo sobre una base existente. La receta paso a paso de ejecución
(instalación nueva vs. migración) está en
[`scripts/README.md`](scripts/README.md).

Las pruebas de negocio están en `src/test/java/com/cooperativa/FinancialOperationsIntegration.java`.
Son una clase `main` de integración con Oracle, sin dependencias nuevas; no se
ejecutan automáticamente con `mvn test`. Ver la guía para ejecutarlas exclusivamente
en un esquema desechable `TF_QA_*`. La prueba del estado del formulario puede ejecutarse
con `node src/test/frontend/liquidacion.test.cjs`.

## 🔥 Fase IV · Respaldo en caliente Oracle → PostgreSQL

Copia consistente de las 15 tablas desde Oracle a una base **PostgreSQL** secundaria **mientras la
cooperativa sigue operando**. Es *en caliente* porque Oracle no se detiene: el respaldo abre una
sesión `READ ONLY` que fija una **foto** de los datos, copia en lotes de 250, **verifica los
conteos** de cada tabla en ambos lados y alinea las 14 secuencias. Si algo falla, todo se revierte
y Oracle queda intacto.

- **Desde la interfaz**: módulo **Respaldo** → *Respaldar ahora*. Muestra el estado de PostgreSQL,
  la fecha del último respaldo y las 15 tablas con sus filas verificadas. Al terminar, el sistema
  **anuncia en pantalla** que la migración se completó (o el motivo del fallo), aunque hayas cambiado
  de módulo mientras se copiaba.
- **Endpoints**: `GET /api/mantenimiento/respaldo` (estado) y `POST /api/mantenimiento/respaldo`
  (dispara el respaldo; `202` si arranca, `409` si ya hay uno en curso).
- **Línea de comandos**:
  ```powershell
  java -cp "target\classes;lib\ojdbc14.jar;lib\postgresql-42.7.13.jar" com.cooperativa.utils.BackupEnCaliente respaldar
  ```

La **recuperación** (PostgreSQL → Oracle) no se expone en la interfaz a propósito: exige la API
detenida y un esquema Oracle **nuevo y vacío**, por lo que se ejecuta por CLI. La configuración
(`pg.*` y `restore.oracle.*` en `config.properties`) y las recetas completas —crear la base,
cargar el esquema `09`–`12` y ejecutar respaldo/recuperación— están en
[`scripts/README.md`](scripts/README.md) (Recetas C y D).

---

## 📋 Endpoints de la API REST

### 1. Personas y Socios

- GET /api/personas — Lista todas las personas registradas.

- GET /api/personas/socios — Lista únicamente a los socios.

- GET /api/personas/{id} — Obtiene una persona por su ID.

- POST /api/personas — Registra una nueva persona.

### 2. Cuentas Bancarias

- POST /api/cuentas — Abre una nueva cuenta con titulares y saldo inicial.

- GET /api/cuentas/{id}/saldo — Consulta el saldo actual de la cuenta.

- PATCH /api/cuentas/{id}/depositar?monto=500.00 — Realiza un depósito.

- PATCH /api/cuentas/{id}/retirar?monto=100.00 — Realiza un retiro.

### Inventario y Productos

- GET /api/productos — Lista todo el inventario.

- GET /api/productos/stock-bajo?umbral=5 — Muestra productos con stock crítico.

- POST /api/productos — Da de alta un nuevo producto.

### Préstamos

- GET /api/prestamos — Lista todos los créditos.

- POST /api/prestamos — Registra un préstamo (efectivo o consumo de productos con descuento de stock atómico).

- PATCH /api/prestamos/{id}/abono?montoAbono=200.00 — Registra un abono o liquidación.

### Albaranes (Entregas de Cosecha)

- GET /api/albaranes — Lista todas las entregas de los socios.

- POST /api/albaranes — Registra un nuevo albarán con sus líneas de productos.

- PATCH /api/albaranes/{id}/liquidar?cuentaId=2 — Liquida el albarán acreditándolo a una cuenta del socio.

### Catálogos

- GET /api/catalogos/sedes

- GET /api/catalogos/estatus

- GET /api/catalogos/tipos-persona

- GET /api/catalogos/tipos-cuenta

- GET /api/catalogos/tipos-prestamo

- GET /api/catalogos/categorias-producto

### Mantenimiento (Fase IV)

- GET /api/mantenimiento/respaldo — Estado del respaldo: en curso, último resultado, conteos por tabla y disponibilidad de PostgreSQL.

- POST /api/mantenimiento/respaldo — Dispara la copia Oracle → PostgreSQL (202 al iniciar, 409 si ya hay una en curso).
