param (
    [string]$Version   = "3.3.7-SNAPSHOT",
    [string]$AppName   = "MapsMessaging"
)

$BaseDir     = Get-Location
$StagingDir  = Join-Path $BaseDir "build\staging"
$RuntimeDir  = Join-Path $BaseDir "build\runtime"
$OutputDir   = Join-Path $BaseDir "out\win"

# Clean
Remove-Item -Recurse -Force $StagingDir, $RuntimeDir, $OutputDir -ErrorAction Ignore
New-Item -ItemType Directory -Force -Path $StagingDir | Out-Null

# Run jlink builder
& "$BaseDir\jlink_build.ps1" `
  -Version $Version `
  -AppName $AppName `
  -RuntimeDir $RuntimeDir

# Run jpackage script
& "$BaseDir\jpackage_script.ps1" `
  -Version $Version `
  -AppName $AppName `
  -BaseDir $BaseDir `
  -RuntimeImage $RuntimeDir `
  -OutputDir $OutputDir `
  -StagingDir $StagingDir

