$ErrorActionPreference = 'Stop'

$Workspace = Split-Path -Parent $MyInvocation.MyCommand.Path
$ToolsDir = Join-Path $Workspace '.tools'
$TomcatHome = if ($env:CATALINA_HOME) { $env:CATALINA_HOME } else { Join-Path $ToolsDir 'apache-tomcat-9.0.93' }

$tomcatProcesses = Get-CimInstance Win32_Process -ErrorAction SilentlyContinue |
  Where-Object { $_.CommandLine -and $_.CommandLine.Contains('org.apache.catalina.startup.Bootstrap') -and $_.CommandLine.Contains($TomcatHome) }

$portListeners = Get-NetTCPConnection -LocalPort 8080, 8005 -State Listen -ErrorAction SilentlyContinue
foreach ($listener in $portListeners) {
  $pidValue = [int]$listener.OwningProcess
  $processInfo = Get-CimInstance Win32_Process -Filter "ProcessId = $pidValue" -ErrorAction SilentlyContinue
  if ($processInfo -and $processInfo.CommandLine -and $processInfo.CommandLine.Contains($TomcatHome)) {
    $tomcatProcesses += $processInfo
  }
}

$ids = $tomcatProcesses | Select-Object -ExpandProperty ProcessId -Unique
if (!$ids -or $ids.Count -eq 0) {
  Write-Host 'Pixel Lab Java backend is not running.'
  exit 0
}

foreach ($id in $ids) {
  Stop-Process -Id $id -Force -ErrorAction SilentlyContinue
}

Write-Host "Stopped Pixel Lab Java backend PID(s): $($ids -join ', ')"
