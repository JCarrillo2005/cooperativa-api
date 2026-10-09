-- DDL oficial: 15 tablas, 15 PK, 19 FK restrictivas y 14 secuencias.
-- Contrato exacto de src/com/cooperativa/{model,dao,controller}.
-- No ejecutar con SYSTEM/SYS: usar un esquema propio de la cooperativa.
-- Requiere esquema VACIO. Oracle confirma DDL implicitamente; no es atomico.
-- No hay triggers: los DAO Oracle solicitan NEXTVAL antes del INSERT.
-- Reglas interfilas (socio activo, ultimo titular, stock y total de detalles)
-- siguen a cargo de los controllers en sus transacciones.
SET DEFINE OFF
WHENEVER SQLERROR EXIT FAILURE ROLLBACK
WHENEVER OSERROR EXIT FAILURE ROLLBACK
CREATE SEQUENCE seq_sedes START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NOCYCLE NOCACHE;
CREATE SEQUENCE seq_estatus START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NOCYCLE NOCACHE;
CREATE SEQUENCE seq_tipos_persona START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NOCYCLE NOCACHE;
CREATE SEQUENCE seq_tipos_cuenta START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NOCYCLE NOCACHE;
CREATE SEQUENCE seq_tipos_prestamo START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NOCYCLE NOCACHE;
CREATE SEQUENCE seq_categorias_producto START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NOCYCLE NOCACHE;
CREATE SEQUENCE seq_personas START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NOCYCLE NOCACHE;
CREATE SEQUENCE seq_productos START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NOCYCLE NOCACHE;
CREATE SEQUENCE seq_cuentas START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NOCYCLE NOCACHE;
CREATE SEQUENCE seq_saldos START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NOCYCLE NOCACHE;
CREATE SEQUENCE seq_prestamos START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NOCYCLE NOCACHE;
CREATE SEQUENCE seq_detalles_prestamo START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NOCYCLE NOCACHE;
CREATE SEQUENCE seq_albaranes START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NOCYCLE NOCACHE;
CREATE SEQUENCE seq_detalles_albaran START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NOCYCLE NOCACHE;

CREATE TABLE sedes (
  id NUMBER(10) NOT NULL,
  nombre VARCHAR2(100 CHAR) NOT NULL,
  direccion VARCHAR2(150 CHAR),
  departamento VARCHAR2(60 CHAR),
  CONSTRAINT pk_sd PRIMARY KEY (id),
  CONSTRAINT ck_sd_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_sd_nombre CHECK (TRIM(nombre) IS NOT NULL)
);

CREATE TABLE estatus (
  id NUMBER(10) NOT NULL,
  nombre VARCHAR2(30 CHAR) NOT NULL,
  descripcion VARCHAR2(150 CHAR),
  CONSTRAINT pk_es PRIMARY KEY (id),
  CONSTRAINT ck_es_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_es_nombre CHECK (TRIM(nombre) IS NOT NULL)
);

CREATE TABLE tipos_persona (
  id NUMBER(10) NOT NULL,
  nombre VARCHAR2(30 CHAR) NOT NULL,
  CONSTRAINT pk_tp PRIMARY KEY (id),
  CONSTRAINT ck_tp_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_tp_nombre CHECK (TRIM(nombre) IS NOT NULL)
);

CREATE TABLE tipos_cuenta (
  id NUMBER(10) NOT NULL,
  nombre VARCHAR2(30 CHAR) NOT NULL,
  CONSTRAINT pk_tc PRIMARY KEY (id),
  CONSTRAINT ck_tc_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_tc_nombre CHECK (TRIM(nombre) IS NOT NULL)
);

CREATE TABLE tipos_prestamo (
  id NUMBER(10) NOT NULL,
  nombre VARCHAR2(30 CHAR) NOT NULL,
  CONSTRAINT pk_tpr PRIMARY KEY (id),
  CONSTRAINT ck_tpr_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_tpr_nombre CHECK (TRIM(nombre) IS NOT NULL)
);

CREATE TABLE categorias_producto (
  id NUMBER(10) NOT NULL,
  nombre VARCHAR2(50 CHAR) NOT NULL,
  CONSTRAINT pk_cat PRIMARY KEY (id),
  CONSTRAINT ck_cat_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_cat_nombre CHECK (TRIM(nombre) IS NOT NULL)
);

CREATE TABLE personas (
  id NUMBER(10) NOT NULL,
  nombre VARCHAR2(60 CHAR) NOT NULL,
  apellido VARCHAR2(60 CHAR) NOT NULL,
  dni VARCHAR2(20 CHAR) NOT NULL,
  direccion VARCHAR2(150 CHAR),
  telefono VARCHAR2(20 CHAR),
  tipo_persona_id NUMBER(10) NOT NULL,
  estatus_id NUMBER(10) NOT NULL,
  sede_id NUMBER(10) NOT NULL,
  fecha_registro DATE DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_pe PRIMARY KEY (id),
  CONSTRAINT ck_pe_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_pe_nombre CHECK (TRIM(nombre) IS NOT NULL),
  CONSTRAINT ck_pe_apellido CHECK (TRIM(apellido) IS NOT NULL),
  CONSTRAINT ck_pe_dni CHECK (TRIM(dni) IS NOT NULL),
  CONSTRAINT fk_pe_tipo FOREIGN KEY (tipo_persona_id) REFERENCES tipos_persona (id),
  CONSTRAINT fk_pe_estatus FOREIGN KEY (estatus_id) REFERENCES estatus (id),
  CONSTRAINT fk_pe_sede FOREIGN KEY (sede_id) REFERENCES sedes (id),
  CONSTRAINT uq_pe_dni UNIQUE (dni)
);

