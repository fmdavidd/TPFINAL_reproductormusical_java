# Verificación de la configuración Maven

Comprobado el 16/11/2026 en Windows con Temurin JDK 21.0.12.1:

- El wrapper oficial 3.3.4 descargó y ejecutó Maven 3.9.9 sin Maven en el PATH.
- `mvnw.cmd -o clean compile javafx:run` compiló los 47 archivos y abrió la ventana real «Biblioteca musical». Se comprobaron los controles de acceso mediante accesibilidad. Al cerrar, Maven terminó con BUILD SUCCESS.
- `scripts/verificar.ps1` terminó correctamente: autenticación, permisos, privacidad, playlists, favoritos y vistas JavaFX con JDBC simulado. No se probó ni modificó MySQL real en esta tarea.
- Una copia en otro directorio con espacios, sin `.idea`, módulos antiguos ni `lib`, compiló sin conexión. `Main` abrió su ventana con el classpath de las dependencias Maven, sin opciones VM ni SDK JavaFX externo. Esto comprueba también el lanzador que usa Run sobre `Main.main()`.
- No hay rutas absolutas de esta máquina en el POM, wrapper o configuración Run compartida.

La configuración compartida «Biblioteca musical» ejecuta `javafx:run`. No se pudo comprobar interactivamente el botón de IntelliJ: la captura de pantalla falló por composición de escritorio deshabilitada y el IDE solo expuso el marco de la ventana en accesibilidad. Sí se comprobó el objetivo Maven y el lanzador Java usados por las dos alternativas documentadas. No se probaron Linux/macOS.

Las rutas de caché alternativas utilizadas durante estas comprobaciones se pasaron únicamente al proceso de verificación; no forman parte de la configuración del proyecto.

También se comprobó el comando documentado '.\mvnw.cmd clean compile' usando la caché normal del usuario, sin parámetros de rutas: BUILD SUCCESS.
Los comandos javafx:run y -o javafx:run también abrieron la ventana con la caché normal del usuario, sin parámetros de rutas.
