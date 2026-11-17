param(
    [string]$MavenCommand = (Join-Path $PSScriptRoot '../mvnw.cmd'),
    [string]$RepositorioLocal = (Join-Path $env:USERPROFILE '.m2/repository')
)
$ErrorActionPreference = 'Stop'
Push-Location (Split-Path $PSScriptRoot -Parent)
try {
    & $MavenCommand -B -ntp "-Dmaven.repo.local=$RepositorioLocal" compile
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación Maven.' }
    $fxJars = @('base', 'graphics', 'controls') | ForEach-Object {
        Join-Path $RepositorioLocal "org/openjfx/javafx-$_/21.0.8/javafx-$_-21.0.8-win.jar"
    }
    foreach ($jar in $fxJars) { if (!(Test-Path -LiteralPath $jar)) { throw "Falta dependencia: $jar" } }
    $modulos = $fxJars -join ';'
    New-Item -ItemType Directory -Force target/test-classes | Out-Null
    & javac -encoding UTF-8 -cp "target/classes;$modulos" -d target/test-classes test/PruebaGUI.java
    if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación de la prueba.' }
    & java --module-path $modulos --add-modules javafx.controls -cp 'target/classes;target/test-classes' PruebaGUI
    if ($LASTEXITCODE -ne 0) { throw 'Falló la prueba aislada.' }
} finally { Pop-Location }
