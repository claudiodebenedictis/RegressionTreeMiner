$ErrorActionPreference = 'Continue'
$guiRoot = Split-Path -Parent $PSScriptRoot
$fxLib = Join-Path $guiRoot 'lib/javafx-sdk-21.0.12/lib'
$guiBin = Join-Path $guiRoot 'bin'
if (-not (Test-Path -LiteralPath (Join-Path $fxLib 'javafx.fxml.jar'))) {
    throw 'JavaFX SDK 21.0.12 assente: consultare mapGui/README.md.'
}
$guiSources = @(Get-ChildItem -LiteralPath (Join-Path $guiRoot 'src') -Recurse -Filter '*.java' -File | ForEach-Object FullName)
New-Item -ItemType Directory -Path $guiBin -Force | Out-Null
& javac -encoding UTF-8 --module-path $fxLib --add-modules javafx.controls,javafx.fxml -d $guiBin @guiSources
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host 'Build mapGui riuscita (exit code 0).'