CREATE TABLE productos (
  id NUMBER(10) NOT NULL,
  nombre VARCHAR2(80 CHAR) NOT NULL,
  descripcion VARCHAR2(200 CHAR),
  categoria_id NUMBER(10) NOT NULL,
  precio NUMBER(12,2) NOT NULL,
  stock NUMBER(10) DEFAULT 0 NOT NULL,
  CONSTRAINT pk_pr PRIMARY KEY (id),
  CONSTRAINT ck_pr_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_pr_nombre CHECK (TRIM(nombre) IS NOT NULL),
  CONSTRAINT fk_pr_categoria FOREIGN KEY (categoria_id) REFERENCES categorias_producto (id),
  CONSTRAINT ck_pr_precio CHECK (precio > 0),
  CONSTRAINT ck_pr_stock CHECK (stock BETWEEN 0 AND 2147483647)
);

CREATE TABLE cuentas (
  id NUMBER(10) NOT NULL,
  numero VARCHAR2(30 CHAR) NOT NULL,
  tipo_cuenta_id NUMBER(10) NOT NULL,
  estatus_id NUMBER(10) NOT NULL,
  sede_id NUMBER(10) NOT NULL,
  fecha_apertura DATE DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_cu PRIMARY KEY (id),
  CONSTRAINT ck_cu_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_cu_numero CHECK (TRIM(numero) IS NOT NULL),
  CONSTRAINT fk_cu_tipo FOREIGN KEY (tipo_cuenta_id) REFERENCES tipos_cuenta (id),
  CONSTRAINT fk_cu_estatus FOREIGN KEY (estatus_id) REFERENCES estatus (id),
  CONSTRAINT fk_cu_sede FOREIGN KEY (sede_id) REFERENCES sedes (id),
  CONSTRAINT uq_cu_numero UNIQUE (numero)
);

CREATE TABLE cuentas_personas (
  cuenta_id NUMBER(10) NOT NULL,
  persona_id NUMBER(10) NOT NULL,
  titular NUMBER(1) DEFAULT 0 NOT NULL,
  CONSTRAINT fk_cp_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id),
  CONSTRAINT fk_cp_persona FOREIGN KEY (persona_id) REFERENCES personas (id),
  CONSTRAINT pk_cp PRIMARY KEY (cuenta_id, persona_id),
  CONSTRAINT ck_cp_titular CHECK (titular IN (0, 1))
);

CREATE TABLE saldos (
  id NUMBER(10) NOT NULL,
  cuenta_id NUMBER(10) NOT NULL,
  monto NUMBER(12,2) NOT NULL,
  fecha DATE DEFAULT SYSDATE NOT NULL,
  CONSTRAINT pk_sa PRIMARY KEY (id),
  CONSTRAINT ck_sa_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT fk_sa_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id),
  CONSTRAINT ck_sa_monto CHECK (monto >= 0)
);

CREATE TABLE prestamos (
  id NUMBER(10) NOT NULL,
  persona_id NUMBER(10) NOT NULL,
  tipo_prestamo_id NUMBER(10) NOT NULL,
  estatus_id NUMBER(10) NOT NULL,
  monto NUMBER(12,2) NOT NULL,
  tasa_interes NUMBER(5,2) DEFAULT 0 NOT NULL,
  fecha DATE DEFAULT SYSDATE NOT NULL,
  fecha_vencimiento DATE,
  CONSTRAINT pk_pre PRIMARY KEY (id),
  CONSTRAINT ck_pre_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT fk_pre_persona FOREIGN KEY (persona_id) REFERENCES personas (id),
  CONSTRAINT fk_pre_tipo FOREIGN KEY (tipo_prestamo_id) REFERENCES tipos_prestamo (id),
  CONSTRAINT fk_pre_estatus FOREIGN KEY (estatus_id) REFERENCES estatus (id),
  -- Monto representa el saldo pendiente; un prestamo pagado llega a cero.
  CONSTRAINT ck_pre_monto CHECK (monto >= 0),
  CONSTRAINT ck_pre_tasa CHECK (tasa_interes >= 0),
  CONSTRAINT ck_pre_fechas CHECK (fecha_vencimiento IS NULL OR fecha_vencimiento >= fecha)
);

