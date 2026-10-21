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
$MainJar   = "maps-$Version.jar"

$content = (Get-Content "$InputDir\conf\logback.xml" -Raw) -replace 'MAPS_DATA', 'ProgramData'
$content | Set-Content "$InputDir\conf\logback.xml"

$IconPath = Join-Path $InputDir "www\admin\favicon.ico"
if (-not (Test-Path $IconPath)) {
  throw "Windows package icon not found at $IconPath"
}

& "$Env:JAVA_HOME\bin\jpackage" `
  --type msi `
  --icon "$IconPath" `
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
