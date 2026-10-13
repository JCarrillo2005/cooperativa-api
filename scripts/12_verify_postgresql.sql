-- Ejecutar con psql, en SESION NUEVA y despues de 09 + 10.
-- Las pruebas trabajan sobre filas existentes, sin NEXTVAL.
-- La funcion es temporal y se elimina junto con sus cambios por ROLLBACK.
\set ON_ERROR_STOP on
BEGIN;
CREATE FUNCTION pg_temp.rechazar(etiqueta TEXT, sentencia TEXT, esperado TEXT)
RETURNS VOID LANGUAGE plpgsql AS $$
DECLARE recibido TEXT := NULL;
BEGIN
  BEGIN
    EXECUTE sentencia;
  EXCEPTION WHEN OTHERS THEN
    recibido := SQLSTATE;
  END;
  IF recibido IS DISTINCT FROM esperado THEN
    RAISE EXCEPTION '%: se esperaba SQLSTATE %, obtenido % (NULL significa dato invalido aceptado).', etiqueta, esperado, coalesce(recibido, 'NULL');
  END IF;
  RAISE NOTICE 'OK: %', etiqueta;
END $$;
DO $$
DECLARE
  tablas TEXT[] := ARRAY['sedes','estatus','tipos_persona','tipos_cuenta','tipos_prestamo','categorias_producto','personas','productos','cuentas','cuentas_personas','saldos','prestamos','detalles_prestamo','albaranes','detalles_albaran'];
  t TEXT;
  n BIGINT;
  fk RECORD;
  pk RECORD;
  columnas TEXT;
  n_fks INTEGER := 0;
  n_pks INTEGER := 0;