CREATE TABLE detalles_prestamo (
  id NUMBER(10) NOT NULL,
  prestamo_id NUMBER(10) NOT NULL,
  producto_id NUMBER(10) NOT NULL,
  cantidad NUMBER(10) NOT NULL,
  precio_unitario NUMBER(12,2) NOT NULL,
  CONSTRAINT pk_dp PRIMARY KEY (id),
  CONSTRAINT ck_dp_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT fk_dp_prestamo FOREIGN KEY (prestamo_id) REFERENCES prestamos (id),
  CONSTRAINT fk_dp_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
  CONSTRAINT ck_dp_cantidad CHECK (cantidad BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_dp_precio CHECK (precio_unitario > 0)
);

CREATE TABLE albaranes (
  id NUMBER(10) NOT NULL,
  numero_talonario VARCHAR2(30 CHAR) NOT NULL,
  persona_id NUMBER(10) NOT NULL,
  sede_id NUMBER(10) NOT NULL,
  fecha DATE DEFAULT SYSDATE NOT NULL,
  liquidado NUMBER(1) DEFAULT 0 NOT NULL,
  CONSTRAINT pk_al PRIMARY KEY (id),
  CONSTRAINT ck_al_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_al_numero_talonario CHECK (TRIM(numero_talonario) IS NOT NULL),
  CONSTRAINT ck_al_liquidado CHECK (liquidado IN (0, 1)),
  CONSTRAINT fk_al_persona FOREIGN KEY (persona_id) REFERENCES personas (id),
  CONSTRAINT fk_al_sede FOREIGN KEY (sede_id) REFERENCES sedes (id),
  CONSTRAINT uq_al_talonario UNIQUE (numero_talonario)
);

CREATE TABLE detalles_albaran (
  id NUMBER(10) NOT NULL,
  albaran_id NUMBER(10) NOT NULL,
  producto_id NUMBER(10) NOT NULL,
  cantidad_aceptada NUMBER(12,2) DEFAULT 0 NOT NULL,
  cantidad_rechazada NUMBER(12,2) DEFAULT 0 NOT NULL,
  precio_unitario NUMBER(12,2) NOT NULL,
  CONSTRAINT pk_da PRIMARY KEY (id),
  CONSTRAINT ck_da_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT fk_da_albaran FOREIGN KEY (albaran_id) REFERENCES albaranes (id),
  CONSTRAINT fk_da_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
  CONSTRAINT ck_da_aceptada CHECK (cantidad_aceptada >= 0),
  CONSTRAINT ck_da_rechazada CHECK (cantidad_rechazada >= 0),
  CONSTRAINT ck_da_cantidades CHECK (cantidad_aceptada + cantidad_rechazada > 0),
  CONSTRAINT ck_da_precio CHECK (precio_unitario > 0)
);

-- Los nombres de catalogo no distinguen mayusculas en Catalogos.java.
CREATE UNIQUE INDEX uq_sd_nombre ON sedes (UPPER(nombre));
CREATE UNIQUE INDEX uq_es_nombre ON estatus (UPPER(nombre));
CREATE UNIQUE INDEX uq_tp_nombre ON tipos_persona (UPPER(nombre));
CREATE UNIQUE INDEX uq_tc_nombre ON tipos_cuenta (UPPER(nombre));
CREATE UNIQUE INDEX uq_tpr_nombre ON tipos_prestamo (UPPER(nombre));
CREATE UNIQUE INDEX uq_cat_nombre ON categorias_producto (UPPER(nombre));

-- Indices FK: la PK compuesta ya cubre cuentas_personas.cuenta_id.
CREATE INDEX ix_pe_tipo ON personas (tipo_persona_id);
CREATE INDEX ix_pe_estatus ON personas (estatus_id);
CREATE INDEX ix_pe_sede ON personas (sede_id);
CREATE INDEX ix_pr_categoria ON productos (categoria_id);
CREATE INDEX ix_cu_tipo ON cuentas (tipo_cuenta_id);
CREATE INDEX ix_cu_estatus ON cuentas (estatus_id);
CREATE INDEX ix_cu_sede ON cuentas (sede_id);
CREATE INDEX ix_cp_persona ON cuentas_personas (persona_id);
CREATE INDEX ix_sa_cuenta ON saldos (cuenta_id);
CREATE INDEX ix_pre_persona ON prestamos (persona_id);
CREATE INDEX ix_pre_tipo ON prestamos (tipo_prestamo_id);
CREATE INDEX ix_pre_estatus ON prestamos (estatus_id);
CREATE INDEX ix_dp_prestamo ON detalles_prestamo (prestamo_id);
CREATE INDEX ix_dp_producto ON detalles_prestamo (producto_id);
CREATE INDEX ix_al_persona ON albaranes (persona_id);
CREATE INDEX ix_al_sede ON albaranes (sede_id);
CREATE INDEX ix_da_albaran ON detalles_albaran (albaran_id);
CREATE INDEX ix_da_producto ON detalles_albaran (producto_id);
CREATE INDEX ix_sa_historial ON saldos (cuenta_id, fecha DESC, id DESC);
PROMPT Esquema Oracle creado: 15 tablas y 14 secuencias.
