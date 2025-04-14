$Version = "3.3.7-SNAPSHOT"
$AppName = "MapsMessaging"
$InputDir = "maps-$Version"
$MainJar = "maps-$Version.jar"
$MainClass = "io.mapsmessaging.MessageDaemon"
$OutputDir = "out\win"
$RuntimeImage = "runtime"  # Use jlink or prebuilt

jpackage `
  --type exe `
  --name $AppName `
  --app-version $Version `
  --input "$InputDir\lib" `
  --dest "$OutputDir" `
  --main-jar $MainJar `
  --main-class $MainClass `
  --runtime-image "$RuntimeImage" `
  --install-dir "C:\Program Files\$AppName" `
  --resource-dir "$InputDir" `
  --win-menu `
  --win-shortcut `
  --java-options "-DMAPS_HOME=C:\Program Files\$AppName -DMAPS_CONF=C:\Program Files\$AppName\conf" `
  --license-file "$InputDir\LICENSE"
