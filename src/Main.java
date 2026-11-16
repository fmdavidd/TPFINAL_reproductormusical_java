import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;

import Enumeraciones.Genero;
import Enumeraciones.NivelAdmin;
import Excepciones.ConexionBDException;
import Excepciones.PermisoDenegadoException;
import Seguridad.Seguridad;
import Service.AlbumService;
import Service.ArtistaService;
import Service.CancionService;
import Service.PlaylistService;
import Service.SesionService;
import Service.UsuarioService;
import modelo.Album;
import modelo.Artista;
import modelo.Cancion;
import modelo.Playlist;
import modelo.Usuario;
import modelo.UsuarioAdministrador;
import modelo.UsuarioNormal;

public class Main {
    private static final Scanner teclado = new Scanner(System.in);

    private static UsuarioService usuarios;
    private static ArtistaService artistas;
    private static CancionService canciones;
    private static AlbumService albumes;
    private static PlaylistService playlists;
    private static SesionService sesion;

    public static void main(String[] args) {
        try {
            usuarios = new UsuarioService();
            sesion = new SesionService(usuarios);
            artistas = new ArtistaService();
            canciones = new CancionService();
            albumes = new AlbumService(canciones);
            playlists = new PlaylistService(canciones);
        } catch (ConexionBDException e) {
            System.out.println("No se pudo conectar a MySQL: " + e.getMessage());
            return;
        }

        boolean salir = false;

        while (!salir) {
            try {
                Usuario usuarioActual = sesion.getUsuarioActual();
                int opcion;

                if (usuarioActual == null) {
                    System.out.println("\nREPRODUCTOR MUSICAL");
                    System.out.println("1. Iniciar sesion");
                    System.out.println("2. Registrarse");
                    System.out.println("0. Salir");

                    opcion = numero("Opcion");

                    switch (opcion) {
                        case 1: {
                            String email = texto("Email");
                            String contrasenia = texto("Contrasenia");
                            sesion.iniciarSesion(email, contrasenia);
                            break;
                        }
                        case 2: {
                            String nombre = texto("Nombre");
                            String email = texto("Email");
                            String contrasenia = texto("Contrasenia");

                            sesion.registrar(nombre, email, contrasenia);
                            System.out.println("Usuario registrado. Ya podes iniciar sesion.");
                            break;
                        }
                        case 0: {
                            salir = true;
                            break;
                        }
                        default: {
                            System.out.println("Opcion invalida.");
                            break;
                        }
                    }
                } else {
                    System.out.println("\nHola, " + usuarioActual.getNombre());
                    System.out.println("1. Canciones");
                    System.out.println("2. Artistas");
                    System.out.println("3. Albumes");
                    System.out.println("4. Playlists");
                    System.out.println("5. Usuarios (admin)");
                    System.out.println("6. Favoritos");
                    System.out.println("0. Cerrar sesion");

                    opcion = numero("Opcion");

                    switch (opcion) {
                        case 1: {
                            gestionar(1);
                            break;
                        }
                        case 2: {
                            gestionar(2);
                            break;
                        }
                        case 3: {
                            gestionar(3);
                            break;
                        }
                        case 4: {
                            gestionar(4);
                            break;
                        }
                        case 5: {
                            exigirAdmin();
                            gestionar(5);
                            break;
                        }
                        case 6: {
                            favoritos();
                            break;
                        }
                        case 0: {
                            sesion.cerrarSesion();
                            break;
                        }
                        default: {
                            System.out.println("Opcion invalida.");
                            break;
                        }
                    }
                }
            } catch (NoSuchElementException e) {
                salir = true;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        }
    }

    // tipo: 1 canciones, 2 artistas, 3 albumes, 4 playlists, 5 usuarios.
    private static void gestionar(int tipo) throws Exception {
        System.out.println("\n1. Listar");
        System.out.println("2. Consultar por ID");
        System.out.println("3. Alta");
        System.out.println("4. Modificar");
        System.out.println("5. Baja");
        System.out.println("8. Ver dados de baja (admin)");
        System.out.println("9. Reactivar (admin)");
        System.out.println("0. Volver");

        if (tipo == 4) {
            System.out.println("6. Agregar cancion");
            System.out.println("7. Quitar cancion");
        }

        int opcion = numero("Opcion");

        if (opcion == 0) {
            return;
        }

        if (opcion < 1 || opcion > 9) {
            System.out.println("Opcion invalida.");
            return;
        }

        // Como las playlists son las únicas que tienen opciones mayores a 5,
        // si el tipo no es una playlist una opción mayor a 5 no puede ser
        // válida
        if (tipo != 4 && (opcion == 6 || opcion == 7)) {
            System.out.println("Opcion invalida.");
            return;
        }

        // Los cambios sobre canciones, artistas, albumes y usuarios son de administrador.
        if (opcion >= 3 && tipo != 4) {
            exigirAdmin();
        }

        // Ver dados de baja y reactivar son siempre de administrador (tambien en playlists).
        if (opcion == 8 || opcion == 9) {
            exigirAdmin();
        }

        int id = 0;

        // Para consultar, modificar o dar de baja se pide el ID.
        if (opcion != 1 && opcion != 3 && opcion != 8) {
            id = numero("ID");

            if (id <= 0) {
                throw new IllegalArgumentException("El ID debe ser positivo.");
            }
        }

        // Una playlist privada solo puede consultarla su duenio o un administrador.
        // Para modificarla, tambien se necesita permiso aunque sea publica.
        if (tipo == 4 && id != 0 && opcion != 9) {
            Playlist playlist = playlists.buscarPorId(id);
            Usuario usuarioActual = sesion.getUsuarioActual();

            // El usuario tiene permiso para modificar si es administrador o si es el propietario de la playlist.
            boolean tienePermiso = sesion.esAdministrador();

            if (playlist.getIdPropietario() == usuarioActual.getIdUsuario()) {
                tienePermiso = true;
            }

            if (opcion == 2 && playlist.isPublica()) {
                tienePermiso = true;
            }

            if (!tienePermiso) {
                throw new PermisoDenegadoException("No tenes permiso sobre esta playlist.");
            }
        }

        switch (opcion) {
            case 1: {
                listar(tipo);
                break;
            }
            case 2: {
                consultar(tipo, id);
                break;
            }
            case 3: {
                guardar(tipo, 0);
                break;
            }
            case 4: {
                consultar(tipo, id);
                guardar(tipo, id);
                break;
            }
            case 5: {
                if (tipo == 5 && id == sesion.getUsuarioActual().getIdUsuario()) {
                    throw new IllegalArgumentException("No podes dar de baja tu propia sesion.");
                }

                switch (tipo) {
                    case 1: {
                        canciones.baja(id);
                        break;
                    }
                    case 2: {
                        artistas.baja(id);
                        break;
                    }
                    case 3: {
                        albumes.baja(id);
                        break;
                    }
                    case 4: {
                        playlists.baja(id);
                        break;
                    }
                    case 5: {
                        usuarios.baja(id);
                        break;
                    }
                }

                System.out.println("Baja realizada.");
                break;
            }
            case 8: {
                listarInactivos(tipo);
                break;
            }
            case 9: {
                switch (tipo) {
                    case 1: {
                        canciones.reactivar(id);
                        break;
                    }
                    case 2: {
                        artistas.reactivar(id);
                        break;
                    }
                    case 3: {
                        albumes.reactivar(id);
                        break;
                    }
                    case 4: {
                        playlists.reactivar(id);
                        break;
                    }
                    case 5: {
                        usuarios.reactivar(id);
                        break;
                    }
                }

                System.out.println("Reactivado.");
                break;
            }
            case 6: {
                int idCancion = numero("ID de cancion");
                playlists.agregarCancion(id, idCancion);
                System.out.println("Cancion agregada.");
                break;
            }
            case 7: {
                int idCancion = numero("ID de cancion");
                playlists.quitarCancion(id, idCancion);
                System.out.println("Cancion quitada.");
                break;
            }
        }
    }

    private static void listarInactivos(int tipo) throws Exception {
        switch (tipo) {
            case 1: {
                for (Cancion cancion : canciones.listarInactivos()) {
                    System.out.println(cancion.getIdCancion() + " - " + cancion.getTitulo());
                }
                break;
            }
            case 2: {
                for (Artista artista : artistas.listarInactivos()) {
                    System.out.println(artista.getIdArtista() + " - " + artista.getNombre());
                }
                break;
            }
            case 3: {
                for (Album album : albumes.listarInactivos()) {
                    System.out.println(album.getIdAlbum() + " - " + album.getTitulo());
                }
                break;
            }
            case 4: {
                for (Playlist playlist : playlists.listarInactivos()) {
                    System.out.println(playlist.getIdPlaylist() + " - " + playlist.getNombre());
                }
                break;
            }
            case 5: {
                for (Usuario usuario : usuarios.listarInactivos()) {
                    System.out.println(usuario);
                }
                break;
            }
        }
    }

    private static void listar(int tipo) throws Exception {
        switch (tipo) {
            case 1: {
                for (Cancion cancion : canciones.listar()) {
                    System.out.println(cancion.getIdCancion() + " - " + cancion.getTitulo()
                            + " - " + cancion.getDuracionFormateada());
                }
                break;
            }
            case 2: {
                for (Artista artista : artistas.listar()) {
                    System.out.println(artista.getIdArtista() + " - " + artista.getNombre());
                }
                break;
            }
            case 3: {
                for (Album album : albumes.listar()) {
                    System.out.println(album.getIdAlbum() + " - " + album.getTitulo());
                }
                break;
            }
            case 4: {
                for (Playlist playlist : playlists.listar()) {
                    boolean puedeVer = sesion.esAdministrador();

                    if (playlist.isPublica()) {
                        puedeVer = true;
                    }

                    if (playlist.getIdPropietario() == sesion.getUsuarioActual().getIdUsuario()) {
                        puedeVer = true;
                    }

                    if (puedeVer) {
                        String visibilidad = "Privada";

                        if (playlist.isPublica()) {
                            visibilidad = "Publica";
                        }

                        System.out.println(playlist.getIdPlaylist() + " - " + playlist.getNombre()
                                + " - " + visibilidad);
                    }
                }
                break;
            }
            case 5: {
                for (Usuario usuario : usuarios.listar()) {
                    System.out.println(usuario);
                }
                break;
            }
        }
    }

    private static void consultar(int tipo, int id) throws Exception {
        switch (tipo) {
            case 1: {
                Cancion cancion = canciones.buscarPorId(id);

                System.out.println(cancion + " - " + cancion.getArtista().getNombre()
                        + " - " + cancion.getGenero() + " - " + cancion.getDuracionFormateada());
                break;
            }
            case 2: {
                Artista artista = artistas.buscarPorId(id);
                System.out.println(artista + " - " + artista.getPaisOrigen() + " - " + artista.getGeneros());
                break;
            }
            case 3: {
                Album album = albumes.buscarPorId(id);
                System.out.println(album.getTitulo() + " - " + album.getAnioLanzamiento()
                        + " - " + album.getArtista().getNombre());

                for (Cancion cancion : albumes.listarCanciones(id)) {
                    System.out.println(cancion);
                }
                break;
            }
            case 4: {
                Playlist playlist = playlists.buscarPorId(id);
                System.out.println(playlist.getNombre() + " - " + playlist.getDescripcion()
                        + " - " + playlist.getDuracionTotal() + " segundos");

                for (Cancion cancion : playlist.getCanciones()) {
                    System.out.println(cancion);
                }
                break;
            }
            case 5: {
                Usuario usuario = usuarios.buscarPorId(id);
                System.out.println(usuario);
                break;
            }
        }
    }

    private static void guardar(int tipo, int id) throws Exception {
        switch (tipo) {
            case 1: {
                String titulo = texto("Titulo");
                int duracion = numero("Duracion en segundos");

                if (duracion <= 0) {
                    throw new IllegalArgumentException("La duracion debe ser positiva.");
                }

                String ruta = texto("Ruta de archivo");
                System.out.println(Arrays.toString(Genero.values()));
                Genero genero = Genero.valueOf(texto("Genero").toUpperCase());

                int idArtista = numero("ID de artista");
                Artista artista = artistas.buscarPorId(idArtista);

                int idAlbum = numero("ID de album (0 para single)");
                Album album = null;

                if (idAlbum != 0) {
                    album = albumes.buscarPorId(idAlbum);
                }

                if (album != null) {
                    if (album.getArtista().getIdArtista() != artista.getIdArtista()) {
                        throw new IllegalArgumentException("El album pertenece a otro artista.");
                    }
                }

                Cancion cancion = new Cancion(titulo, duracion, ruta, genero, artista, album, true);
                cancion.setIdCancion(id);

                if (id == 0) {
                    canciones.alta(cancion);
                } else {
                    canciones.modificar(cancion);
                }
                break;
            }
            case 2: {
                String nombre = texto("Nombre");
                String pais = texto("Pais");
                Artista artista = new Artista(nombre, pais, true);

                System.out.println(Arrays.toString(Genero.values()));
                String generosIngresados = texto("Generos separados por coma");
                String[] generos = generosIngresados.split(",");

                for (String nombreGenero : generos) {
                    Genero genero = Genero.valueOf(nombreGenero.trim().toUpperCase());
                    artista.agregarGenero(genero);
                }

                artista.setIdArtista(id);

                if (id == 0) {
                    artistas.alta(artista);
                } else {
                    artistas.modificar(artista);
                }
                break;
            }
            case 3: {
                String titulo = texto("Titulo");
                int anio = numero("Anio");
                String rutaPortada = texto("Ruta de portada");
                int idArtista = numero("ID de artista");

                Artista artista = artistas.buscarPorId(idArtista);
                Album album = new Album(titulo, anio, rutaPortada, artista, true);
                album.setIdAlbum(id);

                if (id == 0) {
                    albumes.alta(album);
                } else {
                    albumes.modificar(album);
                }
                break;
            }
            case 4: {
                int idPropietario;

                if (id == 0) {
                    idPropietario = sesion.getUsuarioActual().getIdUsuario();
                } else {
                    Playlist playlistAnterior = playlists.buscarPorId(id);
                    idPropietario = playlistAnterior.getIdPropietario();
                }

                String nombre = texto("Nombre");
                String descripcion = texto("Descripcion");
                int opcionPublica = numero("Publica: 1 si, 0 no");
                boolean esPublica = opcionPublica == 1;

                Playlist playlist = new Playlist(nombre, descripcion, esPublica, idPropietario, true);
                playlist.setIdPlaylist(id);

                if (id == 0) {
                    playlists.alta(playlist);
                } else {
                    playlists.modificar(playlist);
                }
                break;
            }
            case 5: {
                guardarUsuario(id);
                break;
            }
        }

        System.out.println("Datos guardados.");
    }

    private static void guardarUsuario(int id) throws Exception {
        Usuario anterior = null;

        if (id != 0) {
            anterior = usuarios.buscarPorId(id);
        }

        String nombre = texto("Nombre");
        String email = texto("Email");
        String contrasenia = texto("Contrasenia nueva");

        String salt = Seguridad.generarSalt();
        String hash = Seguridad.hashear(contrasenia, salt);

        // Si es una modificacion, el usuario mantiene su tipo original.
        boolean esAdministrador;

        if (anterior == null) {
            int tipoUsuario = numero("Tipo: 1 administrador, 2 normal");
            esAdministrador = tipoUsuario == 1;
        } else {
            esAdministrador = anterior instanceof UsuarioAdministrador;
        }

        Usuario nuevo;

        if (esAdministrador) {
            System.out.println(Arrays.toString(NivelAdmin.values()));
            String nombreNivel = texto("Nivel").toUpperCase();
            NivelAdmin nivel = NivelAdmin.valueOf(nombreNivel);

            UsuarioAdministrador administrador = new UsuarioAdministrador(nombre, email, hash, salt, nivel);

            if (anterior != null) {
                UsuarioAdministrador administradorAnterior = (UsuarioAdministrador) anterior;
                administrador.setRegistroAcciones(administradorAnterior.getRegistroAcciones());
            }

            nuevo = administrador;
        } else {
            int opcionPremium = numero("Premium: 1 si, 0 no");
            boolean premium = opcionPremium == 1;

            UsuarioNormal usuarioNormal = new UsuarioNormal(nombre, email, hash, salt, premium);

            if (anterior != null) {
                UsuarioNormal usuarioAnterior = (UsuarioNormal) anterior;
                usuarioNormal.setFavoritos(usuarioAnterior.getFavoritos());
                usuarioNormal.setPlaylists(usuarioAnterior.getPlaylists());
            }

            nuevo = usuarioNormal;
        }

        nuevo.setIdUsuario(id);

        if (id == 0) {
            usuarios.alta(nuevo);
        } else {
            nuevo.setFechaRegistro(anterior.getFechaRegistro());
            usuarios.modificar(nuevo);

            if (id == sesion.getUsuarioActual().getIdUsuario()) {
                sesion.iniciarSesion(email, contrasenia);
            }
        }
    }

    private static void favoritos() throws Exception {
        Usuario usuario = sesion.getUsuarioActual();

        if (!(usuario instanceof UsuarioNormal)) {
            System.out.println("Los favoritos son para usuarios normales.");
            return;
        }

        UsuarioNormal usuarioNormal = (UsuarioNormal) usuario;

        System.out.println("\n1. Listar favoritos");
        System.out.println("2. Agregar");
        System.out.println("3. Quitar");
        System.out.println("0. Volver");

        int opcion = numero("Opcion");

        switch (opcion) {
            case 1: {
                for (Cancion cancion : canciones.listar()) {
                    if (usuarioNormal.esFavorita(cancion.getIdCancion())) {
                        System.out.println(cancion);
                    }
                }
                break;
            }
            case 2: {
                int idCancion = numero("ID de cancion");
                canciones.buscarPorId(idCancion);

                boolean agregado = usuarios.agregarFavorito(usuario.getIdUsuario(), idCancion);

                if (agregado) {
                    System.out.println("Agregado.");
                } else {
                    System.out.println("Ya era favorita.");
                }
                break;
            }
            case 3: {
                int idCancion = numero("ID de cancion");
                boolean quitado = usuarios.quitarFavorito(usuario.getIdUsuario(), idCancion);

                if (quitado) {
                    System.out.println("Quitado.");
                } else {
                    System.out.println("No era favorita.");
                }
                break;
            }
            case 0: {
                break;
            }
            default: {
                System.out.println("Opcion invalida.");
                break;
            }
        }
    }

    private static void exigirAdmin() throws PermisoDenegadoException {
        if (!sesion.esAdministrador()) {
            throw new PermisoDenegadoException("Solo para administradores.");
        }
    }

    private static String texto(String mensaje) {
        System.out.print(mensaje + ": ");
        String valor = teclado.nextLine().trim();

        if (valor.isEmpty()) {
            throw new IllegalArgumentException("El campo no puede estar vacio.");
        }


        return valor;
    }

    private static int numero(String mensaje) {
        String valor = texto(mensaje);
        return Integer.parseInt(valor);
    }
}
