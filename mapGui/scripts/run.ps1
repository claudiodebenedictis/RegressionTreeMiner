param([switch]$SkipBuild)

$ErrorActionPreference = 'Continue'
$guiRoot = Split-Path -Parent $PSScriptRoot
$projectRoot = Split-Path -Parent $guiRoot
$fxLib = Join-Path $guiRoot 'lib/javafx-sdk-21.0.12/lib'
if (-not $SkipBuild) {
    & powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'build.ps1')
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}
$guiClassPath = (Join-Path $guiRoot 'bin') + [IO.Path]::PathSeparator + (Join-Path $guiRoot 'resources')
& java --module-path $fxLib --add-modules javafx.controls,javafx.fxml "-Dmap.root=$projectRoot" -cp $guiClassPath gui.Main
exit $LASTEXITCODE
