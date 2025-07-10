param (
  [Parameter(Mandatory=$true)][string]$Version,
  [Parameter(Mandatory=$true)][string]$AppName,
  [Parameter(Mandatory=$true)][string]$BaseDir,
  [Parameter(Mandatory=$true)][string]$OutputDir,
  [Parameter(Mandatory=$true)][string]$RuntimeImage,
  [Parameter(Mandatory=$true)][string]$StagingDir
)

$InputDir    = Join-Path $StagingDir "maps-$Version"
$MainJar     = "maps-$Version.jar"
$MainClass   = "io.mapsmessaging.MessageDaemon"

$SanitizedVersion = $Version -replace '-SNAPSHOT', ''
$SourceDir = "maps-$Version"
$DestDir   = Join-Path $StagingDir "maps-$SanitizedVersion"

Move-Item -Force $SourceDir $DestDir

$InputDir  = $DestDir
$MainJar   = "maps-$Version.jar"  # Leave as original JAR name


# Copy config (if it exists)
if (Test-Path "$BaseDir\Maps.cfg") {
  Copy-Item -Force "$BaseDir\Maps.cfg" "$InputDir"
}

# Run jpackage
& "$Env:JAVA_HOME\bin\jpackage" `
  --type msi `
  --name "$AppName" `
  --app-version "$SanitizedVersion" `
  --input "$InputDir" `
  --main-jar "lib\$MainJar" `
  --main-class "$MainClass" `
  --runtime-image "$RuntimeImage" `
  --dest "$OutputDir" `
  --resource-dir "$InputDir" `
  --install-dir "MapsMessaging" `
  --win-dir-chooser `
  --win-menu `
  --win-shortcut `
  --win-console `
  --vendor "Maps Messaging" `
  --license-file "$InputDir\LICENSE" `
  --java-options "-cp App\lib\* -DMAPS_CONF=$INSTALLDIR\conf -DMAPS_DATA=$Env:APPDATA\MapsMessaging"




