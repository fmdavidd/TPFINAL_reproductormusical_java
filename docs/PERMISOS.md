# Permisos administrativos

La autorización está centralizada en `SesionService`: `puedeConsultarTodo()`, `puedeEditarContenido()`, `puedeGestionarUsuarios()` y `puedeGestionarAdministradores()`. Las comprobaciones consultan la cuenta activa del usuario autenticado; el nivel elegido en un formulario no determina los permisos del actor.

| Cuenta | Consulta del catálogo y playlists privadas ajenas | ABM del catálogo | Gestión de cuentas y niveles |
| --- | --- | --- | --- |
| NORMAL | Catálogo, playlists propias y públicas | Sus propias playlists | No |
| VISUALIZANTE | Sí | No | No |
| EDITOR | Sí | Sí | No |
| SUPERADMIN | Sí | Sí | Sí |

## Clases modificadas

- `SesionService`: permisos por nivel, exigencias con `PermisoDenegadoException`, identidad de sesión por ID y registro público exclusivo de usuarios normales sin premium.
- `UsuarioService`: altas administrativas, edición, baja, reactivación y listado de cuentas exclusivos de SUPERADMIN. Protege el último SUPERADMIN activo, rechaza la baja propia y el cambio del tipo de cuenta. Devuelve copias de administradores para que modificar un objeto o formulario no cambie los permisos en memoria antes de autorizar y persistir. Cambiar nivel conserva las credenciales. Reutiliza `registrarAccion` y `UsuarioDAO`.
- `ArtistaService`, `AlbumService`, `CancionService`: altas, modificaciones, bajas y reactivaciones exigen EDITOR o SUPERADMIN. Registran acciones después de guardar.
- `PlaylistService`: separa consulta de edición; VISUALIZANTE ve todas las playlists y no las modifica. NORMAL conserva las reglas de propiedad y visibilidad. Registra cambios administrativos, incluidas altas y bajas de canciones.
- `MainView`, `CancionesView`, `ArtistasView`, `AlbumesView`, `PlaylistsView`, `UsuariosView`, `ListaView`: usan los permisos centralizados y presentan errores de autorización en los manejadores existentes. El filtro de propietarios de playlists se obtiene de las playlists visibles sin abrir la gestión de cuentas.
- `PruebaGUI`: amplía la prueba aislada existente con escenarios por nivel y controles JavaFX.
- `PruebaMySQLLectura` (nueva): verifica lectura real de niveles y carga de servicios sin modificar datos ni mostrar credenciales.

No se cambiaron las jerarquías de modelos, los enums ni el esquema. `UsuarioDAO` ya escribe `nivel_admin` y lo reconstruye con `NivelAdmin.valueOf`; se conserva ese comportamiento y el SQL sigue en los DAO.

## Uso de la interfaz

Al iniciar sesión se crea un `MainView` nuevo. VISUALIZANTE ve catálogo, filtros y detalles, sin altas ni edición; las acciones de edición de playlists están deshabilitadas. EDITOR ve ABM del catálogo. Solo SUPERADMIN ve **Usuarios**.

Desde **Usuarios → Agregar**, SUPERADMIN ingresa nombre, correo y contraseña, elige Administrador y selecciona `NivelAdmin` en el ComboBox. El formulario valida, genera salt aleatorio y hash con `Seguridad`, guarda mediante el servicio y actualiza la tabla. La columna **Nivel** muestra el nivel administrativo. Para editarlo, seleccionar la cuenta y pulsar **Editar**. La contraseña nueva es opcional; si queda vacía, se conservan hash y salt. Ninguna tabla muestra esas credenciales.

Tras cerrar sesión e iniciar con otra cuenta, el menú y los controles se construyen con los permisos de esa cuenta, sin reiniciar la aplicación. Los servicios vuelven a comprobar el permiso en cada operación, aunque se invoquen directamente o quede abierto un formulario anterior. El detalle de una playlist permite cambiar el orden mediante arrastre de filas cuando la cuenta tiene permiso de edición.

## Verificación ejecutada

`./scripts/verificar.ps1` compila la aplicación y ejecuta JavaFX con un doble JDBC, sin tocar MySQL. Se verificaron:

