-- Ejecutar con SQL*Plus, en una SESION NUEVA y despues de 01 + 02.
-- Prueba todas las PK/FK y borrados padre, UNIQUE, CHECK y NOT NULL.
-- 75 rechazos + 4 casos validos: saldo cero, flags 0/1 y default pendiente.
-- ROLLBACK completo al finalizar; NO consume NEXTVAL ni modifica datos permanentes.
SET DEFINE OFF
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT FAILURE ROLLBACK
WHENEVER OSERROR EXIT FAILURE ROLLBACK
DECLARE
  n NUMBER;
  v_columnas VARCHAR2(4000);
  v_fks NUMBER := 0;
  v_pks NUMBER := 0;
  v_rechazos NUMBER := 0;
  v_aceptados NUMBER := 0;
  PROCEDURE rechazar(etiqueta VARCHAR2, sentencia VARCHAR2, esperado NUMBER) IS
    recibido NUMBER := 0;
  BEGIN
    SAVEPOINT caso_invalido;
    BEGIN
      EXECUTE IMMEDIATE sentencia;
    EXCEPTION WHEN OTHERS THEN
      recibido := SQLCODE;
    END;
    ROLLBACK TO caso_invalido;
    IF recibido <> esperado THEN
      RAISE_APPLICATION_ERROR(-20030, etiqueta || ': se esperaba ' || esperado ||
        ', se obtuvo ' || recibido || ' (0 significa dato invalido aceptado).');
    END IF;
    v_rechazos := v_rechazos + 1;
    DBMS_OUTPUT.PUT_LINE('OK rechazo: ' || etiqueta);
  END;
  PROCEDURE aceptar(etiqueta VARCHAR2, sentencia VARCHAR2) IS
  BEGIN
    SAVEPOINT caso_valido;
    EXECUTE IMMEDIATE sentencia;
    IF SQL%ROWCOUNT = 0 THEN
      RAISE_APPLICATION_ERROR(-20035, etiqueta || ': la prueba no afecto ninguna fila.');
    END IF;
    ROLLBACK TO caso_valido;
    v_aceptados := v_aceptados + 1;
    DBMS_OUTPUT.PUT_LINE('OK aceptacion: ' || etiqueta);
  END;
