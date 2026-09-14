$packageRoot = Split-Path -Parent $PSScriptRoot
$serverClassPath = (Join-Path $packageRoot 'mapServer/bin') + [IO.Path]::PathSeparator + (Join-Path $packageRoot 'mapServer/lib/mysql-connector-java-8.0.17.jar')
Push-Location -LiteralPath $packageRoot
try {
    & java -cp $serverClassPath Server.MultiServer
    $serverExit = $LASTEXITCODE
} finally { Pop-Location }
exit $serverExit
