# Verificacion y guion de defensa

## Cambios realizados

| Archivos | Cambio |
| --- | --- |
| `src/ConsultarBaseDatos.java` | Ejemplo de lectura JDBC de canciones ya cargadas, sin inicializacion automatica. |
| `src/Main.java`, `src/Consola.java` | Inicio interactivo, menus por rol, CRUD de cinco entidades, favoritos y playlists. |
| `src/Service/Acceso.java` | Comprobaciones compartidas de sesion, nivel y propietario; validacion de texto y resultados JDBC. |
| `src/Service/*Service.java` | Permisos en servicios, validaciones, lectura actual sin caches, politica de bajas y cuentas inactivas. |
| `src/DAO/AlbumDAO.java`, `CancionDAO.java` | Hidratacion explicita de albumes sin recursion y filtrado de padres inactivos. |
| `src/DAO/ArtistaDAO.java` | No modificar generos si falla la modificacion principal. |
| `src/DAO/PlaylistDAO.java` | Carga ordenada, MAX al agregar, normalizacion y UPDATE CASE para mover. |
| `src/DAO/UsuarioDAO.java` | Buscar emails incluidos inactivos, favoritos activos, playlists del usuario e historial sin sobrescrituras. |
| `src/modelo/Album.java`, `Artista.java`, `Playlist.java`, `UsuarioNormal.java` | Colecciones inicializadas y copias defensivas. |
| `src/modelo/Usuario.java`, `src/Interfaces/Reproducible.java` | Comentarios ajustados a las responsabilidades implementadas. |
| `src/Conexion/ConexionBD.java` | Variables de entorno y propiedades Java. |
| `lib/sql.txt`, `sql/01_esquema.sql`, `sql/02_datos.sql` | Inicializacion manual sin DROP, al menos 15 filas por entidad y hashes compatibles. |
| `tests/PruebasLocales.java`, `tests/verificar_datos.py` | Comprobaciones pequenas sin frameworks. |
| `README.md`, `VERIFICACION.md` | Configuracion, politicas, limites y demostracion. |

Se conservaron paquetes, jerarquia, interfaces, enums, excepciones y driver. No se
modifico el proyecto ajeno de la carpeta superior. No se creo UML ni se implementaron
transacciones, frameworks, infraestructura ni un motor de audio.

## Evidencia obtenida

- Compilacion completa de `src` con OpenJDK 21 y el JAR incluido: correcta.
- 23 comprobaciones Java sin base: colecciones no nulas, copias defensivas, duracion,
  orden del modelo y posiciones invalidas, historial de 1000 acciones, login correcto,
  fallo de login que cierra la sesion anterior, baja que revoca sesion, cuenta inactiva,
  rechazo de usuario normal/visualizante, distincion EDITOR/SUPERADMIN, propietario
  incorrecto y servicio sin sesion.
- Datos iniciales comprobados con SQLite en memoria: 15 usuarios, 15 artistas,
  15 albumes, 16 canciones y 15 playlists; claves foraneas, propietarios normales,
  coincidencia artista/album/cancion, posiciones contiguas y los 15 hashes correctos.
  Esto no equivale a haber ejecutado los scripts en MySQL.
- Consola real sin base: entrada no numerica, opcion inexistente, fallo de conexion
  al iniciar sesion, registro con nombre vacio, recuperacion y salida.
- No se ejecutaron scripts contra una base existente. Se confirmo posteriormente
  que MySQL80 esta iniciado y Workbench conecta mediante `conn` a 127.0.0.1:3306.
  La prueba inicial estaba bloqueada por el aislamiento de red. Las credenciales
  originales fueron rechazadas; Workbench uso su conexion guardada. Con aprobacion
  explicita del usuario se ejecuto el script combinado: 31 sentencias correctas,
  tablas y datos creados en ReproductorMusicalBD. Las otras bases se conservaron.
