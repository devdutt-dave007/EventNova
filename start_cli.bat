@echo off
title EventNova Java Console Portal
cd /d "%~dp0"
echo ====================================================
echo  Compiling and Running EventNova Java Console CLI
echo ====================================================

if not exist out mkdir out

dir /s /b src\*.java > sources.txt
javac -d out @sources.txt
del sources.txt

if %errorlevel% equ 0 (
    echo Compilation successful! Launching EventNova...
    java -cp out main.Main
) else (
    echo [ERROR] Compilation failed. Please check your JDK installation.
)
pause
