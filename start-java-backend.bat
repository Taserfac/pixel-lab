@echo off
setlocal

set "PROJECT_ROOT=%~dp0"
if "%PROJECT_ROOT:~-1%"=="\" set "PROJECT_ROOT=%PROJECT_ROOT:~0,-1%"

set "CATALINA_HOME=%PROJECT_ROOT%\.tools\apache-tomcat-9.0.93"
if not defined DB_HOST set "DB_HOST=localhost"
if not defined DB_PORT set "DB_PORT=3306"
if not defined DB_NAME set "DB_NAME=pixel_lab"
if not defined DB_USER set "DB_USER=root"

if not defined DB_PASSWORD set "DB_PASSWORD=lvmanyi2006121"

set "WAR_SOURCE=%PROJECT_ROOT%\pixel-lab-java-backend\target\ROOT.war"
set "WAR_TARGET=%CATALINA_HOME%\webapps\ROOT.war"

if not exist "%CATALINA_HOME%\bin\startup.bat" (
  echo Tomcat startup script not found: "%CATALINA_HOME%\bin\startup.bat"
  exit /b 1
)

if exist "%WAR_SOURCE%" (
  echo Deploying latest Java backend WAR...
  copy /Y "%WAR_SOURCE%" "%WAR_TARGET%" >nul
) else (
  echo WAR not found, starting Tomcat with current deployment: "%WAR_SOURCE%"
)

echo Starting Pixel Lab Java backend on http://localhost:8080 ...
call "%CATALINA_HOME%\bin\startup.bat"
