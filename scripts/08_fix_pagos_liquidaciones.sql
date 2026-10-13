-- Oracle 10g XE: permite saldo pendiente cero y registra albaranes liquidados.
-- EJECUCION MANUAL: detener la API y usar una sesion nueva del esquema propietario.
-- No ejecutar como SYS/SYSTEM ni con CURRENT_SCHEMA de otro usuario.
-- DDL confirma implicitamente. Un fallo NO revierte DDL anterior; esta migracion
-- reconoce sus estados intermedios para poder reintentarse tras corregir la causa.
-- No elimina datos/tablas/usuarios ni reinicia secuencias. No registra pagos.
--
-- Albaranes antiguos: no existe un historial que identifique sus pagos.
-- Solo la copia COOPERATIVA de desarrollo o un esquema QA reservado TF_QA_*,
-- con las 38 filas demo originales
-- y sin pagos previos, puede inicializarse automaticamente como pendiente.
-- Otros esquemas con albaranes deben conciliar primero: ver LEEME_FIX_PAGOS.md.
SET DEFINE OFF
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT FAILURE ROLLBACK
WHENEVER OSERROR EXIT FAILURE ROLLBACK

DECLARE
  v_n NUMBER;
  v_filas NUMBER := 0;
  v_demo BOOLEAN := TRUE;
  v_liquidado NUMBER;
  v_nullable VARCHAR2(1);
  v_default VARCHAR2(32767);
  v_pre VARCHAR2(32767);
  v_guardia VARCHAR2(32767);
  v_flag VARCHAR2(32767);
  v_condicion VARCHAR2(32767);

  FUNCTION normalizar(texto VARCHAR2) RETURN VARCHAR2 IS
  BEGIN
    RETURN REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(
      UPPER(texto), ' ', ''), CHR(9), ''), CHR(10), ''), CHR(13), ''),
      '"', ''), '(', ''), ')', '');
  END;

  PROCEDURE exigir(condicion BOOLEAN, mensaje VARCHAR2) IS
  BEGIN
    IF condicion IS NULL OR NOT condicion THEN
      RAISE_APPLICATION_ERROR(-20080, mensaje);
    END IF;
  END;

  -- Nombres fijos del contrato; no se aceptan identificadores del operador.
  PROCEDURE tabla(nombre VARCHAR2, columnas VARCHAR2, filas_demo NUMBER) IS
    inicio PLS_INTEGER := 1;
    fin PLS_INTEGER;
    columna VARCHAR2(30);
    lista VARCHAR2(1000) := columnas || ',';
    cantidad NUMBER;
  BEGIN
    SELECT COUNT(*) INTO cantidad FROM user_tables WHERE table_name = nombre;
    exigir(cantidad = 1, 'Falta tabla del proyecto: ' || nombre);
    LOOP
      fin := INSTR(lista, ',', inicio);
      EXIT WHEN fin = 0;
      columna := SUBSTR(lista, inicio, fin - inicio);
      SELECT COUNT(*) INTO cantidad FROM user_tab_columns
      WHERE table_name = nombre AND column_name = columna;
      exigir(cantidad = 1, 'Falta columna: ' || nombre || '.' || columna);
      inicio := fin + 1;
    END LOOP;
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM ' || nombre INTO cantidad;
    v_filas := v_filas + cantidad;
    v_demo := v_demo AND cantidad = filas_demo;
  END;

  FUNCTION restriccion(nombre VARCHAR2, tabla_esperada VARCHAR2,
                       columna_esperada VARCHAR2) RETURN VARCHAR2 IS
    cantidad NUMBER;
    tabla_real VARCHAR2(30);
    tipo VARCHAR2(1);
    estado VARCHAR2(8);
    validacion VARCHAR2(13);
    texto VARCHAR2(32767);
  BEGIN
    SELECT COUNT(*) INTO cantidad FROM user_constraints WHERE constraint_name = nombre;
    IF cantidad = 0 THEN RETURN NULL; END IF;
    SELECT table_name, constraint_type, status, validated, search_condition
      INTO tabla_real, tipo, estado, validacion, texto
      FROM user_constraints WHERE constraint_name = nombre;
    exigir(tabla_real = tabla_esperada AND tipo = 'C'
      AND estado = 'ENABLED' AND validacion = 'VALIDATED',
      'Restriccion incompatible, deshabilitada o no validada: ' || nombre);
    SELECT COUNT(*) INTO cantidad FROM user_cons_columns
      WHERE constraint_name = nombre;
    exigir(cantidad = 1, 'La restriccion debe afectar una sola columna: ' || nombre);
    SELECT COUNT(*) INTO cantidad FROM user_cons_columns
      WHERE constraint_name = nombre AND column_name = columna_esperada;
    exigir(cantidad = 1, 'Columna inesperada en restriccion: ' || nombre);
    RETURN normalizar(texto);
  END;

  PROCEDURE ddl(sentencia VARCHAR2) IS
  BEGIN
    EXECUTE IMMEDIATE sentencia;
    DBMS_OUTPUT.PUT_LINE('APLICADO: ' || sentencia);
  END;
