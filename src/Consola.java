import Service.*;
import Excepciones.*;
import Enumeraciones.*;
import modelo.*;
import Seguridad.Seguridad;
import java.util.Scanner;
import java.util.NoSuchElementException;

public class Consola {
    private final Scanner entrada = new Scanner(System.in);
    private final UsuarioService usuarios = new UsuarioService();
    private final SesionService sesion = new SesionService(usuarios);
    private final ArtistaService artistas = new ArtistaService(sesion);
    private final CancionService canciones = new CancionService(sesion);
    private final AlbumService albumes = new AlbumService(sesion);
    private final PlaylistService playlists = new PlaylistService(sesion, canciones);

    public Consola() throws ConexionBDException { }

    public void ejecutar() {
        System.out.println("GESTOR MUSICAL - Programacion II");
        boolean continuar = true;
        while (continuar) {
            try {
                Usuario actual = sesion.getUsuarioActual();
                if (actual == null) {
                    System.out.println("\n1. Registrarse | 2. Iniciar sesion | 0. Salir");
                    switch (numero("Opcion: ")) {
                        case 1: registrar(); break;
                        case 2:
                            Usuario u = sesion.iniciarSesion(texto("Email: "), texto("Contrasenia: "));
                            System.out.println("Bienvenido/a, " + u.getNombre()); break;
                        case 0: continuar = false; break;
                        default: System.out.println("Opcion invalida.");
                    }
                } else {
                    System.out.println("\n" + actual.getNombre() + " (" + actual.getRol() + ")");
                    System.out.println("1. Artistas | 2. Albumes | 3. Canciones | 4. Playlists");
                    if (actual instanceof UsuarioNormal) System.out.println("5. Favoritos");
                    if (actual.puedeAdministrar()) System.out.println("6. Usuarios | 7. Mi historial administrativo");
                    System.out.println("8. Cerrar sesion | 0. Salir");
                    switch (numero("Opcion: ")) {
                        case 1: menuCatalogo(1); break;
                        case 2: menuCatalogo(2); break;
                        case 3: menuCatalogo(3); break;
                        case 4: menuPlaylists(); break;
                        case 5:
                            if (!(actual instanceof UsuarioNormal)) throw new PermisoDenegadoException("Opcion para usuarios normales.");
                            menuFavoritos(); break;
                        case 6: menuUsuarios(); break;
                        case 7:
                            if (!actual.puedeAdministrar()) throw new PermisoDenegadoException("Requiere administrador.");
                            System.out.println(((UsuarioAdministrador) actual).getRegistroAcciones()); break;
                        case 8: sesion.cerrarSesion(); System.out.println("Sesion cerrada."); break;
                        case 0: continuar = false; break;
                        default: System.out.println("Opcion invalida.");
                    }
                }
            } catch (NoSuchElementException e) {
                continuar = false; // Fin de entrada: tambien permite redirigir un archivo de demostracion.
            } catch (ConexionBDException | CredencialesInvalidasException | ElementoDuplicadoException
                     | ElementoNoEncontradoException | PermisoDenegadoException
                     | IllegalArgumentException | IllegalStateException | IndexOutOfBoundsException e) {
                System.out.println("No se pudo completar: " + e.getMessage());
            }
        }
        sesion.cerrarSesion();
        System.out.println("Hasta luego.");
    }

    private void registrar() throws ConexionBDException, ElementoDuplicadoException, ElementoNoEncontradoException, PermisoDenegadoException {
        sesion.registrar(texto("Nombre: "), texto("Email: "), texto("Contrasenia: "));
        System.out.println("Usuario registrado. Ya puede iniciar sesion.");
    }

    private boolean puedeEditarCatalogo() {
        Usuario u = sesion.getUsuarioActual();
        return u instanceof UsuarioAdministrador && ((UsuarioAdministrador) u).getNivelAdmin() != NivelAdmin.VISUALIZANTE;
    }

