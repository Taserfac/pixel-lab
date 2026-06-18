param(
  [switch]$Run,
  [switch]$SkipBuild,
  [switch]$Clean
)

$ErrorActionPreference = 'Stop'

$Workspace = Split-Path -Parent $MyInvocation.MyCommand.Path
$ToolsDir = Join-Path $Workspace '.tools'
$TomcatHome = if ($env:CATALINA_HOME) { $env:CATALINA_HOME } else { Join-Path $ToolsDir 'apache-tomcat-9.0.93' }
$MavenCmd = Join-Path $ToolsDir 'apache-maven-3.9.9\bin\mvn.cmd'
$MavenRepo = Join-Path $Workspace '.m2repo'
$JavaBackendDir = Join-Path $Workspace 'pixel-lab-java-backend'
$FrontendDir = Join-Path $Workspace 'pixel-lab-frontend'
$WarPath = Join-Path $JavaBackendDir 'target\ROOT.war'
$EnvFile = Join-Path $Workspace '.env.local'
$HealthUrl = 'http://127.0.0.1:8080/api/health'

function Import-DotEnv {
  param([string]$Path)

  if (!(Test-Path $Path)) {
    Write-Host "No .env found at $Path; using current shell environment."
    return
  }

  Get-Content $Path | ForEach-Object {
    $line = $_.Trim()
    if ($line.Length -eq 0 -or $line.StartsWith('#') -or !$line.Contains('=')) {
      return
    }

    $name, $value = $line.Split('=', 2)
    $name = $name.Trim()
    $value = $value.Trim()

    if ($value.Length -ge 2) {
      $first = $value.Substring(0, 1)
      $last = $value.Substring($value.Length - 1, 1)
      if (($first -eq '"' -and $last -eq '"') -or ($first -eq "'" -and $last -eq "'")) {
        $value = $value.Substring(1, $value.Length - 2)
      }
    }

    [Environment]::SetEnvironmentVariable($name, $value, 'Process')
  }
}

function Get-TomcatProcessIds {
  $ids = New-Object System.Collections.Generic.List[int]

  $listeners = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
  foreach ($listener in $listeners) {
    $pidValue = [int]$listener.OwningProcess
    $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $pidValue" -ErrorAction SilentlyContinue
    if ($processInfo -and $processInfo.CommandLine -and $processInfo.CommandLine.Contains($TomcatHome)) {
      $ids.Add($pidValue)
    }
    elseif ($processInfo) {
      throw "Port 8080 is already used by PID $pidValue. Stop it first, or change Tomcat's port."
    }
  }

  $tomcatProcesses = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
  Where-Object { $_.CommandLine -and $_.CommandLine.Contains('org.apache.catalina.startup.Bootstrap') -and $_.CommandLine.Contains($TomcatHome) }
  foreach ($processInfo in $tomcatProcesses) {
    $ids.Add([int]$processInfo.ProcessId)
  }

  return $ids | Select-Object -Unique
}

function Stop-ExistingTomcat {
  $ids = Get-TomcatProcessIds
  foreach ($id in $ids) {
    Stop-Process -Id $id -Force -ErrorAction SilentlyContinue
  }
  if ($ids.Count -gt 0) {
    Start-Sleep -Seconds 2
  }
}

function Set-ProcessPathCase {
  $pathValue = [Environment]::GetEnvironmentVariable('Path', 'Process')
  if ([string]::IsNullOrEmpty($pathValue)) {
    $pathValue = [Environment]::GetEnvironmentVariable('PATH', 'Process')
  }

  if (![string]::IsNullOrEmpty($pathValue)) {
    [Environment]::SetEnvironmentVariable('PATH', $null, 'Process')
    [Environment]::SetEnvironmentVariable('Path', $pathValue, 'Process')
  }
}

