#!/usr/bin/env sh
set -eu

if [ "$#" -gt 1 ]; then
    echo "Usage: sh scripts/seed-demo-data.sh [database-path]" >&2
    exit 2
fi

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
gradlew="$script_dir/../gradlew"
if [ "$#" -eq 1 ]; then
    "$gradlew" databaseTool -PdatabaseToolCommand=seed "-PdatabasePath=$1"
else
    "$gradlew" databaseTool -PdatabaseToolCommand=seed
fi
