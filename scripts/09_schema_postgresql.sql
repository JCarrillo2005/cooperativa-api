-- DDL oficial: 15 tablas, 15 PK, 19 FK restrictivas y 14 secuencias.
-- Contrato exacto de src/com/cooperativa/{model,dao,controller}.
-- Usar la base secundaria cooperativa_respaldo y su usuario propietario.
-- Requiere esquema VACIO. El DDL PostgreSQL se ejecuta en una transaccion.
-- NEXTVAL por defecto al insertar; el respaldo puede conservar los ids de Oracle.
-- Reglas interfilas (socio activo, ultimo titular, stock y total de detalles)
-- siguen a cargo de los controllers en sus transacciones.
\set ON_ERROR_STOP on
BEGIN;
CREATE SEQUENCE seq_sedes AS INTEGER START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NO CYCLE;
CREATE SEQUENCE seq_estatus AS INTEGER START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NO CYCLE;
CREATE SEQUENCE seq_tipos_persona AS INTEGER START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NO CYCLE;
CREATE SEQUENCE seq_tipos_cuenta AS INTEGER START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NO CYCLE;
CREATE SEQUENCE seq_tipos_prestamo AS INTEGER START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NO CYCLE;
CREATE SEQUENCE seq_categorias_producto AS INTEGER START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NO CYCLE;
CREATE SEQUENCE seq_personas AS INTEGER START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NO CYCLE;
CREATE SEQUENCE seq_productos AS INTEGER START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NO CYCLE;
CREATE SEQUENCE seq_cuentas AS INTEGER START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NO CYCLE;
CREATE SEQUENCE seq_saldos AS INTEGER START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NO CYCLE;
CREATE SEQUENCE seq_prestamos AS INTEGER START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NO CYCLE;
CREATE SEQUENCE seq_detalles_prestamo AS INTEGER START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NO CYCLE;
CREATE SEQUENCE seq_albaranes AS INTEGER START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NO CYCLE;
CREATE SEQUENCE seq_detalles_albaran AS INTEGER START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 2147483647 NO CYCLE;

CREATE TABLE sedes (
  id INTEGER DEFAULT nextval('seq_sedes') NOT NULL,
  nombre VARCHAR(100) NOT NULL,
  direccion VARCHAR(150),
  departamento VARCHAR(60),
  CONSTRAINT pk_sd PRIMARY KEY (id),
  CONSTRAINT ck_sd_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_sd_nombre CHECK (length(btrim(nombre)) > 0)
);
ALTER SEQUENCE seq_sedes OWNED BY sedes.id;

CREATE TABLE estatus (
  id INTEGER DEFAULT nextval('seq_estatus') NOT NULL,
  nombre VARCHAR(30) NOT NULL,
  descripcion VARCHAR(150),
  CONSTRAINT pk_es PRIMARY KEY (id),
  CONSTRAINT ck_es_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_es_nombre CHECK (length(btrim(nombre)) > 0)
);
ALTER SEQUENCE seq_estatus OWNED BY estatus.id;

CREATE TABLE tipos_persona (
  id INTEGER DEFAULT nextval('seq_tipos_persona') NOT NULL,
  nombre VARCHAR(30) NOT NULL,
  CONSTRAINT pk_tp PRIMARY KEY (id),
  CONSTRAINT ck_tp_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_tp_nombre CHECK (length(btrim(nombre)) > 0)
);
ALTER SEQUENCE seq_tipos_persona OWNED BY tipos_persona.id;

CREATE TABLE tipos_cuenta (
  id INTEGER DEFAULT nextval('seq_tipos_cuenta') NOT NULL,
  nombre VARCHAR(30) NOT NULL,
  CONSTRAINT pk_tc PRIMARY KEY (id),
  CONSTRAINT ck_tc_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_tc_nombre CHECK (length(btrim(nombre)) > 0)
);
ALTER SEQUENCE seq_tipos_cuenta OWNED BY tipos_cuenta.id;

CREATE TABLE tipos_prestamo (
  id INTEGER DEFAULT nextval('seq_tipos_prestamo') NOT NULL,
  nombre VARCHAR(30) NOT NULL,
  CONSTRAINT pk_tpr PRIMARY KEY (id),
  CONSTRAINT ck_tpr_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_tpr_nombre CHECK (length(btrim(nombre)) > 0)
);
ALTER SEQUENCE seq_tipos_prestamo OWNED BY tipos_prestamo.id;

CREATE TABLE categorias_producto (
  id INTEGER DEFAULT nextval('seq_categorias_producto') NOT NULL,
  nombre VARCHAR(50) NOT NULL,
  CONSTRAINT pk_cat PRIMARY KEY (id),
  CONSTRAINT ck_cat_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_cat_nombre CHECK (length(btrim(nombre)) > 0)
);
ALTER SEQUENCE seq_categorias_producto OWNED BY categorias_producto.id;