BEGIN
  FOREACH t IN ARRAY tablas LOOP
    EXECUTE format('SELECT count(*) FROM %I.%I', current_schema(), t) INTO n;
    IF n = 0 THEN RAISE EXCEPTION 'Cargue 05_initial_data_postgresql.sql: falta una fila en %.', t; END IF;
    IF t <> 'cuentas_personas' THEN
      EXECUTE format('SELECT count(*) FROM %I.%I WHERE id = 2147483647', current_schema(), t) INTO n;
      IF n <> 0 THEN RAISE EXCEPTION 'Id reservado por las pruebas ocupado en %.', t; END IF;
    END IF;
  END LOOP;
  SELECT count(*) INTO n FROM pg_sequences
  WHERE schemaname = current_schema() AND sequencename = ANY(ARRAY['seq_sedes','seq_estatus','seq_tipos_persona','seq_tipos_cuenta','seq_tipos_prestamo','seq_categorias_producto','seq_personas','seq_productos','seq_cuentas','seq_saldos','seq_prestamos','seq_detalles_prestamo','seq_albaranes','seq_detalles_albaran'])
    AND min_value = 1 AND max_value = 2147483647 AND increment_by = 1 AND NOT cycle;
  IF n <> 14 THEN RAISE EXCEPTION 'Faltan secuencias o limites compatibles con Java int.'; END IF;
  SELECT count(*) INTO n FROM pg_constraint c
  JOIN pg_class r ON r.oid = c.conrelid
  JOIN pg_namespace ns ON ns.oid = r.relnamespace
  WHERE ns.nspname = current_schema() AND r.relname = ANY(tablas) AND NOT c.convalidated;
  IF n <> 0 THEN RAISE EXCEPTION 'Hay restricciones sin validar.'; END IF;
  FOR fk IN
    SELECT c.conname, hijo.relname AS tabla, hc.attname AS columna,
           padre.relname AS tabla_padre, pc.attname AS columna_padre,
           c.confdeltype, c.confupdtype, c.conrelid, c.conkey[1] AS columna_numero
    FROM pg_constraint c
    JOIN pg_class hijo ON hijo.oid = c.conrelid
    JOIN pg_namespace ns ON ns.oid = hijo.relnamespace
    JOIN pg_class padre ON padre.oid = c.confrelid
    JOIN pg_attribute hc ON hc.attrelid = hijo.oid AND hc.attnum = c.conkey[1]
    JOIN pg_attribute pc ON pc.attrelid = padre.oid AND pc.attnum = c.confkey[1]
    WHERE c.contype = 'f' AND ns.nspname = current_schema() AND hijo.relname = ANY(tablas)
    ORDER BY c.conname
  LOOP
    n_fks := n_fks + 1;
    IF fk.confdeltype <> 'r' OR fk.confupdtype <> 'r' THEN RAISE EXCEPTION 'FK no restrictiva: %', fk.conname; END IF;
    SELECT count(*) INTO n FROM pg_index i
    WHERE i.indrelid = fk.conrelid AND i.indisvalid AND i.indkey[0] = fk.columna_numero;
    IF n = 0 THEN RAISE EXCEPTION 'Falta indice FK: %', fk.conname; END IF;
    PERFORM pg_temp.rechazar('FK ' || fk.conname, format('UPDATE %I.%I SET %I = 2147483647 WHERE ctid = (SELECT ctid FROM %I.%I LIMIT 1)',
      current_schema(), fk.tabla, fk.columna, current_schema(), fk.tabla), '23503');
    -- En PostgreSQL, ON DELETE RESTRICT devuelve 23001 (restrict_violation); ON UPDATE RESTRICT, 23503.
    PERFORM pg_temp.rechazar('DELETE padre de ' || fk.conname, format('DELETE FROM %I.%I WHERE %I = (SELECT %I FROM %I.%I LIMIT 1)',
      current_schema(), fk.tabla_padre, fk.columna_padre, fk.columna, current_schema(), fk.tabla), '23001');
  END LOOP;
  IF n_fks <> 19 THEN RAISE EXCEPTION 'Se esperaban 19 FK; detectadas: %', n_fks; END IF;
  FOR pk IN SELECT r.oid, r.relname FROM pg_constraint c
    JOIN pg_class r ON r.oid = c.conrelid
    JOIN pg_namespace ns ON ns.oid = r.relnamespace
    WHERE c.contype = 'p' AND ns.nspname = current_schema() AND r.relname = ANY(tablas)
  LOOP
    n_pks := n_pks + 1;
    SELECT string_agg(quote_ident(attname), ',' ORDER BY attnum) INTO columnas
    FROM pg_attribute WHERE attrelid = pk.oid AND attnum > 0 AND NOT attisdropped;
    PERFORM pg_temp.rechazar('PK/fila duplicada de ' || pk.relname, format('INSERT INTO %I.%I (%s) SELECT %s FROM %I.%I LIMIT 1',
      current_schema(), pk.relname, columnas, columnas, current_schema(), pk.relname), '23505');
  END LOOP;
  IF n_pks <> 15 THEN RAISE EXCEPTION 'Se esperaban 15 PK.'; END IF;
  PERFORM pg_temp.rechazar('DNI unico', 'INSERT INTO personas (id,nombre,apellido,dni,tipo_persona_id,estatus_id,sede_id,fecha_registro) SELECT 2147483647,nombre,apellido,dni,tipo_persona_id,estatus_id,sede_id,fecha_registro FROM personas LIMIT 1', '23505');
  PERFORM pg_temp.rechazar('numero de cuenta unico', 'INSERT INTO cuentas (id,numero,tipo_cuenta_id,estatus_id,sede_id,fecha_apertura) SELECT 2147483647,numero,tipo_cuenta_id,estatus_id,sede_id,fecha_apertura FROM cuentas LIMIT 1', '23505');
  PERFORM pg_temp.rechazar('talonario unico', 'INSERT INTO albaranes (id,numero_talonario,persona_id,sede_id,fecha) SELECT 2147483647,numero_talonario,persona_id,sede_id,fecha FROM albaranes LIMIT 1', '23505');
  PERFORM pg_temp.rechazar('liquidado solo 0 o 1', 'UPDATE albaranes SET liquidado = 2', '23514');
  PERFORM pg_temp.rechazar('nombre de catalogo sin duplicados por mayusculas', 'INSERT INTO estatus (id,nombre) SELECT 2147483647,lower(nombre) FROM estatus LIMIT 1', '23505');
  PERFORM pg_temp.rechazar('id positivo', 'INSERT INTO sedes (id,nombre) VALUES (0,''VERIFICACION INVALIDA'')', '23514');
  PERFORM pg_temp.rechazar('nombre obligatorio sin espacios', 'UPDATE personas SET nombre = ''   ''', '23514');
  PERFORM pg_temp.rechazar('FK no nula', 'UPDATE personas SET sede_id = NULL', '23502');
  PERFORM pg_temp.rechazar('titular solo 0 o 1', 'UPDATE cuentas_personas SET titular = 2', '23514');
  PERFORM pg_temp.rechazar('saldo no negativo', 'UPDATE saldos SET monto = -0.01', '23514');
  PERFORM pg_temp.rechazar('stock no negativo', 'UPDATE productos SET stock = -1', '23514');
  PERFORM pg_temp.rechazar('precio de producto positivo', 'UPDATE productos SET precio = 0', '23514');
  PERFORM pg_temp.rechazar('monto de prestamo no negativo', 'UPDATE prestamos SET monto = -0.01', '23514');
  PERFORM pg_temp.rechazar('tasa no negativa', 'UPDATE prestamos SET tasa_interes = -0.01', '23514');
  PERFORM pg_temp.rechazar('vencimiento no anterior al alta', 'UPDATE prestamos SET fecha_vencimiento = fecha - INTERVAL ''1 day''', '23514');
  PERFORM pg_temp.rechazar('cantidad de prestamo positiva', 'UPDATE detalles_prestamo SET cantidad = 0', '23514');
  PERFORM pg_temp.rechazar('precio de prestamo positivo', 'UPDATE detalles_prestamo SET precio_unitario = 0', '23514');
  PERFORM pg_temp.rechazar('cantidad aceptada no negativa', 'UPDATE detalles_albaran SET cantidad_aceptada = -0.01', '23514');
  PERFORM pg_temp.rechazar('cantidad rechazada no negativa', 'UPDATE detalles_albaran SET cantidad_rechazada = -0.01', '23514');
  PERFORM pg_temp.rechazar('detalle albaran no vacio', 'UPDATE detalles_albaran SET cantidad_aceptada = 0, cantidad_rechazada = 0', '23514');
  PERFORM pg_temp.rechazar('precio del albaran positivo', 'UPDATE detalles_albaran SET precio_unitario = 0', '23514');
  RAISE NOTICE 'VERIFICACION POSTGRESQL OK: 15 PK, 19 FK y sus borrados, 21 casos adicionales. Sin cambios.';
END $$;
ROLLBACK;
