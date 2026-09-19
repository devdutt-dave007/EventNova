@echo off
title EventNova Web Server
cd /d "%~dp0web"
echo Starting EventNova Web Portal on http://localhost:5000 ...
node server.js
pause
