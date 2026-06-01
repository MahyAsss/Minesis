#!/bin/bash
# Gradle Wrapper script for Unix/Linux/Mac
# This script can be used instead of "gradle" command

# Get the absolute path of the script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Execute gradle wrapper
"$SCRIPT_DIR/gradlew" "$@"