    private void menuCatalogo(int entidad) throws ConexionBDException, ElementoDuplicadoException, ElementoNoEncontradoException, PermisoDenegadoException {
        while (true) {
            System.out.println("\n1. Listar | 2. Consultar por ID | 0. Volver");
            if (puedeEditarCatalogo()) System.out.println("3. Crear | 4. Modificar | 5. Dar de baja");
            int opcion = numero("Opcion: ");
            if (opcion == 0) return;
            if (opcion == 1) { listarCatalogo(entidad); continue; }
            if (opcion == 2) { consultarCatalogo(entidad, numero("ID: ")); continue; }
            if (opcion < 3 || opcion > 5) { System.out.println("Opcion invalida."); continue; }
            // Los servicios vuelven a comprobar los permisos.
            Acceso.administrador(sesion, false);
            int id = opcion == 3 ? 0 : numero("ID: ");
            if (opcion == 5) {
                if (entidad == 1) artistas.baja(id);
                if (entidad == 2) albumes.baja(id);
                if (entidad == 3) canciones.baja(id);
            } else if (entidad == 1) {
                Artista a = id == 0 ? new Artista("", "", true) : artistas.buscarPorId(id);
                a.setNombre(texto("Nombre: ")); a.setPaisOrigen(texto("Pais: "));
                a.setGeneros(new java.util.HashSet<Genero>());
                a.agregarGenero(genero());
                if (opcion == 3) artistas.alta(a); else artistas.modificar(a);
                id = a.getIdArtista();
            } else if (entidad == 2) {
                Album a = id == 0 ? new Album("", 2000, "", null, true) : albumes.buscarPorId(id);
                a.setTitulo(texto("Titulo: ")); a.setAnioLanzamiento(numero("Anio: "));
                a.setPortada(texto("Ruta de portada (opcional): "));
                a.setArtista(artistas.buscarPorId(numero("ID artista: ")));
                if (opcion == 3) albumes.alta(a); else albumes.modificar(a);
                id = a.getIdAlbum();
            } else {
                Cancion c = id == 0 ? new Cancion("", 1, "", Genero.ROCK, null, null, true) : canciones.buscarPorId(id);
                c.setTitulo(texto("Titulo: ")); c.setDuracionSegundos(numero("Duracion en segundos: "));
                c.setRutaArchivo(texto("Ruta de archivo (opcional): ")); c.setGenero(genero());
                c.setArtista(artistas.buscarPorId(numero("ID artista: ")));
                int idAlbum = numero("ID album (0 para single): ");
                c.setAlbum(idAlbum == 0 ? null : albumes.buscarPorId(idAlbum));
                if (opcion == 3) canciones.alta(c); else canciones.modificar(c);
                id = c.getIdCancion();
            }
            System.out.println("Operacion realizada. ID: " + id);
            registrarAccion("Catalogo " + entidad + ": opcion " + opcion + ", ID " + id);
        }
    }

    private void listarCatalogo(int entidad) throws ConexionBDException, ElementoDuplicadoException, ElementoNoEncontradoException, PermisoDenegadoException {
        if (entidad == 1) for (Artista a : artistas.listar()) System.out.println(a.getIdArtista() + " - " + a.getNombre() + " / " + a.getPaisOrigen() + " / " + a.getGeneros());
        if (entidad == 2) for (Album a : albumes.listar()) System.out.println(a.getIdAlbum() + " - " + a.getTitulo() + " / " + a.getArtista().getNombre() + " / " + a.getDuracionTotal() + " segundos");
        if (entidad == 3) for (Cancion c : canciones.listar()) mostrarCancion(c);
    }

    private void consultarCatalogo(int entidad, int id) throws ConexionBDException, ElementoDuplicadoException, ElementoNoEncontradoException, PermisoDenegadoException {
        if (entidad == 1) {
            Artista a = artistas.buscarPorId(id);
            System.out.println(a + " / " + a.getPaisOrigen() + " / " + a.getGeneros());
            for (Album album : albumes.listarPorArtista(id)) System.out.println("Album: " + album.getIdAlbum() + " - " + album.getTitulo());
        }
        if (entidad == 2) {
            Album a = albumes.buscarPorId(id);
            System.out.println(a.getTitulo() + " / " + a.getAnioLanzamiento() + " / " + a.getDuracionTotal() + " segundos");
            for (Cancion c : a.getCanciones()) mostrarCancion(c);
        }
        if (entidad == 3) mostrarCancion(canciones.buscarPorId(id));
    }