CREATE TABLE personas (
  id INTEGER DEFAULT nextval('seq_personas') NOT NULL,
  nombre VARCHAR(60) NOT NULL,
  apellido VARCHAR(60) NOT NULL,
  dni VARCHAR(20) NOT NULL,
  direccion VARCHAR(150),
  telefono VARCHAR(20),
  tipo_persona_id INTEGER NOT NULL,
  estatus_id INTEGER NOT NULL,
  sede_id INTEGER NOT NULL,
  fecha_registro TIMESTAMP WITHOUT TIME ZONE DEFAULT LOCALTIMESTAMP NOT NULL,
  CONSTRAINT pk_pe PRIMARY KEY (id),
  CONSTRAINT ck_pe_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_pe_nombre CHECK (length(btrim(nombre)) > 0),
  CONSTRAINT ck_pe_apellido CHECK (length(btrim(apellido)) > 0),
  CONSTRAINT ck_pe_dni CHECK (length(btrim(dni)) > 0),
  CONSTRAINT fk_pe_tipo FOREIGN KEY (tipo_persona_id) REFERENCES tipos_persona (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_pe_estatus FOREIGN KEY (estatus_id) REFERENCES estatus (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_pe_sede FOREIGN KEY (sede_id) REFERENCES sedes (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT uq_pe_dni UNIQUE (dni)
);
ALTER SEQUENCE seq_personas OWNED BY personas.id;

CREATE TABLE productos (
  id INTEGER DEFAULT nextval('seq_productos') NOT NULL,
  nombre VARCHAR(80) NOT NULL,
  descripcion VARCHAR(200),
  categoria_id INTEGER NOT NULL,
  precio NUMERIC(12,2) NOT NULL,
  stock INTEGER DEFAULT 0 NOT NULL,
  CONSTRAINT pk_pr PRIMARY KEY (id),
  CONSTRAINT ck_pr_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_pr_nombre CHECK (length(btrim(nombre)) > 0),
  CONSTRAINT fk_pr_categoria FOREIGN KEY (categoria_id) REFERENCES categorias_producto (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_pr_precio CHECK (precio > 0),
  CONSTRAINT ck_pr_stock CHECK (stock BETWEEN 0 AND 2147483647)
);
ALTER SEQUENCE seq_productos OWNED BY productos.id;

CREATE TABLE cuentas (
  id INTEGER DEFAULT nextval('seq_cuentas') NOT NULL,
  numero VARCHAR(30) NOT NULL,
  tipo_cuenta_id INTEGER NOT NULL,
  estatus_id INTEGER NOT NULL,
  sede_id INTEGER NOT NULL,
  fecha_apertura TIMESTAMP WITHOUT TIME ZONE DEFAULT LOCALTIMESTAMP NOT NULL,
  CONSTRAINT pk_cu PRIMARY KEY (id),
  CONSTRAINT ck_cu_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_cu_numero CHECK (length(btrim(numero)) > 0),
  CONSTRAINT fk_cu_tipo FOREIGN KEY (tipo_cuenta_id) REFERENCES tipos_cuenta (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_cu_estatus FOREIGN KEY (estatus_id) REFERENCES estatus (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_cu_sede FOREIGN KEY (sede_id) REFERENCES sedes (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT uq_cu_numero UNIQUE (numero)
);
ALTER SEQUENCE seq_cuentas OWNED BY cuentas.id;

CREATE TABLE cuentas_personas (
  cuenta_id INTEGER NOT NULL,
  persona_id INTEGER NOT NULL,
  titular SMALLINT DEFAULT 0 NOT NULL,
  CONSTRAINT fk_cp_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_cp_persona FOREIGN KEY (persona_id) REFERENCES personas (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT pk_cp PRIMARY KEY (cuenta_id, persona_id),
  CONSTRAINT ck_cp_titular CHECK (titular IN (0, 1))
);

CREATE TABLE saldos (
  id INTEGER DEFAULT nextval('seq_saldos') NOT NULL,
  cuenta_id INTEGER NOT NULL,
  monto NUMERIC(12,2) NOT NULL,
  fecha TIMESTAMP WITHOUT TIME ZONE DEFAULT LOCALTIMESTAMP NOT NULL,
  CONSTRAINT pk_sa PRIMARY KEY (id),
  CONSTRAINT ck_sa_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT fk_sa_cuenta FOREIGN KEY (cuenta_id) REFERENCES cuentas (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_sa_monto CHECK (monto >= 0)
);
ALTER SEQUENCE seq_saldos OWNED BY saldos.id;

CREATE TABLE prestamos (
  id INTEGER DEFAULT nextval('seq_prestamos') NOT NULL,
  persona_id INTEGER NOT NULL,
  tipo_prestamo_id INTEGER NOT NULL,
  estatus_id INTEGER NOT NULL,
  monto NUMERIC(12,2) NOT NULL,
  tasa_interes NUMERIC(5,2) DEFAULT 0 NOT NULL,
  fecha TIMESTAMP WITHOUT TIME ZONE DEFAULT LOCALTIMESTAMP NOT NULL,
  fecha_vencimiento TIMESTAMP WITHOUT TIME ZONE,
  CONSTRAINT pk_pre PRIMARY KEY (id),
  CONSTRAINT ck_pre_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT fk_pre_persona FOREIGN KEY (persona_id) REFERENCES personas (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_pre_tipo FOREIGN KEY (tipo_prestamo_id) REFERENCES tipos_prestamo (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_pre_estatus FOREIGN KEY (estatus_id) REFERENCES estatus (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  -- Monto representa el saldo pendiente; un prestamo pagado llega a cero.
  CONSTRAINT ck_pre_monto CHECK (monto >= 0),
  CONSTRAINT ck_pre_tasa CHECK (tasa_interes >= 0),
  CONSTRAINT ck_pre_fechas CHECK (fecha_vencimiento IS NULL OR fecha_vencimiento >= fecha)
);
ALTER SEQUENCE seq_prestamos OWNED BY prestamos.id;

CREATE TABLE detalles_prestamo (
  id INTEGER DEFAULT nextval('seq_detalles_prestamo') NOT NULL,
  prestamo_id INTEGER NOT NULL,
  producto_id INTEGER NOT NULL,
  cantidad INTEGER NOT NULL,
  precio_unitario NUMERIC(12,2) NOT NULL,
  CONSTRAINT pk_dp PRIMARY KEY (id),
  CONSTRAINT ck_dp_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT fk_dp_prestamo FOREIGN KEY (prestamo_id) REFERENCES prestamos (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_dp_producto FOREIGN KEY (producto_id) REFERENCES productos (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_dp_cantidad CHECK (cantidad BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_dp_precio CHECK (precio_unitario > 0)
);
ALTER SEQUENCE seq_detalles_prestamo OWNED BY detalles_prestamo.id;

CREATE TABLE albaranes (
  id INTEGER DEFAULT nextval('seq_albaranes') NOT NULL,
  numero_talonario VARCHAR(30) NOT NULL,
  persona_id INTEGER NOT NULL,
  sede_id INTEGER NOT NULL,
  fecha TIMESTAMP WITHOUT TIME ZONE DEFAULT LOCALTIMESTAMP NOT NULL,
  liquidado SMALLINT DEFAULT 0 NOT NULL,
  CONSTRAINT pk_al PRIMARY KEY (id),
  CONSTRAINT ck_al_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT ck_al_numero_talonario CHECK (length(btrim(numero_talonario)) > 0),
  CONSTRAINT ck_al_liquidado CHECK (liquidado IN (0, 1)),
  CONSTRAINT fk_al_persona FOREIGN KEY (persona_id) REFERENCES personas (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_al_sede FOREIGN KEY (sede_id) REFERENCES sedes (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT uq_al_talonario UNIQUE (numero_talonario)
);
ALTER SEQUENCE seq_albaranes OWNED BY albaranes.id;

CREATE TABLE detalles_albaran (
  id INTEGER DEFAULT nextval('seq_detalles_albaran') NOT NULL,
  albaran_id INTEGER NOT NULL,
  producto_id INTEGER NOT NULL,
  cantidad_aceptada NUMERIC(12,2) DEFAULT 0 NOT NULL,
  cantidad_rechazada NUMERIC(12,2) DEFAULT 0 NOT NULL,
  precio_unitario NUMERIC(12,2) NOT NULL,
  CONSTRAINT pk_da PRIMARY KEY (id),
  CONSTRAINT ck_da_id CHECK (id BETWEEN 1 AND 2147483647),
  CONSTRAINT fk_da_albaran FOREIGN KEY (albaran_id) REFERENCES albaranes (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT fk_da_producto FOREIGN KEY (producto_id) REFERENCES productos (id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT ck_da_aceptada CHECK (cantidad_aceptada >= 0),
  CONSTRAINT ck_da_rechazada CHECK (cantidad_rechazada >= 0),
  CONSTRAINT ck_da_cantidades CHECK (cantidad_aceptada + cantidad_rechazada > 0),
  CONSTRAINT ck_da_precio CHECK (precio_unitario > 0)
);
ALTER SEQUENCE seq_detalles_albaran OWNED BY detalles_albaran.id;

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
COMMIT;