- En MySQL se confirmaron 15 usuarios, 15 artistas, 15 albumes, 16 canciones y
  15 playlists. Las consultas de artista/album/cancion, posiciones duplicadas
  y posiciones discontinuas devolvieron cero filas.
- Falta disponer de credenciales validas para Java. No se comprobo aun el CRUD
  JDBC ni la persistencia al reiniciar la aplicacion.

## Prueba pendiente contra MySQL

Usar una base nueva y los pasos del README. Las operaciones siguientes modifican
solo los datos de demostracion; comprobar el resultado despues de cada paso.

1. Juan: login, listar catalogo y album 1; comprobar 260 segundos. Consultar playlist 2
   privada propia y playlist 1 publica. Crear una playlist, agregar canciones 1, 2 y 3,
   mover la tercera a la primera posicion. Salir y reiniciar: el orden debe ser 3, 1, 2.
   Quitar la cancion 1, agregar la 4: debe quedar 3, 2, 4 sin posiciones repetidas.
2. Juan: agregar favorito 1 dos veces, quitarlo y listar. Modificar nombre, descripcion
   y privacidad de su playlist. Sus playlists consultadas mediante UsuarioService y
   PlaylistService deben coincidir en una lectura nueva.
3. Maria: la playlist privada 2 de Juan debe rechazar el acceso. Intentar modificar
   playlist publica 1 debe rechazar la operacion. Una llamada a UsuarioService que
   cambie favoritos de Juan debe lanzar PermisoDenegadoException.
4. Tomas: consultar catalogo/usuarios; intentar crear artista o modificar usuario
   desde servicio debe rechazarlo. Laura: CRUD de catalogo permitido; CRUD de
   usuarios y cambios de nivel rechazados.
5. Admin: crear artista sin dependencias, modificarlo, crear album y cancion coherentes.
   Probar titulo vacio, duracion cero, ID inexistente, duplicados y album de otro artista.
   Las bajas de artista/album con contenido activo deben rechazarse. Dar de baja la
   cancion primero; luego album y artista. Comprobar exclusion en listados.
6. Juan: agregar cancion 2 a una playlist. Admin: dar de baja cancion 2. Juan: comprobar
   que no aparece en favoritos ni playlists y que puede agregar/reordenar otras canciones.
   Revisar `playlist_cancion` con los SELECT de posiciones incluidos en los datos.
7. Admin: crear usuario sin playlists, editar y darlo de baja. Registrarse con ese email
   debe dar el mensaje de email reservado. No debe llegar a una violacion UNIQUE.
   Cambiar nivel de otro administrador. Probar que no puede dar de baja su propia cuenta.
8. Realizar varias acciones administrativas rapidamente, reiniciar y comprobar que
   ninguna desaparece del historial. Login correcto seguido de otro fallido debe dejar
   la aplicacion sin sesion autenticada.
9. Desconectar MySQL durante una escritura; revisar la base al reconectar. No debe
   haber una cache local alterada. En secuencias de varias sentencias pueden existir
   cambios parciales; comprobar y completar la operacion segun el estado real.

## Lo que deben explicar los estudiantes

- Herencia de Usuario, roles/niveles y polimorfismo de Reproducible.
- Separacion consola -> servicios (reglas) -> DAOs (SQL) -> ConexionBD.
- PreparedStatement, claves generadas, try-with-resources y excepciones existentes.
- Colecciones inicializadas, copias defensivas e instantaneas consultadas de la base.
- Baja logica y por que se impiden bajas con dependencias activas.
- Verificacion de propietario en servicios, incluso si se invocan sin usar los menus.
- Salt + hash de contrasenas y por que un intento fallido cierra la sesion previa.
- Posiciones desde cero en base/servicio y desde uno en consola; UPDATE CASE y MAX.
- Por que las operaciones con varias sentencias no son atomicas al excluir transacciones.
- Limites comprobados: gestion de metadatos, una instancia y prueba MySQL pendiente.
