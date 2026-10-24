@echo off
echo ============================================================================
echo  Executing Automated Test Suite for KEC R2024 Evaluation Rules
echo ============================================================================
java -cp ".;lib\mysql_classes;lib\mysql-connector-j-9.1.0.jar" TestRunner
pause
