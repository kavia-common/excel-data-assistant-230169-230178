#!/bin/bash
cd /home/kavia/workspace/code-generation/excel-data-assistant-230169-230178/excel_dashboard_frontend
./gradlew lint
LINT_EXIT_CODE=$?
if [ $LINT_EXIT_CODE -ne 0 ]; then
   exit 1
fi