function New-WarPackage {
  if ($SkipBuild) {
    if (!(Test-Path $WarPath)) {
      throw "Missing $WarPath. Run without -SkipBuild first."
    }
    return
  }

  Write-Host 'Building frontend...'
  Push-Location $FrontendDir
  try {
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) {
      throw "Frontend build failed with exit code $LASTEXITCODE."
    }
  }
  finally {
    Pop-Location
  }

  if (!(Test-Path $MavenCmd)) {
    $MavenCmd = 'mvn.cmd'
  }

  Write-Host 'Building Java backend...'
  $mavenGoal = if ($Clean) { 'clean package' } else { 'package' }
  & $MavenCmd "-Dmaven.repo.local=$MavenRepo" -f (Join-Path $JavaBackendDir 'pom.xml') $mavenGoal.Split(' ')
  if ($LASTEXITCODE -ne 0) {
    throw "Maven build failed with exit code $LASTEXITCODE."
  }
}

function Install-WarToTomcat {
  if (!(Test-Path $TomcatHome)) {
    throw "Tomcat not found at $TomcatHome."
  }
  if (!(Test-Path $WarPath)) {
    throw "Missing WAR: $WarPath."
  }

  $webapps = Join-Path $TomcatHome 'webapps'
  $rootDir = Join-Path $webapps 'ROOT'
  $rootWar = Join-Path $webapps 'ROOT.war'

  if (Test-Path $rootDir) {
    Remove-Item -LiteralPath $rootDir -Recurse -Force
  }
  if (Test-Path $rootWar) {
    Remove-Item -LiteralPath $rootWar -Force
  }

  # 清理 Tomcat work 目录，避免编译缓存导致旧版本残留
  $workDir = Join-Path $TomcatHome 'work\Catalina'
  if (Test-Path $workDir) {
    Remove-Item -LiteralPath $workDir -Recurse -Force
  }

  Copy-Item -LiteralPath $WarPath -Destination $rootWar -Force
}

function Wait-ForHealth {
  for ($i = 0; $i -lt 30; $i++) {
    try {
      $response = Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing -TimeoutSec 2
      Write-Host "Java backend is ready: $HealthUrl"
      Write-Host $response.Content
      return
    }
    catch {
      Start-Sleep -Seconds 1
    }
  }

  throw "Java backend did not become healthy in time. Check $TomcatHome\logs."
}

Import-DotEnv $EnvFile
$env:CATALINA_HOME = $TomcatHome
$env:CATALINA_BASE = $TomcatHome

if ($Run) {
  Push-Location $TomcatHome
  try {
    & (Join-Path $TomcatHome 'bin\catalina.bat') run
    exit $LASTEXITCODE
  }
  finally {
    Pop-Location
  }
}

New-WarPackage
Stop-ExistingTomcat
Install-WarToTomcat

Write-Host 'Starting Tomcat in the background...'
$stdoutLog = Join-Path $ToolsDir 'tomcat-wrapper.out.log'
$stderrLog = Join-Path $ToolsDir 'tomcat-wrapper.err.log'

Set-ProcessPathCase

try {
  $process = Start-Process -FilePath 'powershell.exe' `
    -ArgumentList '-NoProfile', '-ExecutionPolicy', 'Bypass', '-File', $MyInvocation.MyCommand.Path, '-Run', '-SkipBuild' `
    -WindowStyle Hidden `
    -RedirectStandardOutput $stdoutLog `
    -RedirectStandardError $stderrLog `
    -PassThru

  Write-Host "Tomcat wrapper PID: $($process.Id)"
}
catch {
  Write-Host "Start-Process failed, falling back to cmd start: $($_.Exception.Message)"
  $scriptPath = $MyInvocation.MyCommand.Path
  $cmdLine = 'start "" /min powershell.exe -NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File "' + $scriptPath + '" -Run -SkipBuild'
  & cmd.exe /d /c $cmdLine
}

Wait-ForHealth
$tomcatIds = Get-TomcatProcessIds
if ($tomcatIds) {
  Write-Host "Tomcat PID(s): $($tomcatIds -join ', ')"
}
