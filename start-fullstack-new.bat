@echo off
setlocal EnableExtensions

set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"

set "FRONTEND_DIR=%PROJECT_ROOT%\pixel-lab-frontend"
set "BACKEND_SCRIPT=%PROJECT_ROOT%\start-java-backend.ps1"
set "PID_FILE=%PROJECT_ROOT%\backend.pid"

echo Pixel Lab Fullstack Startup
echo.

:: =========================
:: 1. 清理旧端口（关键）
:: =========================
echo Cleaning old processes...

for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080') do taskkill /F /PID %%a >nul 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173') do taskkill /F /PID %%a >nul 2>nul

:: =========================
:: 2. 启动后端（记录 PID）
:: =========================
echo Starting Java backend...

for /f %%i in ('
  powershell -NoProfile -Command ^
  "$p = Start-Process powershell -ArgumentList "-NoProfile -ExecutionPolicy Bypass -File \"%BACKEND_SCRIPT%\"" -PassThru; $p.Id"
') do set BACKEND_PID=%%i

echo %BACKEND_PID% > "%PID_FILE%"
echo Backend PID: %BACKEND_PID%

:: =========================
:: 3. 启动前端
:: =========================
echo.
echo Starting frontend...

start "Pixel Lab Frontend" /D "%FRONTEND_DIR%" cmd /k "npm run dev"

echo.
echo =========================
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:5173
echo =========================
echo.

pause
exit /b 0