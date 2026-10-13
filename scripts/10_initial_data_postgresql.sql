-- Datos FICTICIOS para desarrollo academico. No contienen datos de personas reales.
-- Ejecutar UNA vez despues del DDL, con tablas vacias. No es una migracion.
-- 38 filas en las 15 tablas; no se insertan ids fijos ni se desfasan secuencias.
\set ON_ERROR_STOP on
BEGIN;
DO $$
DECLARE t TEXT; n BIGINT;
BEGIN
  FOREACH t IN ARRAY ARRAY['sedes','estatus','tipos_persona','tipos_cuenta','tipos_prestamo','categorias_producto','personas','productos','cuentas','cuentas_personas','saldos','prestamos','detalles_prestamo','albaranes','detalles_albaran'] LOOP
    EXECUTE format('SELECT count(*) FROM %I', t) INTO n;
    IF n <> 0 THEN RAISE EXCEPTION 'La tabla % no esta vacia; cancele la carga demo.', t; END IF;
  END LOOP;
END $$;
INSERT INTO sedes (id, nombre, direccion, departamento)
VALUES (nextval('seq_sedes'), 'SEDE CENTRAL DEMO', 'Direccion ficticia 1', 'Guatemala');
INSERT INTO sedes (id, nombre, direccion, departamento)
VALUES (nextval('seq_sedes'), 'SEDE ALTIPLANO DEMO', 'Direccion ficticia 2', 'Chimaltenango');
INSERT INTO estatus (id, nombre, descripcion)
VALUES (nextval('seq_estatus'), 'ACTIVO', 'Registro habilitado');
INSERT INTO estatus (id, nombre, descripcion)
VALUES (nextval('seq_estatus'), 'INACTIVO', 'Registro deshabilitado');
INSERT INTO estatus (id, nombre, descripcion)
VALUES (nextval('seq_estatus'), 'PAGADO', 'Prestamo pagado');
INSERT INTO estatus (id, nombre, descripcion)
VALUES (nextval('seq_estatus'), 'VENCIDO', 'Prestamo vencido');
INSERT INTO tipos_persona (id, nombre)
VALUES (nextval('seq_tipos_persona'), 'EMPLEADO');
INSERT INTO tipos_persona (id, nombre)
VALUES (nextval('seq_tipos_persona'), 'SOCIO');
INSERT INTO tipos_persona (id, nombre)
VALUES (nextval('seq_tipos_persona'), 'OTRO');
INSERT INTO tipos_cuenta (id, nombre)
VALUES (nextval('seq_tipos_cuenta'), 'AHORRO');
INSERT INTO tipos_cuenta (id, nombre)
VALUES (nextval('seq_tipos_cuenta'), 'MONETARIA');
INSERT INTO tipos_prestamo (id, nombre)
VALUES (nextval('seq_tipos_prestamo'), 'EFECTIVO');
INSERT INTO tipos_prestamo (id, nombre)
VALUES (nextval('seq_tipos_prestamo'), 'HIPOTECARIO');
INSERT INTO tipos_prestamo (id, nombre)
VALUES (nextval('seq_tipos_prestamo'), 'CONSUMO_PRODUCTOS');
INSERT INTO categorias_producto (id, nombre)
VALUES (nextval('seq_categorias_producto'), 'INSUMO_SOCIOS');
INSERT INTO categorias_producto (id, nombre)
VALUES (nextval('seq_categorias_producto'), 'PRODUCTO_EXPORTACION');
INSERT INTO personas (id, nombre, apellido, dni, direccion, telefono, tipo_persona_id, estatus_id, sede_id, fecha_registro)
VALUES (nextval('seq_personas'), 'Ana', 'Ejemplo', 'DEMO-SOCIO-001', 'Direccion ficticia de prueba', NULL, (SELECT id FROM tipos_persona WHERE nombre = 'SOCIO'), (SELECT id FROM estatus WHERE nombre = 'ACTIVO'), (SELECT id FROM sedes WHERE nombre = 'SEDE CENTRAL DEMO'), DATE '2026-01-10');
INSERT INTO personas (id, nombre, apellido, dni, direccion, telefono, tipo_persona_id, estatus_id, sede_id, fecha_registro)
VALUES (nextval('seq_personas'), 'Luis', 'Prueba', 'DEMO-SOCIO-002', 'Direccion ficticia de prueba', NULL, (SELECT id FROM tipos_persona WHERE nombre = 'SOCIO'), (SELECT id FROM estatus WHERE nombre = 'ACTIVO'), (SELECT id FROM sedes WHERE nombre = 'SEDE ALTIPLANO DEMO'), DATE '2026-01-10');
INSERT INTO personas (id, nombre, apellido, dni, direccion, telefono, tipo_persona_id, estatus_id, sede_id, fecha_registro)
VALUES (nextval('seq_personas'), 'Marta', 'Ficticia', 'DEMO-EMPL-001', 'Direccion ficticia de prueba', NULL, (SELECT id FROM tipos_persona WHERE nombre = 'EMPLEADO'), (SELECT id FROM estatus WHERE nombre = 'ACTIVO'), (SELECT id FROM sedes WHERE nombre = 'SEDE CENTRAL DEMO'), DATE '2026-01-10');
INSERT INTO productos (id, nombre, descripcion, categoria_id, precio, stock)
VALUES (nextval('seq_productos'), 'Semilla maiz DEMO', 'Producto ficticio para pruebas academicas', (SELECT id FROM categorias_producto WHERE nombre = 'INSUMO_SOCIOS'), 25.5, 100);
INSERT INTO productos (id, nombre, descripcion, categoria_id, precio, stock)
VALUES (nextval('seq_productos'), 'Abono DEMO', 'Producto ficticio para pruebas academicas', (SELECT id FROM categorias_producto WHERE nombre = 'INSUMO_SOCIOS'), 45.0, 80);
INSERT INTO productos (id, nombre, descripcion, categoria_id, precio, stock)
VALUES (nextval('seq_productos'), 'Tomate DEMO', 'Producto ficticio para pruebas academicas', (SELECT id FROM categorias_producto WHERE nombre = 'PRODUCTO_EXPORTACION'), 7.5, 0);
INSERT INTO productos (id, nombre, descripcion, categoria_id, precio, stock)
VALUES (nextval('seq_productos'), 'Cebolla DEMO', 'Producto ficticio para pruebas academicas', (SELECT id FROM categorias_producto WHERE nombre = 'PRODUCTO_EXPORTACION'), 5.0, 0);
INSERT INTO cuentas (id, numero, tipo_cuenta_id, estatus_id, sede_id, fecha_apertura)
VALUES (nextval('seq_cuentas'), 'CTA-DEMO-001', (SELECT id FROM tipos_cuenta WHERE nombre = 'AHORRO'), (SELECT id FROM estatus WHERE nombre = 'ACTIVO'), (SELECT id FROM sedes WHERE nombre = 'SEDE CENTRAL DEMO'), DATE '2026-01-10');
INSERT INTO cuentas (id, numero, tipo_cuenta_id, estatus_id, sede_id, fecha_apertura)
VALUES (nextval('seq_cuentas'), 'CTA-DEMO-002', (SELECT id FROM tipos_cuenta WHERE nombre = 'MONETARIA'), (SELECT id FROM estatus WHERE nombre = 'ACTIVO'), (SELECT id FROM sedes WHERE nombre = 'SEDE ALTIPLANO DEMO'), DATE '2026-01-10');
INSERT INTO cuentas_personas (cuenta_id, persona_id, titular)
VALUES ((SELECT id FROM cuentas WHERE numero = 'CTA-DEMO-001'), (SELECT id FROM personas WHERE dni = 'DEMO-SOCIO-001'), 1);
INSERT INTO cuentas_personas (cuenta_id, persona_id, titular)
VALUES ((SELECT id FROM cuentas WHERE numero = 'CTA-DEMO-001'), (SELECT id FROM personas WHERE dni = 'DEMO-SOCIO-002'), 0);
INSERT INTO cuentas_personas (cuenta_id, persona_id, titular)
VALUES ((SELECT id FROM cuentas WHERE numero = 'CTA-DEMO-002'), (SELECT id FROM personas WHERE dni = 'DEMO-SOCIO-002'), 1);
INSERT INTO saldos (id, cuenta_id, monto, fecha)
VALUES (nextval('seq_saldos'), (SELECT id FROM cuentas WHERE numero = 'CTA-DEMO-001'), 1000.0, DATE '2026-01-10');
INSERT INTO saldos (id, cuenta_id, monto, fecha)
VALUES (nextval('seq_saldos'), (SELECT id FROM cuentas WHERE numero = 'CTA-DEMO-001'), 1250.0, DATE '2026-01-11');
INSERT INTO saldos (id, cuenta_id, monto, fecha)
VALUES (nextval('seq_saldos'), (SELECT id FROM cuentas WHERE numero = 'CTA-DEMO-002'), 500.0, DATE '2026-01-10');
INSERT INTO prestamos (id, persona_id, tipo_prestamo_id, estatus_id, monto, tasa_interes, fecha, fecha_vencimiento)
VALUES (nextval('seq_prestamos'), (SELECT id FROM personas WHERE dni = 'DEMO-SOCIO-001'), (SELECT id FROM tipos_prestamo WHERE nombre = 'CONSUMO_PRODUCTOS'), (SELECT id FROM estatus WHERE nombre = 'ACTIVO'), 192.0, 10.0, DATE '2026-01-10', DATE '2027-01-10');
INSERT INTO prestamos (id, persona_id, tipo_prestamo_id, estatus_id, monto, tasa_interes, fecha, fecha_vencimiento)
VALUES (nextval('seq_prestamos'), (SELECT id FROM personas WHERE dni = 'DEMO-SOCIO-002'), (SELECT id FROM tipos_prestamo WHERE nombre = 'EFECTIVO'), (SELECT id FROM estatus WHERE nombre = 'ACTIVO'), 1500.0, 12.0, DATE '2026-01-10', DATE '2027-01-10');
INSERT INTO detalles_prestamo (id, prestamo_id, producto_id, cantidad, precio_unitario)
VALUES (nextval('seq_detalles_prestamo'), (SELECT p.id FROM prestamos p JOIN personas pe ON pe.id = p.persona_id JOIN tipos_prestamo tp ON tp.id = p.tipo_prestamo_id WHERE pe.dni = 'DEMO-SOCIO-001' AND tp.nombre = 'CONSUMO_PRODUCTOS'), (SELECT id FROM productos WHERE nombre = 'Semilla maiz DEMO'), 4, 25.5);
INSERT INTO detalles_prestamo (id, prestamo_id, producto_id, cantidad, precio_unitario)
VALUES (nextval('seq_detalles_prestamo'), (SELECT p.id FROM prestamos p JOIN personas pe ON pe.id = p.persona_id JOIN tipos_prestamo tp ON tp.id = p.tipo_prestamo_id WHERE pe.dni = 'DEMO-SOCIO-001' AND tp.nombre = 'CONSUMO_PRODUCTOS'), (SELECT id FROM productos WHERE nombre = 'Abono DEMO'), 2, 45.0);
INSERT INTO albaranes (id, numero_talonario, persona_id, sede_id, fecha)
VALUES (nextval('seq_albaranes'), 'TAL-DEMO-001', (SELECT id FROM personas WHERE dni = 'DEMO-SOCIO-001'), (SELECT id FROM sedes WHERE nombre = 'SEDE CENTRAL DEMO'), DATE '2026-01-12');
INSERT INTO detalles_albaran (id, albaran_id, producto_id, cantidad_aceptada, cantidad_rechazada, precio_unitario)
VALUES (nextval('seq_detalles_albaran'), (SELECT id FROM albaranes WHERE numero_talonario = 'TAL-DEMO-001'), (SELECT id FROM productos WHERE nombre = 'Tomate DEMO'), 125.5, 4.5, 7.5);
INSERT INTO detalles_albaran (id, albaran_id, producto_id, cantidad_aceptada, cantidad_rechazada, precio_unitario)
VALUES (nextval('seq_detalles_albaran'), (SELECT id FROM albaranes WHERE numero_talonario = 'TAL-DEMO-001'), (SELECT id FROM productos WHERE nombre = 'Cebolla DEMO'), 80.0, 0.0, 5.0);
-- Replica el descuento aplicado por PrestamoController: 4 semillas y 2 abonos.
UPDATE productos SET stock = stock - 4 WHERE nombre = 'Semilla maiz DEMO';
UPDATE productos SET stock = stock - 2 WHERE nombre = 'Abono DEMO';
COMMIT;
-- Total del albaran demo: (125.50 * 7.50) + (80.00 * 5.00) = 1341.25.
