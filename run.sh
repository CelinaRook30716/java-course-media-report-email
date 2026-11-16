#!/usr/bin/env sh
set -eu
rm -rf out
mkdir -p out
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out edu.report.ReportDeliveryServiceTest
if [ "${RUN_LIVE:-0}" = "1" ]; then
  java -cp out edu.report.ReportDemo
fi
