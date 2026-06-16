@echo off
setlocal EnableExtensions

set "PROJECT_ROOT=%~dp0"
set "PID_FILE=%PROJECT_ROOT%\backend.pid"

echo Stopping Pixel Lab...

:: =========================
:: 1. 停后端（优先 PID）
:: =========================
if exist "%PID_FILE%" (
    set /p BACKEND_PID=<"%PID_FILE%"
    echo Killing backend PID: %BACKEND_PID%
    taskkill /F /PID %BACKEND_PID% >nul 2>nul
    del "%PID_FILE%" >nul 2>nul
) else (
    echo No PID file found, fallback killing java...
    taskkill /F /IM java.exe /T >nul 2>nul
)

:: =========================
:: 2. 停前端
:: =========================
taskkill /F /IM node.exe /T >nul 2>nul

:: =========================
:: 3. 清理端口兜底
:: =========================
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080') do taskkill /F /PID %%a >nul 2>nul
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :5173') do taskkill /F /PID %%a >nul 2>nul

echo Done.
pause
exit /b 0