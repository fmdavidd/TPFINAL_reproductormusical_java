package Service;

import Enumeraciones.NivelAdmin;
import Excepciones.PermisoDenegadoException;
import modelo.Usuario;
import modelo.UsuarioAdministrador;

// Reglas compartidas por los servicios; una sesion ausente nunca otorga permisos.
public class Acceso {
    public static Usuario autenticado(SesionService sesion) throws PermisoDenegadoException {
        Usuario u = sesion == null ? null : sesion.getUsuarioActual();
        if (u == null || !u.isActivo()) throw new PermisoDenegadoException("Debe iniciar sesion.");
        return u;
    }

    public static void administrador(SesionService sesion, boolean superadmin) throws PermisoDenegadoException {
        Usuario u = autenticado(sesion);
        if (!(u instanceof UsuarioAdministrador)) throw new PermisoDenegadoException("Requiere administrador.");
        NivelAdmin nivel = ((UsuarioAdministrador) u).getNivelAdmin();
        if (nivel == null || nivel == NivelAdmin.VISUALIZANTE || (superadmin && nivel != NivelAdmin.SUPERADMIN))
            throw new PermisoDenegadoException(superadmin ? "Requiere SUPERADMIN." : "Requiere EDITOR o SUPERADMIN.");
    }

    public static void propietario(SesionService sesion, int id) throws PermisoDenegadoException {
        if (autenticado(sesion).getIdUsuario() != id) throw new PermisoDenegadoException("Solo el propietario puede realizar esta operacion.");
    }

    public static void texto(String valor, int maximo, String campo) {
        if (valor == null || valor.trim().isEmpty() || valor.length() > maximo)
            throw new IllegalArgumentException(campo + " es obligatorio (maximo " + maximo + " caracteres).");
    }

    public static void resultado(boolean ok) throws Excepciones.ConexionBDException {
        if (!ok) throw new Excepciones.ConexionBDException("La base no confirmo la operacion.");
    }
}
