#!/usr/bin/env sh
set -eu

if [ "$#" -gt 1 ]; then
    echo "Usage: sh scripts/reset-database.sh [database-path]" >&2
    exit 2
fi

printf '%s' "This permanently deletes the selected BudgetBot database. Type RESET to continue: "
read -r confirmation
if [ "$confirmation" != "RESET" ]; then
    echo "Reset cancelled. No database files were changed." >&2
    exit 2
fi

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
gradlew="$script_dir/../gradlew"
if [ "$#" -eq 1 ]; then
    "$gradlew" databaseTool -PdatabaseToolCommand=reset -PdatabaseToolForce=true "-PdatabasePath=$1"
else
    "$gradlew" databaseTool -PdatabaseToolCommand=reset -PdatabaseToolForce=true
fi
