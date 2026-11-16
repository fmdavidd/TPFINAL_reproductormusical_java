# Gestor musical — Programacion II

Proyecto Java de consola con POO, JDBC y MySQL. Abrir **esta carpeta**
`TPFINAL_reproductormusical_java` en IntelliJ; el `src` de la carpeta superior
pertenece a otro proyecto.

## Requisitos y configuracion

- JDK 21 o posterior. Compilado y probado localmente con OpenJDK 21; la configuracion
  original de IntelliJ referencia un JDK 26. Seleccionar un JDK instalado en
  File > Project Structure > Project SDK y un nivel de lenguaje compatible.
- MySQL 8 o posterior, con un usuario autorizado a operar la base.
- Driver JDBC incluido en `lib/mysql-connector-j-26.10.0.jar`. En IntelliJ ya esta
  declarado como biblioteca; si aparece sin resolver, agregar ese JAR en Modules > Dependencies.
- Sin Maven, Gradle ni otras dependencias. Python 3 solo es opcional para comprobar los datos.

La conexion lee estas variables de entorno (tambien acepta propiedades Java `-D`
con los mismos nombres; las propiedades tienen prioridad):

| Variable | Valor predeterminado |
| --- | --- |
| `MUSICA_DB_URL` | `jdbc:mysql://localhost:3306/ReproductorMusicalBD?connectTimeout=5000&socketTimeout=10000` |
| `MUSICA_DB_USER` | `root` |
| `MUSICA_DB_PASSWORD` | cadena vacia |

En IntelliJ: Run > Edit Configurations > Main > Environment variables. En PowerShell,
antes de ejecutar Java, por ejemplo:

```powershell
$env:MUSICA_DB_USER = 'usuario_mysql'
$env:MUSICA_DB_PASSWORD = 'su_clave'
```

## Inicializar una base de demostracion

En MySQL Workbench, abrir y ejecutar manualmente, en este orden:

1. `sql/01_esquema.sql`.
2. `sql/02_datos.sql`.

Se crean 15 usuarios, 15 artistas, 15 albumes, 16 canciones y 15 playlists,
con favoritos y relaciones de ejemplo. Los SELECT finales permiten comprobar
cantidades, artistas de las canciones y posiciones de playlists.

Estos scripts son para una **base nueva**: no son migraciones ni se pueden repetir
sobre tablas existentes. No contienen DROP ni borran una base. Si ya tiene datos,
conservarlos y preparar otra base de demostracion cambiando el nombre de base en
los dos scripts y en `MUSICA_DB_URL`. La aplicacion nunca ejecuta estos scripts.
`lib/sql.txt` contiene la misma inicializacion combinada para compatibilidad:
ejecutar esa copia **o** los dos archivos anteriores, nunca ambas alternativas.

## Ejecutar

En IntelliJ, ejecutar `src/Main.java`. Tambien, desde esta carpeta en PowerShell:

```powershell
$fuentes = @(Get-ChildItem -Recurse src -Filter '*.java' | ForEach-Object { $_.FullName })
javac -encoding UTF-8 -cp 'lib/*' -d out @fuentes
java -cp 'out;lib/*' Main
```

Para ver un ejemplo directo de Java consultando los datos ya cargados, ejecutar
`src/ConsultarBaseDatos.java` en IntelliJ o, despues de compilar:

```powershell
java -cp 'out;lib/*' ConsultarBaseDatos
```

Ese ejemplo abre la conexion JDBC y lista las canciones disponibles con artista,
album y duracion. Solo lee: no crea tablas ni inserta datos. Usa la misma configuracion
`MUSICA_DB_URL`, `MUSICA_DB_USER` y `MUSICA_DB_PASSWORD` que el sistema.
`Main` sigue siendo el punto de entrada de la aplicacion completa, con permisos.
Las credenciales de MySQL son distintas de los usuarios musicales de prueba.
La base local ya fue inicializada durante la verificacion; no repetir los scripts
sobre ella. Para entregar el proyecto basta incluir los scripts y explicar su ejecucion.

La consola ofrece registro, inicio/cierre de sesion, consultas y CRUD de las cinco
entidades. Los usuarios normales gestionan sus playlists y favoritos. En playlists
se pueden agregar, quitar y mover canciones; las posiciones visibles empiezan en 1.
Los errores vuelven al menu principal; los valores no numericos se vuelven a pedir.
`0` vuelve al menu anterior o sale desde el principal. Fin de entrada tambien cierra.
Las contrasenias se ingresan mediante Scanner y son visibles en la terminal.

## Cuentas de prueba

| Email | Clave | Permiso |
| --- | --- | --- |
| `admin@musica.com` | `admin123` | SUPERADMIN |
| `laura@musica.com` | `admin123` | EDITOR |
| `tomas@musica.com` | `admin123` | VISUALIZANTE |
| `juan@mail.com` | `1234` | Normal premium |
| `maria@mail.com` | `1234` | Normal |

