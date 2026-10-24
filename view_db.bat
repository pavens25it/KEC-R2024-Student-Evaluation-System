@echo off
echo ============================================================================
echo  Viewing KEC (R2024) Student, Course, and Enrollment Tables
echo ============================================================================
java -cp ".;lib\mysql_classes;lib\mysql-connector-j-9.1.0.jar" ViewDatabase
pause
