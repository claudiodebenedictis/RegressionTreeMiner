param([string]$ServerHost = 'localhost', [int]$ServerPort = 8080)
$packageRoot = Split-Path -Parent $PSScriptRoot
& java -cp (Join-Path $packageRoot 'mapClient/bin') map7Client.MainTest $ServerHost $ServerPort
exit $LASTEXITCODE