BEGIN
  exigir(USER NOT IN ('SYS', 'SYSTEM'), 'Conectese al esquema del proyecto, no SYS/SYSTEM.');
  exigir(SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA') = USER,
    'CURRENT_SCHEMA debe coincidir con el usuario propietario conectado.');

  -- Preflight completo ANTES del primer DDL: 15 tablas y sus 68 columnas base.
  tabla('SEDES', 'ID,NOMBRE,DIRECCION,DEPARTAMENTO', 2);
  tabla('ESTATUS', 'ID,NOMBRE,DESCRIPCION', 4);
  tabla('TIPOS_PERSONA', 'ID,NOMBRE', 3);
  tabla('TIPOS_CUENTA', 'ID,NOMBRE', 2);
  tabla('TIPOS_PRESTAMO', 'ID,NOMBRE', 3);
  tabla('CATEGORIAS_PRODUCTO', 'ID,NOMBRE', 2);
  tabla('PERSONAS', 'ID,NOMBRE,APELLIDO,DNI,DIRECCION,TELEFONO,TIPO_PERSONA_ID,ESTATUS_ID,SEDE_ID,FECHA_REGISTRO', 3);
  tabla('PRODUCTOS', 'ID,NOMBRE,DESCRIPCION,CATEGORIA_ID,PRECIO,STOCK', 4);
  tabla('CUENTAS', 'ID,NUMERO,TIPO_CUENTA_ID,ESTATUS_ID,SEDE_ID,FECHA_APERTURA', 2);
  tabla('CUENTAS_PERSONAS', 'CUENTA_ID,PERSONA_ID,TITULAR', 3);
  tabla('SALDOS', 'ID,CUENTA_ID,MONTO,FECHA', 3);
  tabla('PRESTAMOS', 'ID,PERSONA_ID,TIPO_PRESTAMO_ID,ESTATUS_ID,MONTO,TASA_INTERES,FECHA,FECHA_VENCIMIENTO', 2);
  tabla('DETALLES_PRESTAMO', 'ID,PRESTAMO_ID,PRODUCTO_ID,CANTIDAD,PRECIO_UNITARIO', 2);
  tabla('ALBARANES', 'ID,NUMERO_TALONARIO,PERSONA_ID,SEDE_ID,FECHA', 1);
  tabla('DETALLES_ALBARAN', 'ID,ALBARAN_ID,PRODUCTO_ID,CANTIDAD_ACEPTADA,CANTIDAD_RECHAZADA,PRECIO_UNITARIO', 2);

  SELECT COUNT(*) INTO v_n FROM user_constraints
  WHERE table_name IN ('SEDES','ESTATUS','TIPOS_PERSONA','TIPOS_CUENTA',
    'TIPOS_PRESTAMO','CATEGORIAS_PRODUCTO','PERSONAS','PRODUCTOS','CUENTAS',
    'CUENTAS_PERSONAS','SALDOS','PRESTAMOS','DETALLES_PRESTAMO','ALBARANES','DETALLES_ALBARAN')
    AND (status <> 'ENABLED' OR validated <> 'VALIDATED');
  exigir(v_n = 0, 'Hay restricciones deshabilitadas o no validadas; revisarlas antes de migrar.');
  SELECT COUNT(*) INTO v_n FROM user_tab_columns
    WHERE table_name = 'PRESTAMOS' AND column_name = 'MONTO'
      AND data_type = 'NUMBER' AND data_precision = 12 AND data_scale = 2 AND nullable = 'N';
  exigir(v_n = 1, 'Se esperaba PRESTAMOS.MONTO NUMBER(12,2) NOT NULL.');
  EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM prestamos WHERE monto < 0 OR monto IS NULL' INTO v_n;
  exigir(v_n = 0, 'Hay saldos de prestamo invalidos; no se modificaran automaticamente.');

  v_pre := restriccion('CK_PRE_MONTO', 'PRESTAMOS', 'MONTO');
  v_guardia := restriccion('CK_PRE_MONTO_NO_NEG', 'PRESTAMOS', 'MONTO');
  exigir(v_pre IS NULL OR v_pre IN ('MONTO>0', 'MONTO>=0'), 'CK_PRE_MONTO tiene una expresion inesperada.');
  exigir(v_guardia IS NULL OR v_guardia = 'MONTO>=0', 'CK_PRE_MONTO_NO_NEG tiene una expresion inesperada.');
  exigir(v_pre IS NOT NULL OR v_guardia IS NOT NULL,
    'Falta CK_PRE_MONTO y su guardia de migracion; revise el esquema antes de continuar.');

  -- No ocultar otro CHECK que aun pudiera impedir llegar a saldo cero.
  FOR c IN (SELECT c.constraint_name, c.search_condition
    FROM user_constraints c JOIN user_cons_columns cc
      ON cc.constraint_name = c.constraint_name AND cc.table_name = c.table_name
    WHERE c.table_name = 'PRESTAMOS' AND cc.column_name = 'MONTO'
      AND c.constraint_type = 'C'
      AND c.constraint_name NOT IN ('CK_PRE_MONTO', 'CK_PRE_MONTO_NO_NEG')) LOOP
    v_condicion := normalizar(c.search_condition);
    exigir(v_condicion = 'MONTOISNOTNULL',
      'Otro CHECK sobre PRESTAMOS.MONTO requiere revision: ' || c.constraint_name);
  END LOOP;

  SELECT COUNT(*) INTO v_liquidado FROM user_tab_columns
    WHERE table_name = 'ALBARANES' AND column_name = 'LIQUIDADO';
  v_flag := restriccion('CK_AL_LIQUIDADO', 'ALBARANES', 'LIQUIDADO');
  exigir(v_flag IS NULL OR v_flag = 'LIQUIDADOIN0,1', 'CK_AL_LIQUIDADO tiene una expresion inesperada.');
  IF v_liquidado = 1 THEN
    SELECT COUNT(*) INTO v_n FROM user_tab_columns
      WHERE table_name = 'ALBARANES' AND column_name = 'LIQUIDADO'
        AND data_type = 'NUMBER' AND data_precision = 1 AND data_scale = 0;
    exigir(v_n = 1, 'Se esperaba ALBARANES.LIQUIDADO NUMBER(1).');
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM albaranes WHERE liquidado IS NULL OR liquidado NOT IN (0,1)' INTO v_n;
    exigir(v_n = 0, 'Conciliar LIQUIDADO: existen filas nulas o distintas de 0/1. No se cambia ningun valor.');
    FOR c IN (SELECT c.constraint_name, c.search_condition
      FROM user_constraints c JOIN user_cons_columns cc
        ON cc.constraint_name = c.constraint_name AND cc.table_name = c.table_name
      WHERE c.table_name = 'ALBARANES' AND cc.column_name = 'LIQUIDADO'
        AND c.constraint_type = 'C' AND c.constraint_name <> 'CK_AL_LIQUIDADO') LOOP
      v_condicion := normalizar(c.search_condition);
      exigir(v_condicion IN ('LIQUIDADOISNOTNULL', 'LIQUIDADOIN0,1'),
        'Otro CHECK sobre ALBARANES.LIQUIDADO requiere revision: ' || c.constraint_name);
    END LOOP;
  ELSE
    exigir(v_flag IS NULL, 'CK_AL_LIQUIDADO existe sin la columna esperada.');
    EXECUTE IMMEDIATE 'SELECT COUNT(*) FROM albaranes' INTO v_n;
    IF v_n > 0 THEN
      exigir((USER = 'COOPERATIVA' OR SUBSTR(USER, 1, 6) = 'TF_QA_') AND v_demo AND v_filas = 38,
        'Albaranes anteriores sin historial de pago: concilie primero LIQUIDADO. Consulte LEEME_FIX_PAGOS.md.');

      -- Huella adicional de la copia demo conocida; el conteo solo no basta.
      EXECUTE IMMEDIATE q'[SELECT COUNT(*) FROM personas
        WHERE dni IN ('DEMO-SOCIO-001','DEMO-SOCIO-002','DEMO-EMPL-001')]' INTO v_n;
      exigir(v_n = 3, 'Las personas no corresponden a la copia demo original. Conciliar primero.');
      EXECUTE IMMEDIATE q'[SELECT COUNT(*) FROM saldos s JOIN cuentas c ON c.id=s.cuenta_id
        WHERE (c.numero='CTA-DEMO-001' AND s.monto=1000 AND s.fecha=DATE '2026-01-10')
           OR (c.numero='CTA-DEMO-001' AND s.monto=1250 AND s.fecha=DATE '2026-01-11')
           OR (c.numero='CTA-DEMO-002' AND s.monto=500 AND s.fecha=DATE '2026-01-10')]' INTO v_n;
      exigir(v_n = 3, 'Los saldos demo cambiaron: podria haber pagos previos. Conciliar primero.');
      EXECUTE IMMEDIATE q'[SELECT COUNT(*) FROM prestamos p
        JOIN personas pe ON pe.id=p.persona_id
        JOIN tipos_prestamo t ON t.id=p.tipo_prestamo_id
        JOIN estatus e ON e.id=p.estatus_id
        WHERE e.nombre='ACTIVO' AND p.fecha=DATE '2026-01-10'
          AND p.fecha_vencimiento=DATE '2027-01-10'
          AND ((pe.dni='DEMO-SOCIO-001' AND t.nombre='CONSUMO_PRODUCTOS' AND p.monto=192 AND p.tasa_interes=10)
            OR (pe.dni='DEMO-SOCIO-002' AND t.nombre='EFECTIVO' AND p.monto=1500 AND p.tasa_interes=12))]' INTO v_n;
      exigir(v_n = 2, 'Los prestamos demo cambiaron. Conciliar primero los albaranes.');
      EXECUTE IMMEDIATE q'[SELECT COUNT(*) FROM albaranes a JOIN personas p ON p.id=a.persona_id
        WHERE a.numero_talonario='TAL-DEMO-001' AND p.dni='DEMO-SOCIO-001'
          AND a.fecha=DATE '2026-01-12']' INTO v_n;
      exigir(v_n = 1, 'El albaran no corresponde a la copia demo original. Conciliar primero.');
      EXECUTE IMMEDIATE q'[SELECT COUNT(*) FROM detalles_albaran d JOIN productos p ON p.id=d.producto_id
        WHERE (p.nombre='Tomate DEMO' AND d.cantidad_aceptada=125.5 AND d.cantidad_rechazada=4.5 AND d.precio_unitario=7.5)
           OR (p.nombre='Cebolla DEMO' AND d.cantidad_aceptada=80 AND d.cantidad_rechazada=0 AND d.precio_unitario=5)]' INTO v_n;
      exigir(v_n = 2, 'Los detalles del albaran demo cambiaron. Conciliar primero.');
      DBMS_OUTPUT.PUT_LINE('Esquema demo autorizado: 38 filas originales; albaran sin pagos previos, pendiente inicial.');
    END IF;
  END IF;
  DBMS_OUTPUT.PUT_LINE('PREFLIGHT OK: estructura, checks y datos compatibles. Comienza el DDL.');

  -- El guardia mantiene protegidos los negativos durante el cambio y reintentos.
  IF v_pre IS NULL OR v_pre = 'MONTO>0' THEN
    IF v_guardia IS NULL THEN
      ddl('ALTER TABLE prestamos ADD CONSTRAINT ck_pre_monto_no_neg CHECK (monto >= 0) ENABLE VALIDATE');
    END IF;
    IF v_pre IS NOT NULL THEN
      ddl('ALTER TABLE prestamos DROP CONSTRAINT ck_pre_monto');
    END IF;
    ddl('ALTER TABLE prestamos RENAME CONSTRAINT ck_pre_monto_no_neg TO ck_pre_monto');
  ELSIF v_guardia IS NOT NULL THEN
    -- Limpieza de la unica guardia reconocida, sin tocar otras restricciones.
    ddl('ALTER TABLE prestamos DROP CONSTRAINT ck_pre_monto_no_neg');
  ELSE
    DBMS_OUTPUT.PUT_LINE('SIN CAMBIO: CK_PRE_MONTO ya permite monto >= 0.');
  END IF;

  IF v_liquidado = 0 THEN
    ddl('ALTER TABLE albaranes ADD (liquidado NUMBER(1) DEFAULT 0 NOT NULL CONSTRAINT ck_al_liquidado CHECK (liquidado IN (0,1)))');
  ELSE
    -- No sobrescribir flags de albaranes que ya fueron liquidados.
    SELECT nullable, data_default INTO v_nullable, v_default FROM user_tab_columns
      WHERE table_name = 'ALBARANES' AND column_name = 'LIQUIDADO';
    IF v_default IS NULL OR normalizar(v_default) <> '0' THEN
      ddl('ALTER TABLE albaranes MODIFY (liquidado DEFAULT 0)');
    END IF;
    IF v_nullable = 'Y' THEN
      ddl('ALTER TABLE albaranes MODIFY (liquidado NOT NULL)');
    END IF;
    IF v_flag IS NULL THEN
      ddl('ALTER TABLE albaranes ADD CONSTRAINT ck_al_liquidado CHECK (liquidado IN (0,1)) ENABLE VALIDATE');
    END IF;
    DBMS_OUTPUT.PUT_LINE('LIQUIDADO: valores existentes conservados.');
  END IF;
  exigir(restriccion('CK_PRE_MONTO', 'PRESTAMOS', 'MONTO') = 'MONTO>=0', 'Fallo de comprobacion final: CK_PRE_MONTO.');
  exigir(restriccion('CK_AL_LIQUIDADO', 'ALBARANES', 'LIQUIDADO') = 'LIQUIDADOIN0,1', 'Fallo de comprobacion final: CK_AL_LIQUIDADO.');
  SELECT COUNT(*) INTO v_n FROM user_tab_columns
    WHERE table_name='ALBARANES' AND column_name='LIQUIDADO' AND nullable='N';
  exigir(v_n = 1, 'Fallo de comprobacion final: LIQUIDADO debe ser NOT NULL.');
  DBMS_OUTPUT.PUT_LINE('MIGRACION 08 OK: saldo cero permitido; liquidacion registrada una sola vez por la aplicacion.');
EXCEPTION WHEN OTHERS THEN
  DBMS_OUTPUT.PUT_LINE('MIGRACION NO COMPLETADA. La API debe seguir detenida.');
  DBMS_OUTPUT.PUT_LINE('Oracle confirma DDL implicitamente: no se afirma rollback de cambios de estructura.');
  DBMS_OUTPUT.PUT_LINE('Corrija la causa y vuelva a ejecutar este script; no borre ni recargue los datos.');
  RAISE;
END;
/
