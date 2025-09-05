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

$SanitizedVersion = $Version -replace '', ''
$SourceDir = "maps-$Version"
$DestDir   = Join-Path $StagingDir "maps-$SanitizedVersion"

Move-Item -Force $SourceDir $DestDir

$InputDir  = $DestDir
$MainJar   = "maps-$Version.jar"  # Leave as original JAR name


#
# Move the windows version of logback to override the linux version
#
$content = (Get-Content "$InputDir\conf\logback.xml" -Raw) -replace 'MAPS_DATA', 'ProgramData'
$content | Set-Content "$InputDir\conf\logback.xml"


# Run jpackage
& "$Env:JAVA_HOME\bin\jpackage" `
  --type msi `
  --icon "build\staging\maps-4.0.1\www\admin\favicon.ico" `
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
  --add-launcher mapsTop=mapsTop.properties `
  --license-file "$InputDir\LICENSE" `
  --java-options '-DMAPS_HOME="$APPDIR" -DMAPS_CONF="$APPDIR\conf" -DMAPS_DATA="${ProgramData}\MapsMessaging\data" -DCONSUL_URL=http://localhost:8500/'







