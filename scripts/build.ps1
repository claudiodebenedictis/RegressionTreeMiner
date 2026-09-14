# Compila i tre progetti separati, senza scaricare dipendenze.
$ErrorActionPreference = 'Continue'
$packageRoot = Split-Path -Parent $PSScriptRoot
foreach ($component in @('mapServer', 'mapClient')) {
    $componentRoot = Join-Path $packageRoot $component
    $componentSources = @(Get-ChildItem -LiteralPath (Join-Path $componentRoot 'src') -Recurse -File -Filter '*.java' | ForEach-Object FullName)
    $componentBin = Join-Path $componentRoot 'bin'
    New-Item -ItemType Directory -Path $componentBin -Force | Out-Null
    & javac -encoding UTF-8 -d $componentBin @componentSources
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
    Write-Host "Build $component riuscita (exit code 0)."
}
& powershell.exe -NoProfile -ExecutionPolicy Bypass -File (Join-Path $packageRoot 'mapGui/scripts/build.ps1')
exit $LASTEXITCODE
