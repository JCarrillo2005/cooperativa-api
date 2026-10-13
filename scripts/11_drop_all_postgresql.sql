-- DESTRUCTIVO: elimina exclusivamente las 15 tablas y 14 secuencias de esta app.
-- Sin CASCADE: si otro objeto depende de ellas, se detiene para no borrarlo.
-- Hacer respaldo antes. El borrado de PostgreSQL ocurre en una transaccion.
\set ON_ERROR_STOP on
\prompt 'Escriba BORRAR_COOPERATIVA para borrar el esquema de la app: ' confirmar
SELECT :'confirmar' = 'BORRAR_COOPERATIVA' AS aprobado \gset
\if :aprobado
BEGIN;
DROP TABLE IF EXISTS detalles_albaran;
DROP TABLE IF EXISTS albaranes;
DROP TABLE IF EXISTS detalles_prestamo;
DROP TABLE IF EXISTS prestamos;
DROP TABLE IF EXISTS saldos;
DROP TABLE IF EXISTS cuentas_personas;
DROP TABLE IF EXISTS cuentas;
DROP TABLE IF EXISTS productos;
DROP TABLE IF EXISTS personas;
DROP TABLE IF EXISTS categorias_producto;
DROP TABLE IF EXISTS tipos_prestamo;
DROP TABLE IF EXISTS tipos_cuenta;
DROP TABLE IF EXISTS tipos_persona;
DROP TABLE IF EXISTS estatus;
DROP TABLE IF EXISTS sedes;
DROP SEQUENCE IF EXISTS seq_detalles_albaran;
DROP SEQUENCE IF EXISTS seq_albaranes;
DROP SEQUENCE IF EXISTS seq_detalles_prestamo;
DROP SEQUENCE IF EXISTS seq_prestamos;
DROP SEQUENCE IF EXISTS seq_saldos;
DROP SEQUENCE IF EXISTS seq_cuentas;
DROP SEQUENCE IF EXISTS seq_productos;
DROP SEQUENCE IF EXISTS seq_personas;
DROP SEQUENCE IF EXISTS seq_categorias_producto;
DROP SEQUENCE IF EXISTS seq_tipos_prestamo;
DROP SEQUENCE IF EXISTS seq_tipos_cuenta;
DROP SEQUENCE IF EXISTS seq_tipos_persona;
DROP SEQUENCE IF EXISTS seq_estatus;
DROP SEQUENCE IF EXISTS seq_sedes;
COMMIT;
\echo Objetos de la cooperativa eliminados.
\else
\echo Cancelado: no se elimino ningun objeto.
\endif