    private void menuPlaylists() throws ConexionBDException, ElementoDuplicadoException, ElementoNoEncontradoException, PermisoDenegadoException {
        while (true) {
            System.out.println("\n1. Listar visibles | 2. Consultar canciones | 0. Volver");
            if (sesion.getUsuarioActual() instanceof UsuarioNormal)
                System.out.println("3. Crear | 4. Modificar | 5. Dar de baja | 6. Agregar cancion | 7. Quitar cancion | 8. Mover cancion");
            int opcion = numero("Opcion: ");
            if (opcion == 0) return;
            if (opcion == 1) {
                for (Playlist p : playlists.listar()) System.out.println(p.getIdPlaylist() + " - " + p.getNombre() + " / propietario " + p.getIdPropietario() + (p.isPublica() ? " / publica" : " / privada"));
                continue;
            }
            if (opcion < 2 || opcion > 8) { System.out.println("Opcion invalida."); continue; }
            int id = opcion == 3 ? 0 : numero("ID playlist: ");
            if (opcion == 2) {
                Playlist p = playlists.buscarPorId(id);
                System.out.println(p.getNombre() + " - " + p.getDescripcion() + " / " + p.getDuracionTotal() + " segundos");
                int posicion = 1;
                for (Cancion c : p.getCanciones()) { System.out.print(posicion++ + ". "); mostrarCancion(c); }
                continue;
            }
            if (opcion == 3 || opcion == 4) {
                Playlist p = opcion == 3 ? new Playlist("", "", false, sesion.getUsuarioActual().getIdUsuario(), true) : playlists.buscarPorId(id);
                p.setNombre(texto("Nombre: ")); p.setDescripcion(texto("Descripcion: "));
                p.setPublica(siNo("Publica (s/n): "));
                if (opcion == 3) playlists.alta(p); else playlists.modificar(p);
                id = p.getIdPlaylist();
            }
            if (opcion == 5) playlists.baja(id);
            if (opcion == 6) playlists.agregarCancion(id, numero("ID cancion: "));
            if (opcion == 7) playlists.quitarCancion(id, numero("ID cancion: "));
            if (opcion == 8) playlists.moverCancion(id, numero("Posicion actual (desde 1): ") - 1, numero("Posicion destino (desde 1): ") - 1);
            System.out.println("Operacion realizada. Playlist ID: " + id);
        }
    }

    private void menuFavoritos() throws ConexionBDException, ElementoDuplicadoException, ElementoNoEncontradoException, PermisoDenegadoException {
        while (true) {
            int id = sesion.getUsuarioActual().getIdUsuario();
            System.out.println("\n1. Listar favoritos | 2. Agregar | 3. Quitar | 0. Volver");
            int opcion = numero("Opcion: ");
            if (opcion == 0) return;
            if (opcion == 1) {
                UsuarioNormal u = (UsuarioNormal) usuarios.buscarPorId(id);
                for (int idCancion : u.getFavoritos()) mostrarCancion(canciones.buscarPorId(idCancion));
            } else if (opcion == 2 || opcion == 3) {
                int cancion = numero("ID cancion: ");
                boolean cambio = opcion == 2 ? usuarios.agregarFavorito(id, cancion) : usuarios.quitarFavorito(id, cancion);
                System.out.println(cambio ? "Favoritos actualizados." : "Sin cambios: ya estaba agregado o no existia.");
            } else System.out.println("Opcion invalida.");
        }
    }

