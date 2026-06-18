@echo off
setlocal

set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"

set "FRONTEND_DIR=%PROJECT_ROOT%\pixel-lab-frontend"
set "BACKEND_SCRIPT=%PROJECT_ROOT%\start-java-backend.bat"

if not exist "%BACKEND_SCRIPT%" (
  echo Backend script not found: "%BACKEND_SCRIPT%"
  pause
  exit /b 1
)

if not exist "%FRONTEND_DIR%\package.json" (
  echo Frontend package.json not found: "%FRONTEND_DIR%\package.json"
  pause
  exit /b 1
)

echo Starting Pixel Lab Java backend...
start "Pixel Lab Backend" /D "%PROJECT_ROOT%" cmd /k call "%BACKEND_SCRIPT%"

echo Waiting for backend to initialize...
timeout /t 3 /nobreak >nul

echo Stopping old frontend dev server on port 5173...
for /f "tokens=5" %%P in ('netstat -ano ^| findstr ":5173" ^| findstr "LISTENING"') do (
  taskkill /PID %%P /F >nul 2>nul
)

echo Starting Pixel Lab frontend dev server...
start "Pixel Lab Frontend" /D "%FRONTEND_DIR%" cmd /k npm run dev -- --host localhost --strictPort

echo.
echo Pixel Lab is starting.
echo Backend:  http://localhost:8080/api/health
echo Frontend: http://localhost:5173/dashboard
echo.
echo Open this URL manually:
echo http://localhost:5173/dashboard
echo.
pause
