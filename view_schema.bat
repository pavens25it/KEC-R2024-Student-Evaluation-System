@echo off
echo ============================================================================
echo  Inspecting KEC (R2024) Database Table Schemas in Terminal
echo ============================================================================
java -cp ".;lib\mysql_classes;lib\mysql-connector-j-9.1.0.jar" ViewSchema
pause
