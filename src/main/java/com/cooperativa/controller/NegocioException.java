package com.cooperativa.controller;

import java.sql.SQLException;

/** Error de regla de negocio o de datos, con un mensaje listo para mostrar al usuario. */
public class NegocioException extends Exception {

    public NegocioException(String mensaje) {
        super(mensaje);
    }

    public NegocioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }

    /** Traduce un SQLException de Oracle a un mensaje entendible. */
    public static NegocioException deSQL(String contexto, SQLException e) {
        String detalle;
        switch (e.getErrorCode()) {
            case 1:
                detalle = "ya existe un registro con ese valor único (por ejemplo DNI, número de cuenta o talonario).";
                break;
            case 2291:
                detalle = "hace referencia a un dato que no existe (sede, tipo, estatus, persona o producto).";
                break;
            case 2292:
                detalle = "tiene registros asociados y no se puede eliminar.";
                break;
            case 12899:
                detalle = "uno de los valores es demasiado largo para su campo.";
                break;
            default:
                detalle = "error de base de datos (" + e.getMessage() + ").";
        }
        return new NegocioException(contexto + ": " + detalle, e);
    }
}