    private void menuUsuarios() throws ConexionBDException, ElementoDuplicadoException, ElementoNoEncontradoException, PermisoDenegadoException {
        if (!sesion.esAdministrador()) throw new PermisoDenegadoException("Requiere administrador.");
        while (true) {
            System.out.println("\n1. Listar | 2. Consultar | 0. Volver");
            if (((UsuarioAdministrador) sesion.getUsuarioActual()).getNivelAdmin() == NivelAdmin.SUPERADMIN)
                System.out.println("3. Crear | 4. Modificar | 5. Dar de baja | 6. Cambiar nivel admin");
            int opcion = numero("Opcion: ");
            if (opcion == 0) return;
            if (opcion == 1) { for (Usuario u : usuarios.listar()) System.out.println(u); continue; }
            if (opcion == 2) { System.out.println(usuarios.buscarPorId(numero("ID: "))); continue; }
            if (opcion < 3 || opcion > 6) { System.out.println("Opcion invalida."); continue; }
            Acceso.administrador(sesion, true);
            int id = opcion == 3 ? 0 : numero("ID usuario: ");
            if (opcion == 3) {
                String nombre = texto("Nombre: "), email = texto("Email: "), clave = texto("Contrasenia: ");
                Acceso.texto(clave, 100, "Contrasenia");
                String salt = Seguridad.generarSalt(), hash = Seguridad.hashear(clave, salt);
                Usuario u;
                if (siNo("Administrador (s/n): ")) u = new UsuarioAdministrador(nombre, email, hash, salt, nivel());
                else u = new UsuarioNormal(nombre, email, hash, salt, siNo("Premium (s/n): "));
                usuarios.alta(u); id = u.getIdUsuario();
            }
            if (opcion == 4) {
                Usuario u = usuarios.buscarPorId(id);
                u.setNombre(texto("Nombre: ")); u.setEmail(texto("Email: "));
                if (u instanceof UsuarioNormal) ((UsuarioNormal) u).setPremium(siNo("Premium (s/n): "));
                usuarios.modificar(u);
            }
            if (opcion == 5) usuarios.baja(id);
            if (opcion == 6) usuarios.cambiarNivelAdmin(id, nivel());
            System.out.println("Operacion realizada. Usuario ID: " + id);
            registrarAccion("Usuarios: opcion " + opcion + ", ID " + id);
        }
    }

    private void registrarAccion(String accion) {
        try { usuarios.registrarAccion(sesion.getUsuarioActual().getIdUsuario(), accion); }
        catch (ConexionBDException | ElementoNoEncontradoException | PermisoDenegadoException | IllegalStateException e) {
            System.out.println("La operacion se realizo, pero no se pudo registrar en el historial: " + e.getMessage());
        }
    }

    private void mostrarCancion(Cancion c) {
        System.out.println(c.getIdCancion() + " - " + c.getTitulo() + " / " + c.getArtista().getNombre() + " / " + c.getGenero() + " / " + c.getDuracionFormateada());
    }

    private Genero genero() {
        Genero[] valores = Genero.values();
        for (int i = 0; i < valores.length; i++) System.out.println((i + 1) + ". " + valores[i]);
        int n = numero("Genero: ");
        if (n < 1 || n > valores.length) throw new IllegalArgumentException("Genero invalido.");
        return valores[n - 1];
    }

    private NivelAdmin nivel() {
        System.out.println("1. SUPERADMIN | 2. EDITOR | 3. VISUALIZANTE");
        int n = numero("Nivel: ");
        if (n < 1 || n > 3) throw new IllegalArgumentException("Nivel invalido.");
        return NivelAdmin.values()[n - 1];
    }

    private String texto(String mensaje) {
        System.out.print(mensaje);
        return entrada.nextLine().trim();
    }

    private int numero(String mensaje) {
        while (true) {
            try { return Integer.parseInt(texto(mensaje)); }
            catch (NumberFormatException e) { System.out.println("Ingrese un numero entero valido."); }
        }
    }

    private boolean siNo(String mensaje) {
        while (true) {
            String valor = texto(mensaje);
            if (valor.equalsIgnoreCase("s")) return true;
            if (valor.equalsIgnoreCase("n")) return false;
            System.out.println("Ingrese s o n.");
        }
    }
}
