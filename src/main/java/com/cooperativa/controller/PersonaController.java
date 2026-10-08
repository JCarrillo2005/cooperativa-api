package com.cooperativa.controller;

import com.cooperativa.dao.EstatusDAO;
import com.cooperativa.dao.PersonaDAO;
import com.cooperativa.dao.SedeDAO;
import com.cooperativa.dao.TipoPersonaDAO;
import com.cooperativa.model.Persona;
import java.sql.SQLException;
import java.util.List;

public class PersonaController {

    private final PersonaDAO personaDAO = new PersonaDAO();
    private final TipoPersonaDAO tipoPersonaDAO = new TipoPersonaDAO();
    private final EstatusDAO estatusDAO = new EstatusDAO();
    private final SedeDAO sedeDAO = new SedeDAO();

    // ---------- Registrar y actualizar ----------

    /** Registra una persona. Si no trae estatus, queda como ACTIVO. */
    public Persona registrar(Persona p) throws NegocioException {
        if (p == null) {
            throw new NegocioException("No se recibieron los datos de la persona.");
        }
        try {
            if (p.getEstatusId() == 0) {
                p.setEstatusId(Catalogos.estatusId(Catalogos.ESTATUS_ACTIVO));
            }
            validar(p);
            if (personaDAO.obtenerPorDni(p.getDni()) != null) {
                throw new NegocioException("Ya existe una persona con el DNI " + p.getDni() + ".");
            }
            personaDAO.guardar(p);
            return p;
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo registrar la persona", e);
        }
    }

    public Persona actualizar(Persona p) throws NegocioException {
        if (p == null) {
            throw new NegocioException("No se recibieron los datos de la persona.");
        }
        try {
            if (personaDAO.obtenerPorId(p.getId()) == null) {
                throw new NegocioException("La persona no existe.");
            }
            validar(p);
            Persona otra = personaDAO.obtenerPorDni(p.getDni());
            if (otra != null && otra.getId() != p.getId()) {
                throw new NegocioException("Otra persona ya tiene el DNI " + p.getDni() + ".");
            }
            personaDAO.actualizar(p);
            return p;
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo actualizar la persona", e);
        }
    }

    // ---------- Estatus ----------

    public void activar(int personaId) throws NegocioException {
        cambiarEstatus(personaId, Catalogos.ESTATUS_ACTIVO);
    }

    public void desactivar(int personaId) throws NegocioException {
        cambiarEstatus(personaId, Catalogos.ESTATUS_INACTIVO);
    }

    public void cambiarEstatus(int personaId, String nombreEstatus) throws NegocioException {
        try {
            if (personaDAO.obtenerPorId(personaId) == null) {
                throw new NegocioException("La persona no existe.");
            }
            personaDAO.cambiarEstatus(personaId, Catalogos.estatusId(nombreEstatus));
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo cambiar el estatus de la persona", e);
        }
    }

    // ---------- Consultas ----------

    /** Devuelve la persona o lanza NegocioException si no existe. */
    public Persona obtenerPorId(int id) throws NegocioException {
        try {
            Persona p = personaDAO.obtenerPorId(id);
            if (p == null) {
                throw new NegocioException("La persona con id " + id + " no existe.");
            }
            return p;
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo consultar la persona", e);
        }
    }

    /** Devuelve la persona o null si no existe. */
    public Persona buscarPorDni(String dni) throws NegocioException {
        try {
            return personaDAO.obtenerPorDni(dni == null ? "" : dni.trim());
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo buscar la persona", e);
        }
    }

    /** Regla de negocio: solo las personas activas pueden operar. Úsalo desde otros controllers. */
    public Persona obtenerActiva(int id) throws NegocioException {
        Persona p = obtenerPorId(id);
        if (p.getEstatusId() != Catalogos.estatusId(Catalogos.ESTATUS_ACTIVO)) {
            throw new NegocioException("La persona " + p + " no está activa.");
        }
        return p;
    }

    public List<Persona> listarTodas() throws NegocioException {
        try {
            return personaDAO.obtenerTodos();
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo listar las personas", e);
        }
    }

    public List<Persona> listarSocios() throws NegocioException {
        try {
            return personaDAO.obtenerPorTipo(Catalogos.tipoPersonaId(Catalogos.TIPO_PERSONA_SOCIO));
        } catch (SQLException e) {
            throw NegocioException.deSQL("No se pudo listar los socios", e);
        }
    }

    // ---------- Eliminar ----------

    /** Borrado físico. Si la persona tiene cuentas, préstamos o albaranes, conviene desactivarla. */
    public void eliminar(int personaId) throws NegocioException {
        try {
            if (personaDAO.obtenerPorId(personaId) == null) {
                throw new NegocioException("La persona no existe.");
            }
            personaDAO.eliminar(personaId);
        } catch (SQLException e) {
            if (e.getErrorCode() == 2292) {
                throw new NegocioException("No se puede eliminar: la persona tiene cuentas, préstamos o "
                    + "entregas registradas. Desactívala en su lugar.", e);
            }
            throw NegocioException.deSQL("No se pudo eliminar la persona", e);
        }
    }

    // ---------- Validaciones ----------

    private void validar(Persona p) throws NegocioException, SQLException {
        p.setNombre(requerido(p.getNombre(), "El nombre", 60));
        p.setApellido(requerido(p.getApellido(), "El apellido", 60));
        p.setDni(requerido(p.getDni(), "El DNI", 20));

        if (p.getDireccion() != null && p.getDireccion().length() > 150) {
            throw new NegocioException("La dirección no puede pasar de 150 caracteres.");
        }
        if (p.getTelefono() != null && p.getTelefono().length() > 20) {
            throw new NegocioException("El teléfono no puede pasar de 20 caracteres.");
        }
        if (tipoPersonaDAO.obtenerPorId(p.getTipoPersonaId()) == null) {
            throw new NegocioException("El tipo de persona no existe.");
        }
        if (estatusDAO.obtenerPorId(p.getEstatusId()) == null) {
            throw new NegocioException("El estatus no existe.");
        }
        if (sedeDAO.obtenerPorId(p.getSedeId()) == null) {
            throw new NegocioException("La sede no existe.");
        }
    }

    private String requerido(String valor, String campo, int max) throws NegocioException {
        if (valor == null || valor.trim().isEmpty()) {
            throw new NegocioException(campo + " es obligatorio.");
        }
        String limpio = valor.trim();
        if (limpio.length() > max) {
            throw new NegocioException(campo + " no puede pasar de " + max + " caracteres.");
        }
        return limpio;
    }
}