- NORMAL: favoritos, playlists propias, privacidad ajena y rechazo de privilegios administrativos.
- VISUALIZANTE: consulta privada ajena y rechazo de altas, edición, bajas, reactivaciones y cambios de canciones.
- EDITOR: ABM de artistas, álbumes, canciones y playlists; edición de playlists ajenas; rechazo de gestión de cuentas y niveles.
- SUPERADMIN: creación de administradores, cambio de niveles, conservación de credenciales y bajas de otras cuentas.
- Rechazo sin sesión, autoescalada mediante servicios y mutación de objetos administrativos, baja propia y degradación del último SUPERADMIN.
- Cambios de sesión y controles JavaFX por nivel; actualización de permisos del servicio tras degradación propia cuando existe otro SUPERADMIN.
- Logs de cambios exitosos; ausencia de escrituras y logs en operaciones rechazadas; fallo simulado del DAO sin cambiar el nivel en memoria.
- Parámetros JDBC de `nivel_admin` en altas y modificaciones y reconstrucción DAO de los tres niveles.

También se ejecutó `PruebaMySQLLectura` contra la base local real: reconstruyó 1 SUPERADMIN, 1 EDITOR, 1 VISUALIZANTE y 14 usuarios normales activos; cargó 15 artistas, 15 canciones y 15 álbumes. Fueron exclusivamente lecturas.

Para repetir la prueba real, después de compilar:

```powershell
$mysqlJar = Join-Path $env:USERPROFILE '.m2/repository/com/mysql/mysql-connector-j/9.1.0/mysql-connector-j-9.1.0.jar'
javac -encoding UTF-8 -cp "target/classes;$mysqlJar" -d target/test-classes test/PruebaMySQLLectura.java
java -cp "target/classes;target/test-classes;$mysqlJar" PruebaMySQLLectura
```

## Límites y verificación manual pendiente

Las escrituras reales de MySQL y el recorrido completo de los formularios con cuentas existentes quedan para una prueba manual con cuentas de prueba; no se alteraron los datos reales para verificarlas. El doble JDBC y los controles JavaFX sí ejercitan los escenarios indicados, pero no sustituyen esa prueba de persistencia.

Se conserva la arquitectura de colecciones en memoria: la protección del último SUPERADMIN y los permisos toman el estado de esta instancia del programa. No hay coordinación transaccional entre varias aplicaciones ejecutadas simultáneamente o cambios externos a MySQL. Para soportar ese uso sería necesario bloquear y validar en una transacción del DAO.

La escritura del cambio y la de su log usan conexiones independientes, como en los DAO existentes. Si guardar el log falla después del cambio, el servicio informa que el cambio ya fue guardado y que falló el registro; no se revierte automáticamente.

Todo el trabajo permanece local, sin commits ni publicaciones. Se preservaron las pantallas y modificaciones previas.

## Reactivar cuentas dadas de baja

SUPERADMIN puede entrar en **Usuarios**, elegir **Inactivos** en el selector de estado, buscar por nombre o correo, seleccionar una cuenta y pulsar **Reactivar**. Se solicita confirmación y la tabla se actualiza después de guardar. Al deselegir **Inactivos** en el selector de estado aparece la cuenta nuevamente en la lista activa. **Actualizar** vuelve a consultar los estados en la base.

La reactivación utiliza `UsuarioService.reactivar()`: exige SUPERADMIN, conserva nombre, correo, credenciales, tipo de cuenta y nivel administrativo, y registra la acción. Las cuentas inactivas no se pueden editar ni eliminar desde esa lista. La prueba aislada cubre usuarios normales y administradores recuperados, inicio de sesión posterior, rechazo de reactivación duplicada y actualización de controles y filtros JavaFX.

## Consultar y reactivar el catálogo dado de baja

Las pantallas **Canciones**, **Artistas**, **Álbumes** y **Playlists** tienen un selector **Activos / Inactivos / Ambos** para administradores. VISUALIZANTE puede consultar los tres estados; EDITOR y SUPERADMIN también pueden seleccionar una fila inactiva y pulsar **Reactivar**. La operación pide confirmación, guarda mediante el servicio existente, registra la acción y actualiza la lista. Los usuarios normales mantienen su catálogo y playlists activas habituales.

El estado se combina con los filtros existentes: texto, artistas, géneros, propietario y visibilidad según la pantalla. Los filtros incluyen artistas o propietarios que solo aparecen en registros inactivos. **Limpiar filtros** vuelve al catálogo activo. Los controles se distribuyen en varias filas cuando falta ancho.

En una fila dada de baja no se puede editar, eliminar ni modificar sus canciones; primero hay que reactivarla. Se conservan las validaciones del servicio: un álbum necesita un artista activo, y una canción necesita su artista y, cuando corresponda, su álbum activos. Las consultas de bajas exigen administrador en los servicios; las reactivaciones siguen exigiendo EDITOR o SUPERADMIN.

