param(
  [string]$PackageName = 'pixel-lab-release'
)

$ErrorActionPreference = 'Stop'

$Workspace = Resolve-Path (Join-Path $PSScriptRoot '..')
$BuildDir = Join-Path $PSScriptRoot 'build'
$ReleaseDir = Join-Path $BuildDir $PackageName
$ZipPath = Join-Path $BuildDir "$PackageName.zip"
$FrontendDir = Join-Path $Workspace 'pixel-lab-frontend'
$JavaBackendDir = Join-Path $Workspace 'pixel-lab-java-backend'
$MavenCmd = Join-Path $Workspace '.tools\apache-maven-3.9.9\bin\mvn.cmd'
$MavenRepo = Join-Path $Workspace '.m2repo'

if (!(Test-Path $MavenCmd)) {
  $MavenCmd = 'mvn.cmd'
}

if (Test-Path $ReleaseDir) {
  Remove-Item -LiteralPath $ReleaseDir -Recurse -Force
}
if (Test-Path $ZipPath) {
  Remove-Item -LiteralPath $ZipPath -Force
}

New-Item -ItemType Directory -Force -Path $ReleaseDir | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $ReleaseDir 'release') | Out-Null
New-Item -ItemType Directory -Force -Path (Join-Path $ReleaseDir 'sql') | Out-Null

Write-Host 'Building frontend production bundle...'
Push-Location $FrontendDir
try {
  & npm.cmd run build
  if ($LASTEXITCODE -ne 0) {
    throw "Frontend build failed with exit code $LASTEXITCODE."
  }
} finally {
  Pop-Location
}

Write-Host 'Building Java ROOT.war...'
& $MavenCmd "-Dmaven.repo.local=$MavenRepo" -f (Join-Path $JavaBackendDir 'pom.xml') clean package
if ($LASTEXITCODE -ne 0) {
  throw "Maven build failed with exit code $LASTEXITCODE."
}

Copy-Item -LiteralPath (Join-Path $JavaBackendDir 'target\ROOT.war') -Destination (Join-Path $ReleaseDir 'release\ROOT.war') -Force
Copy-Item -LiteralPath (Join-Path $JavaBackendDir 'src\main\resources\init.sql') -Destination (Join-Path $ReleaseDir 'sql\init.sql') -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'README.md') -Destination (Join-Path $ReleaseDir 'README.md') -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'env') -Destination (Join-Path $ReleaseDir 'env') -Recurse -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'nginx') -Destination (Join-Path $ReleaseDir 'nginx') -Recurse -Force
Copy-Item -LiteralPath (Join-Path $PSScriptRoot 'server') -Destination (Join-Path $ReleaseDir 'server') -Recurse -Force

Compress-Archive -Path (Join-Path $ReleaseDir '*') -DestinationPath $ZipPath -Force

Write-Host "Release package created:"
Write-Host "  $ZipPath"