Tambien hay cuentas normales `lucia`, `diego`, `sofia`, `mateo`, `valentina`,
`nicolas`, `camila`, `martin`, `paula` y `andres`, todas con dominio `@mail.com`
y clave `1234`. Los hashes usan el algoritmo existente: SHA-256 de `salt + clave`.
Estas claves son exclusivamente de demostracion.

## Reglas del sistema

- Todas las consultas del dominio y modificaciones requieren sesion, salvo registro
  y autenticacion. Un login fallido cierra cualquier sesion anterior. La sesion guarda
  el ID y vuelve a consultar el usuario; una baja revoca su acceso. Un error de base
  al validar la sesion tambien la cierra.
- EDITOR y SUPERADMIN administran artistas, albumes y canciones. Solo SUPERADMIN
  crea, modifica, da de baja usuarios y cambia niveles. VISUALIZANTE consulta.
  No se permite dar de baja la propia cuenta ni cambiar su propio nivel.
- Solo usuarios normales crean playlists y favoritos. Solo el propietario modifica
  sus playlists, incluso si son publicas. Una playlist privada solo la ve su propietario,
  tambien frente a administradores. Los favoritos son personales.
- La baja es logica. Se impide dar de baja artistas con albumes o canciones activas,
  albumes con canciones activas y usuarios con playlists activas. Primero deben
  resolver esas dependencias; no hay cascadas de bajas de artistas o albumes.
- Las canciones inactivas desaparecen del catalogo, favoritos y playlists. Al dar de
  baja una cancion se retira su relacion de las playlists afectadas y se renumeran.
  Los favoritos conservan su relacion en la base, pero no la muestran. No se ofrece
  reactivacion. Datos antiguos con padres inactivos tampoco se muestran como activos.
- Los emails de cuentas inactivas siguen reservados: se rechaza otro registro con
  el mismo email con un mensaje claro. No se reutilizan ni se reactivan automaticamente.
- Los servicios consultan la base en lugar de mantener caches mutables. Cada lectura
  entrega objetos nuevos; son instantaneas. Modificar un objeto no guarda cambios:
  hay que llamar al servicio. Despues de una operacion se vuelve a consultar para
  obtener favoritos, playlists y demas relaciones actualizados.
- Un album consultado carga sus canciones activas y permite calcular su duracion.
  Las referencias a album dentro de una cancion contienen solo metadatos para evitar
  carga recursiva. `Reproducible` agrupa titulo, duracion y canciones; no es un motor
  de audio. No se encontro una especificacion que exigiera reproducir archivos.
- La consola registra las acciones administrativas exitosas. Si falla ese registro,
  informa que la operacion principal ya se realizo. La carga del historial conserva
  acciones con la misma fecha y segundo mediante claves en memoria distintas.

## Verificacion y limitaciones

```powershell
javac -encoding UTF-8 -cp 'out;lib/*' -d out tests/PruebasLocales.java
java -cp 'out;lib/*' Service.PruebasLocales
python tests/verificar_datos.py
```

Pasaron 23 comprobaciones locales de modelos, sesiones y permisos. El chequeo de
los datos verifica cantidades, claves foraneas, propietarios, orden y hashes en
SQLite adaptando solo las declaraciones del esquema. **No certifica el dialecto
MySQL ni la persistencia JDBC.** Se comprobo tambien el menu inicial con entradas
invalidas, error de conexion y salida. La conexion desde el entorno aislado estaba bloqueada. Posteriormente se confirmo
que MySQL80 esta iniciado y Workbench conecta mediante `conn` a 127.0.0.1:3306.
Con autorizacion del usuario se ejecuto la inicializacion completa en Workbench:
las 31 sentencias terminaron correctamente. Se comprobaron en MySQL 15 usuarios,
15 artistas, 15 albumes, 16 canciones y 15 playlists; las tres consultas de control
de relaciones y posiciones devolvieron cero inconsistencias. No se modificaron
las otras bases. Falta configurar credenciales validas para Java y probar CRUD,
menus autenticados y reinicio mediante JDBC.
El guion de prueba esta en `VERIFICACION.md`.

Por consigna no hay transacciones JDBC: cada sentencia se confirma individualmente.
Alta/modificacion de generos, limpieza/renumeracion de playlists y registro de acciones
pueden quedar parcialmente aplicados si falla una sentencia posterior. La siguiente
modificacion de una playlist limpia canciones no disponibles y normaliza su orden.
Mover canciones guarda todas las posiciones visibles con un solo UPDATE CASE.
Agregar usa MAX(posicion)+1 despues de normalizar. No hay garantias de concurrencia
entre varias instancias: las comprobaciones de duplicados y permisos pueden competir
con cambios externos. El alcance es una demostracion de consola con una instancia.
No se agregaron transacciones ni diagramas UML.