BEGIN
  SELECT COUNT(*) INTO n FROM sedes;
  IF n = 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Cargue 02_initial_data.sql: falta una fila en sedes.'); END IF;
  SELECT COUNT(*) INTO n FROM sedes WHERE id = 2147483647;
  IF n <> 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Id reservado por las pruebas ocupado en sedes.'); END IF;
  SELECT COUNT(*) INTO n FROM estatus;
  IF n = 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Cargue 02_initial_data.sql: falta una fila en estatus.'); END IF;
  SELECT COUNT(*) INTO n FROM estatus WHERE id = 2147483647;
  IF n <> 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Id reservado por las pruebas ocupado en estatus.'); END IF;
  SELECT COUNT(*) INTO n FROM tipos_persona;
  IF n = 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Cargue 02_initial_data.sql: falta una fila en tipos_persona.'); END IF;
  SELECT COUNT(*) INTO n FROM tipos_persona WHERE id = 2147483647;
  IF n <> 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Id reservado por las pruebas ocupado en tipos_persona.'); END IF;
  SELECT COUNT(*) INTO n FROM tipos_cuenta;
  IF n = 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Cargue 02_initial_data.sql: falta una fila en tipos_cuenta.'); END IF;
  SELECT COUNT(*) INTO n FROM tipos_cuenta WHERE id = 2147483647;
  IF n <> 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Id reservado por las pruebas ocupado en tipos_cuenta.'); END IF;
  SELECT COUNT(*) INTO n FROM tipos_prestamo;
  IF n = 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Cargue 02_initial_data.sql: falta una fila en tipos_prestamo.'); END IF;
  SELECT COUNT(*) INTO n FROM tipos_prestamo WHERE id = 2147483647;
  IF n <> 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Id reservado por las pruebas ocupado en tipos_prestamo.'); END IF;
  SELECT COUNT(*) INTO n FROM categorias_producto;
  IF n = 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Cargue 02_initial_data.sql: falta una fila en categorias_producto.'); END IF;
  SELECT COUNT(*) INTO n FROM categorias_producto WHERE id = 2147483647;
  IF n <> 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Id reservado por las pruebas ocupado en categorias_producto.'); END IF;
  SELECT COUNT(*) INTO n FROM personas;
  IF n = 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Cargue 02_initial_data.sql: falta una fila en personas.'); END IF;
  SELECT COUNT(*) INTO n FROM personas WHERE id = 2147483647;
  IF n <> 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Id reservado por las pruebas ocupado en personas.'); END IF;
  SELECT COUNT(*) INTO n FROM productos;
  IF n = 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Cargue 02_initial_data.sql: falta una fila en productos.'); END IF;
  SELECT COUNT(*) INTO n FROM productos WHERE id = 2147483647;
  IF n <> 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Id reservado por las pruebas ocupado en productos.'); END IF;
  SELECT COUNT(*) INTO n FROM cuentas;
  IF n = 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Cargue 02_initial_data.sql: falta una fila en cuentas.'); END IF;
  SELECT COUNT(*) INTO n FROM cuentas WHERE id = 2147483647;
  IF n <> 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Id reservado por las pruebas ocupado en cuentas.'); END IF;
  SELECT COUNT(*) INTO n FROM cuentas_personas;
  IF n = 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Cargue 02_initial_data.sql: falta una fila en cuentas_personas.'); END IF;
  SELECT COUNT(*) INTO n FROM saldos;
  IF n = 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Cargue 02_initial_data.sql: falta una fila en saldos.'); END IF;
  SELECT COUNT(*) INTO n FROM saldos WHERE id = 2147483647;
  IF n <> 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Id reservado por las pruebas ocupado en saldos.'); END IF;
  SELECT COUNT(*) INTO n FROM prestamos;
  IF n = 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Cargue 02_initial_data.sql: falta una fila en prestamos.'); END IF;
  SELECT COUNT(*) INTO n FROM prestamos WHERE id = 2147483647;
  IF n <> 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Id reservado por las pruebas ocupado en prestamos.'); END IF;
  SELECT COUNT(*) INTO n FROM detalles_prestamo;
  IF n = 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Cargue 02_initial_data.sql: falta una fila en detalles_prestamo.'); END IF;
  SELECT COUNT(*) INTO n FROM detalles_prestamo WHERE id = 2147483647;
  IF n <> 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Id reservado por las pruebas ocupado en detalles_prestamo.'); END IF;
  SELECT COUNT(*) INTO n FROM albaranes;
  IF n = 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Cargue 02_initial_data.sql: falta una fila en albaranes.'); END IF;
  SELECT COUNT(*) INTO n FROM albaranes WHERE id = 2147483647;
  IF n <> 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Id reservado por las pruebas ocupado en albaranes.'); END IF;
  SELECT COUNT(*) INTO n FROM detalles_albaran;
  IF n = 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Cargue 02_initial_data.sql: falta una fila en detalles_albaran.'); END IF;
  SELECT COUNT(*) INTO n FROM detalles_albaran WHERE id = 2147483647;
  IF n <> 0 THEN RAISE_APPLICATION_ERROR(-20031, 'Id reservado por las pruebas ocupado en detalles_albaran.'); END IF;
  SELECT COUNT(*) INTO n FROM user_constraints
  WHERE table_name IN ('SEDES', 'ESTATUS', 'TIPOS_PERSONA', 'TIPOS_CUENTA', 'TIPOS_PRESTAMO', 'CATEGORIAS_PRODUCTO', 'PERSONAS', 'PRODUCTOS', 'CUENTAS', 'CUENTAS_PERSONAS', 'SALDOS', 'PRESTAMOS', 'DETALLES_PRESTAMO', 'ALBARANES', 'DETALLES_ALBARAN') AND (status <> 'ENABLED' OR validated <> 'VALIDATED');
  IF n <> 0 THEN RAISE_APPLICATION_ERROR(-20032, 'Hay restricciones deshabilitadas o sin validar.'); END IF;
  SELECT COUNT(*) INTO n FROM user_sequences
  WHERE sequence_name IN ('SEQ_SEDES', 'SEQ_ESTATUS', 'SEQ_TIPOS_PERSONA', 'SEQ_TIPOS_CUENTA', 'SEQ_TIPOS_PRESTAMO', 'SEQ_CATEGORIAS_PRODUCTO', 'SEQ_PERSONAS', 'SEQ_PRODUCTOS', 'SEQ_CUENTAS', 'SEQ_SALDOS', 'SEQ_PRESTAMOS', 'SEQ_DETALLES_PRESTAMO', 'SEQ_ALBARANES', 'SEQ_DETALLES_ALBARAN')
    AND min_value = 1 AND max_value = 2147483647 AND increment_by = 1 AND cycle_flag = 'N';
  IF n <> 14 THEN RAISE_APPLICATION_ERROR(-20032, 'Faltan secuencias o tienen limites incompatibles con Java int.'); END IF;
  FOR fk IN (
    SELECT c.constraint_name, c.table_name, cc.column_name,
           padre.table_name AS tabla_padre, pc.column_name AS columna_padre, c.delete_rule
    FROM user_constraints c
    JOIN user_cons_columns cc ON cc.constraint_name = c.constraint_name
    JOIN user_constraints padre ON padre.constraint_name = c.r_constraint_name
    JOIN user_cons_columns pc ON pc.constraint_name = padre.constraint_name AND pc.position = cc.position
    WHERE c.constraint_type = 'R' AND c.table_name IN ('SEDES', 'ESTATUS', 'TIPOS_PERSONA', 'TIPOS_CUENTA', 'TIPOS_PRESTAMO', 'CATEGORIAS_PRODUCTO', 'PERSONAS', 'PRODUCTOS', 'CUENTAS', 'CUENTAS_PERSONAS', 'SALDOS', 'PRESTAMOS', 'DETALLES_PRESTAMO', 'ALBARANES', 'DETALLES_ALBARAN')
    ORDER BY c.constraint_name
  ) LOOP
    v_fks := v_fks + 1;
    IF fk.delete_rule <> 'NO ACTION' THEN RAISE_APPLICATION_ERROR(-20033, 'Borrado no restrictivo: ' || fk.constraint_name); END IF;
    SELECT COUNT(*) INTO n FROM user_ind_columns
    WHERE table_name = fk.table_name AND column_name = fk.column_name AND column_position = 1;
    IF n = 0 THEN RAISE_APPLICATION_ERROR(-20033, 'Falta indice de FK: ' || fk.constraint_name); END IF;
    rechazar('FK ' || fk.constraint_name, 'UPDATE ' || fk.table_name || ' SET ' ||
      fk.column_name || ' = 2147483647 WHERE ROWNUM = 1', -2291);
    rechazar('DELETE padre de ' || fk.constraint_name, 'DELETE FROM ' || fk.tabla_padre ||
      ' WHERE ' || fk.columna_padre || ' IN (SELECT ' || fk.column_name || ' FROM ' ||
      fk.table_name || ') AND ROWNUM = 1', -2292);
  END LOOP;
  IF v_fks <> 19 THEN RAISE_APPLICATION_ERROR(-20033, 'Se esperaban 19 FK; detectadas: ' || v_fks); END IF;
  FOR pk IN (SELECT table_name, constraint_name FROM user_constraints
    WHERE constraint_type = 'P' AND table_name IN ('SEDES', 'ESTATUS', 'TIPOS_PERSONA', 'TIPOS_CUENTA', 'TIPOS_PRESTAMO', 'CATEGORIAS_PRODUCTO', 'PERSONAS', 'PRODUCTOS', 'CUENTAS', 'CUENTAS_PERSONAS', 'SALDOS', 'PRESTAMOS', 'DETALLES_PRESTAMO', 'ALBARANES', 'DETALLES_ALBARAN')) LOOP
    v_pks := v_pks + 1;
    v_columnas := NULL;
    FOR col IN (SELECT column_name FROM user_tab_columns WHERE table_name = pk.table_name ORDER BY column_id) LOOP
      IF v_columnas IS NOT NULL THEN v_columnas := v_columnas || ','; END IF;
      v_columnas := v_columnas || col.column_name;
    END LOOP;
    rechazar('PK/fila duplicada de ' || pk.table_name, 'INSERT INTO ' || pk.table_name ||
      ' (' || v_columnas || ') SELECT ' || v_columnas || ' FROM ' || pk.table_name || ' WHERE ROWNUM = 1', -1);
  END LOOP;
  IF v_pks <> 15 THEN RAISE_APPLICATION_ERROR(-20034, 'Se esperaban 15 PK.'); END IF;
  rechazar('DNI unico', 'INSERT INTO personas (id,nombre,apellido,dni,tipo_persona_id,estatus_id,sede_id,fecha_registro) SELECT 2147483647,nombre,apellido,dni,tipo_persona_id,estatus_id,sede_id,fecha_registro FROM personas WHERE ROWNUM = 1', -1);
  rechazar('numero de cuenta unico', 'INSERT INTO cuentas (id,numero,tipo_cuenta_id,estatus_id,sede_id,fecha_apertura) SELECT 2147483647,numero,tipo_cuenta_id,estatus_id,sede_id,fecha_apertura FROM cuentas WHERE ROWNUM = 1', -1);
  rechazar('talonario unico', 'INSERT INTO albaranes (id,numero_talonario,persona_id,sede_id,fecha) SELECT 2147483647,numero_talonario,persona_id,sede_id,fecha FROM albaranes WHERE ROWNUM = 1', -1);
  rechazar('nombre de catalogo sin duplicados por mayusculas', 'INSERT INTO estatus (id,nombre) SELECT 2147483647,lower(nombre) FROM estatus WHERE ROWNUM = 1', -1);
  rechazar('id positivo', 'INSERT INTO sedes (id,nombre) VALUES (0,''VERIFICACION INVALIDA'')', -2290);
  rechazar('nombre obligatorio sin espacios', 'UPDATE personas SET nombre = ''   ''', -2290);
  rechazar('FK no nula', 'UPDATE personas SET sede_id = NULL', -1407);
  rechazar('titular solo 0 o 1', 'UPDATE cuentas_personas SET titular = 2', -2290);
  rechazar('saldo no negativo', 'UPDATE saldos SET monto = -0.01', -2290);
  rechazar('stock no negativo', 'UPDATE productos SET stock = -1', -2290);
  rechazar('precio de producto positivo', 'UPDATE productos SET precio = 0', -2290);
  rechazar('saldo pendiente de prestamo no negativo', 'UPDATE prestamos SET monto = -0.01', -2290);
  aceptar('prestamo pagado con monto cero', 'UPDATE prestamos SET monto = 0 WHERE ROWNUM = 1');
  rechazar('tasa no negativa', 'UPDATE prestamos SET tasa_interes = -0.01', -2290);
  rechazar('vencimiento no anterior al alta', 'UPDATE prestamos SET fecha_vencimiento = fecha - 1', -2290);
  rechazar('cantidad de prestamo positiva', 'UPDATE detalles_prestamo SET cantidad = 0', -2290);
  rechazar('precio de prestamo positivo', 'UPDATE detalles_prestamo SET precio_unitario = 0', -2290);
  rechazar('cantidad aceptada no negativa', 'UPDATE detalles_albaran SET cantidad_aceptada = -0.01', -2290);
  rechazar('cantidad rechazada no negativa', 'UPDATE detalles_albaran SET cantidad_rechazada = -0.01', -2290);
  rechazar('detalle albaran no vacio', 'UPDATE detalles_albaran SET cantidad_aceptada = 0, cantidad_rechazada = 0', -2290);
  rechazar('precio del albaran positivo', 'UPDATE detalles_albaran SET precio_unitario = 0', -2290);
  rechazar('liquidado solo 0 o 1', 'UPDATE albaranes SET liquidado = 2', -2290);
  rechazar('liquidado no nulo', 'UPDATE albaranes SET liquidado = NULL', -1407);
  aceptar('albaran pendiente con flag cero', 'UPDATE albaranes SET liquidado = 0 WHERE ROWNUM = 1');
  aceptar('albaran liquidado con flag uno', 'UPDATE albaranes SET liquidado = 1 WHERE ROWNUM = 1');
  SAVEPOINT caso_default_liquidado;
  INSERT INTO albaranes (id, numero_talonario, persona_id, sede_id, fecha)
    SELECT 2147483647, 'VERIF-LIQ-2147483647', persona_id, sede_id, fecha
    FROM albaranes WHERE ROWNUM = 1;
  SELECT liquidado INTO n FROM albaranes WHERE id = 2147483647;
  IF n IS NULL OR n <> 0 THEN
    RAISE_APPLICATION_ERROR(-20035, 'El albaran nuevo sin flag explicito debe quedar pendiente (0).');
  END IF;
  ROLLBACK TO caso_default_liquidado;
  v_aceptados := v_aceptados + 1;
  DBMS_OUTPUT.PUT_LINE('OK aceptacion: nuevo albaran pendiente por defecto');
  ROLLBACK;
  DBMS_OUTPUT.PUT_LINE('VERIFICACION ORACLE OK: 15 PK, 19 FK y sus borrados; ' ||
    v_rechazos || ' rechazos y ' || v_aceptados || ' casos validos. Sin cambios.');
EXCEPTION WHEN OTHERS THEN
  ROLLBACK;
  RAISE;
END;
/
