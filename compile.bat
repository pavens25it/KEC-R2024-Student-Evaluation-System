@echo off
echo ============================================================================
echo  Compiling Kongu Engineering College (R2024) Academic System
echo ============================================================================
javac -cp ".;lib\mysql_classes;lib\mysql-connector-j-9.1.0.jar" *.java
if %ERRORLEVEL% equ 0 (
    echo [SUCCESS] All Java source files compiled successfully!
) else (
    echo [ERROR] Compilation failed. Please check the error messages above.
)
pause
