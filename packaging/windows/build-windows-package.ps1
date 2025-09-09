param (
    [string]$Version,
    [string]$AppName
)

$BaseDir     = Get-Location
$StagingDir  = Join-Path $BaseDir "build\staging"
$RuntimeDir  = Join-Path $BaseDir "build\runtime"
$OutputDir   = Join-Path $BaseDir "out\win"

# Clean
Remove-Item -Recurse -Force $StagingDir, $RuntimeDir, $OutputDir -ErrorAction Ignore
New-Item -ItemType Directory -Force -Path $StagingDir | Out-Null

# Run jlink builder
& "$BaseDir\jdk_copy.ps1"

# Run jpackage script
& "$BaseDir\jpackage_script.ps1" `
  -Version $Version `
  -AppName $AppName `
  -BaseDir $BaseDir `
  -RuntimeImage $RuntimeDir `
  -OutputDir $OutputDir `
  -StagingDir $StagingDir

