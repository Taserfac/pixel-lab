@echo off
setlocal EnableExtensions

set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"

set "FRONTEND_DIR=%PROJECT_ROOT%\pixel-lab-frontend"
set "BACKEND_SCRIPT=%PROJECT_ROOT%\start-java-backend.ps1"
set "BUNDLED_MAVEN=%PROJECT_ROOT%\.tools\apache-maven-3.9.9\bin\mvn.cmd"
set "BUNDLED_TOMCAT=%PROJECT_ROOT%\.tools\apache-tomcat-9.0.93"
set "NO_PAUSE="
if /I "%~1"=="--no-pause" set "NO_PAUSE=1"

echo Pixel Lab full stack startup
echo.

if not exist "%BACKEND_SCRIPT%" (
  echo Missing backend startup script: "%BACKEND_SCRIPT%"
  goto :fail
)

if not exist "%FRONTEND_DIR%\package.json" (
  echo Missing frontend project: "%FRONTEND_DIR%\package.json"
  goto :fail
)

where npm.cmd >nul 2>nul
if errorlevel 1 (
  echo Node.js/npm was not found. Install Node.js, then reopen this terminal.
  goto :fail
)

if defined JAVA_HOME (
  if not exist "%JAVA_HOME%\bin\java.exe" (
    echo JAVA_HOME does not point to a valid JDK:
    echo   "%JAVA_HOME%"
    goto :fail
  )
) else (
  where java.exe >nul 2>nul
  if errorlevel 1 (
    echo Java was not found. Install JDK 17 and set JAVA_HOME, then reopen this terminal.
    goto :fail
  )
)

if not exist "%FRONTEND_DIR%\node_modules\.bin\vite.cmd" (
  echo Frontend dependencies are missing.
  echo Run:
  echo   cd /d "%FRONTEND_DIR%"
  echo   npm install
  goto :fail
)

if not exist "%BUNDLED_MAVEN%" (
  where mvn.cmd >nul 2>nul
  if errorlevel 1 (
    where mvn >nul 2>nul
    if errorlevel 1 (
      echo Maven was not found.
      echo Install Maven and add it to PATH, or extract apache-maven-3.9.9 to:
      echo   "%PROJECT_ROOT%\.tools"
      goto :fail
    )
  )
)

if defined CATALINA_HOME (
  set "TOMCAT_HOME=%CATALINA_HOME%"
) else (
  set "TOMCAT_HOME=%BUNDLED_TOMCAT%"
)

if not exist "%TOMCAT_HOME%\bin\catalina.bat" (
  echo Tomcat was not found.
  echo Set CATALINA_HOME to your Tomcat folder, or extract apache-tomcat-9.0.93 to:
  echo   "%PROJECT_ROOT%\.tools"
  goto :fail
)

if not exist "%PROJECT_ROOT%\pixel-lab-backend\.env" (
  echo Warning: "%PROJECT_ROOT%\pixel-lab-backend\.env" was not found.
  echo The backend may fail if DB_PASSWORD and other settings are not in the environment.
  echo.
)

echo Starting Java backend...
powershell.exe -NoProfile -ExecutionPolicy Bypass -File "%BACKEND_SCRIPT%"
if errorlevel 1 (
  echo Java backend failed to start.
  goto :fail
)

echo.
echo Starting Vue frontend...
start "Pixel Lab Frontend" /D "%FRONTEND_DIR%" cmd.exe /k "npm.cmd run dev"

echo.
echo Startup commands completed.
echo Backend:  http://localhost:8080
echo Frontend: http://localhost:5173
echo.
if not defined NO_PAUSE pause
exit /b 0

:fail
echo.
echo Startup stopped.
if not defined NO_PAUSE pause
exit /b 1