La prueba aislada verifica los cuatro archivos con datos JDBC simulados: Inactivos/Ambos, filtros combinados, acciones habilitadas por estado, permisos de NORMAL y VISUALIZANTE, recuperación por EDITOR, rechazo por dependencias inactivas, logs y desaparición de las filas recuperadas de la lista de bajas. No modifica el catálogo real de MySQL.

## Historial administrativo

Solo SUPERADMIN ve la opción **Historial administrativo** en el menú. La tabla consulta `registro_accion` y muestra ID de registro, fecha y hora, ID del administrador, nombre, correo y acción, con los registros más recientes primero. Incluye acciones de cuentas actualmente dadas de baja y conserva registros que coinciden en fecha y hora.

El historial permite buscar por acción, administrador, correo o identificador, limpiar la búsqueda y actualizar desde MySQL. Se carga fuera del hilo gráfico y es de solo lectura. `UsuarioService.listarHistorialAdministrativo()` vuelve a exigir SUPERADMIN antes de llamar al DAO; ocultar el menú no es la única protección. La consulta SQL está en `UsuarioDAO` y no requiere cambios de esquema. El modelo `RegistroAdministrativo` no contiene contraseñas, hashes ni salts.

Las pruebas verifican el rechazo sin sesión y para NORMAL, VISUALIZANTE y EDITOR; el menú y panel exclusivos de SUPERADMIN; la carga de acciones recientes e históricas de administradores inactivos; búsquedas, limpieza, actualización, historial vacío y ausencia de escrituras al consultar.

## Rangos Desde / Hasta

**Usuarios** permite filtrar por fecha de registro; **Historial administrativo**, por día de la acción; **Favoritos**, por fecha de incorporación de la canción. Se pueden elegir ambos extremos o uno solo. El rango incluye los días indicados completos, incluso la última hora del día del historial. Si Desde es posterior a Hasta se muestra una indicación junto al filtro y no hay resultados. **Limpiar filtros** borra los extremos.

**Álbumes** permite indicar año de lanzamiento desde/hasta: el modelo solo guarda el año, por lo que no se inventan meses ni días. Los valores deben estar entre 1 y 9999, con el inicio menor o igual al final. Los demás filtros y el estado activo/inactivo siguen combinándose con estos rangos.

Las fechas de favoritos se recuperan de `favorito.fecha_agregado` con el usuario y se conservan al editar su cuenta. Se agrega la columna **Agregada el** a Favoritos. Un favorito con fecha SQL NULL aparece sin rango y se excluye al aplicar límites. No se cambió el esquema. Canciones, artistas y playlists no guardan una fecha propia en el modelo y no reciben fechas ficticias.

## Orden de canciones mediante drag and drop

En **Playlists → Ver canciones**, el propietario normal, EDITOR y SUPERADMIN pueden arrastrar una fila a otra posición; soltarla en una fila vacía la coloca al final. VISUALIZANTE, otros usuarios normales y las playlists dadas de baja se mantienen en consulta. Los encabezados no cambian el orden de reproducción. La numeración se actualiza al guardar y el diálogo permanece abierto durante la operación.

`PlaylistService.moverCancion()` comprueba sesión, permiso, pertenencia de la canción y destino antes de llamar a `PlaylistDAO.reordenarCanciones()`. El DAO bloquea la playlist y sus vínculos, verifica que el orden visible siga siendo el esperado y actualiza las posiciones en una transacción. Si hay un cambio concurrente se pide actualizar la playlist. Las canciones inactivas no se eliminan ni se trasladan: conservan sus lugares entre las canciones visibles. La colección en memoria cambia solo después del commit; un error SQL revierte las posiciones en la base. Los cambios administrativos reutilizan el historial existente.

La prueba aislada verifica movimientos en ambos sentidos, ausencia de escrituras para movimientos sin cambios, preservación de canciones ocultas, permisos de propietario/otro usuario/VISUALIZANTE/EDITOR/SUPERADMIN, destino y canción inválidos, rollback tras una escritura parcial, rechazo de orden desactualizado, logs y manejadores JavaFX de arrastre solo para cuentas autorizadas.

También se comprobó el guardado real en MySQL con una cuenta normal y una playlist temporales: se movió una canción al final y al principio, y se verificó cada resultado con una nueva lectura del DAO. Los datos temporales se eliminaron al terminar, sin modificar el catálogo ni las cuentas existentes.
