# Biblioteca musical · JavaFX

Aplicación de escritorio en español, construida con JavaFX programático. Mantiene los modelos, servicios, DAO, esquema MySQL y el directorio tradicional `src`. No usa FXML, Swing ni bibliotecas visuales externas.

## Abrir en IntelliJ IDEA

1. Abrir **esta carpeta** (`TPFINAL_reproductormusical_java_actualizado`) como proyecto, seleccionando `pom.xml` y cargándolo como Maven.
2. Configurar **JDK 21** en Project SDK y en Settings → Build Tools → Maven → Runner → JRE. Recargar el proyecto Maven para descargar las dependencias.
3. La conexión en `src/Conexion/ConexionBD.java` usa `localhost:3306/ReproductorMusicalBD`, con el usuario `user2` y la contraseña indicada por el propietario del proyecto. MySQL debe estar accesible y contener el esquema del proyecto. No ejecutar `lib/sql.txt` sobre una base con datos: comienza borrando y recreando la base.
4. En la ventana Maven, ejecutar `Plugins → javafx → javafx:run`. También se puede ejecutar el método `main` de **Main** como aplicación Java una vez que IntelliJ importe las dependencias Maven. No ejecutar directamente la clase `GUI.Aplicacion` con una configuración sin JavaFX.

No hace falta instalar manualmente un SDK JavaFX ni agregar opciones VM: Maven resuelve JavaFX y sus componentes nativos. La primera ejecución necesita Internet. El JAR de MySQL que ya estaba en `lib` se conserva; Maven usa Connector/J desde su repositorio, sin rutas absolutas a JAR locales.

## Ejecutar desde una terminal

Con Java 21, `JAVA_HOME` configurado y Maven en el PATH:

```powershell
mvn compile
mvn javafx:run
```

Si Maven no está en el PATH, se puede usar el `mvn.cmd` incluido en IntelliJ o ejecutar el objetivo desde su ventana Maven. `mvn package` compila y genera un JAR convencional; no genera un instalador ni un JAR autónomo con JavaFX. Para iniciar, usar `javafx:run`.

Configuración basada en la [documentación oficial de OpenJFX](https://openjfx.io/openjfx-docs/).

## Pantallas y uso

- **Acceso y registro:** correo, contraseña oculta y validación. El registro crea usuarios normales; cerrar sesión vuelve al acceso.
- **Canciones:** tabla con título, artista, álbum, género y duración; búsqueda, filtro de género y selección de varios artistas. Administradores agregan, editan y dan de baja; usuarios normales pueden agregar favoritos.
- **Artistas:** tabla con nombre, país y géneros; búsqueda por nombre y filtro por uno o varios géneros. Formulario con selección múltiple de géneros.
- **Álbumes:** búsqueda por título, selección de varios artistas y detalle de canciones. Relaciones seleccionadas por nombre.
- **Playlists:** lista accesible según privacidad, búsqueda y filtro por propietario. Creación, edición y baja; detalle con canciones en orden, agregado y eliminación de canciones. Solo el dueño o un administrador puede modificar una playlist, incluso si es pública. Una nueva playlist pertenece al usuario que la crea.
- **Favoritos:** disponible a usuarios normales; muestra canciones activas, permite agregar y quitar favoritos propios.
- **Usuarios:** exclusivo de administradores; alta, edición y baja lógica, contraseña opcional al editar, premium y nivel administrativo. El tipo normal/administrador se elige al crear y se conserva al editar, porque el DAO existente no cambia el rol. No se permite dar de baja la cuenta de la sesión actual.

Las tablas permiten ordenar columnas, filtrar mientras se escribe y actualizar desde MySQL. Los diálogos conservan los valores ante errores y no modifican objetos existentes al cancelar. Las bajas se confirman y son lógicas; quitar canciones de playlists o favoritos elimina solo la relación. Las operaciones JDBC se ejecutan en segundo plano con los controles temporalmente deshabilitados.

Los filtros múltiples se abren como un menú de casillas que permanece abierto al marcar opciones. Seleccionar varios artistas muestra los resultados de cualquiera de ellos; seleccionar varios géneros en Artistas incluye a quienes tengan al menos uno de esos géneros, sin duplicarlos. La búsqueda por texto y los demás filtros se aplican al mismo tiempo. Sin casillas marcadas se muestran todos; **Limpiar filtros** restablece todas las selecciones y la búsqueda.

## Cambios pequeños en los servicios y JDBC

- Comprobaciones de sesión/administrador dentro de las operaciones administrativas; propiedad y visibilidad dentro de `PlaylistService`; favoritos limitados al usuario autenticado. No se depende solamente de ocultar botones.
- Métodos `recargar()` para consultar nuevamente los DAO, porque los servicios originales solo cargaban sus colecciones en el constructor.
- La sesión consulta el usuario vigente en la colección, incluso después de editarlo o actualizarlo. Se rechazan cuentas inactivas.
- `PermisoDenegadoException` pasa a ser una excepción de ejecución para compartir los guardas de permisos entre los servicios sin alterar todas sus firmas. Los rechazos se muestran como errores recuperables en la interfaz.
- Tiempos máximos JDBC de conexión (5 segundos) y lectura (10 segundos). La conexión se actualizó a localhost con las credenciales solicitadas durante la implementación.
- Al agregar a una playlist se calcula el final desde las posiciones persistidas, incluyendo vínculos con canciones inactivas. La eliminación del vínculo y el ajuste de posiciones se ejecutan en una misma transacción para evitar cambios parciales.

Se mantiene la regla existente: todos los administradores pueden administrar. No se implementa una jerarquía diferente para SUPERADMIN, EDITOR o VISUALIZANTE. No se modificó el esquema ni el UML.

## Verificación

Se compiló con JDK 21 y Maven. Se abrió la ventana JavaFX y se inspeccionaron visualmente acceso, validación de campos vacíos y registro.

La prueba `test/PruebaGUI.java` usa un doble JDBC aislado que no se conecta a la red ni toca MySQL. Comprueba autenticación válida/inválida, rechazos antes de escribir en el DAO, navegación y tablas de todas las pantallas para ambos roles, validación de correo/números, privacidad de playlists, agregado/quita de canciones y favoritos, duplicados y cierre de sesión. **No demuestra persistencia SQL.** No agrega dependencias de pruebas.

En Windows, con Maven y JDK en PATH:

```powershell
.\scripts\verificar.ps1
```

Si se usa el Maven incluido en IntelliJ, pasar `-MavenCommand 'ruta\a\mvn.cmd'`. El parámetro opcional `-RepositorioLocal` permite elegir la caché de Maven.

También se verificó la base MySQL real de localhost usando registros temporales identificados con un UUID. Pasaron autenticación con el servicio real, carga del catálogo existente, altas y ediciones de artistas/álbumes/canciones/usuarios, bajas lógicas, creación y edición de playlists, agregado/quita de canciones y favoritos. Se volvió a consultar por JDBC después de las operaciones para comprobar persistencia. Los registros temporales se eliminaron al terminar, conservando los datos originales. La aplicación no crea datos de demostración ni cambia al modo de prueba automáticamente. Sigue siendo conveniente una revisión manual de los formularios con los usuarios habituales antes de una demostración.

No se implementa reproducción de audio. En el detalle de playlists, las cuentas con permiso de edición pueden reordenar canciones arrastrando filas. El nuevo orden se guarda en MySQL mediante una transacción; las cuentas de solo consulta no pueden modificarlo.

Todos los cambios quedan locales, sin commits, push ni pull request.
