[CmdletBinding()]
param(
    [string]$DatabasePath,
    [switch]$Force
)

if (-not $Force) {
    $confirmation = Read-Host "This permanently deletes the selected BudgetBot database. Type RESET to continue"
    if ($confirmation -cne "RESET") {
        Write-Error "Reset cancelled. No database files were changed."
        exit 2
    }
}

$gradleArguments = @(
    "databaseTool",
    "-PdatabaseToolCommand=reset",
    "-PdatabaseToolForce=true"
)
if ($DatabasePath) {
    $gradleArguments += "-PdatabasePath=$DatabasePath"
}

& (Join-Path $PSScriptRoot "..\gradlew.bat") @gradleArguments
exit $LASTEXITCODE
