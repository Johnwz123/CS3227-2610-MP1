[CmdletBinding()]
param(
    [string]$DatabasePath
)

$gradleArguments = @(
    "databaseTool",
    "-PdatabaseToolCommand=seed"
)
if ($DatabasePath) {
    $gradleArguments += "-PdatabasePath=$DatabasePath"
}

& (Join-Path $PSScriptRoot "..\gradlew.bat") @gradleArguments
exit $LASTEXITCODE
