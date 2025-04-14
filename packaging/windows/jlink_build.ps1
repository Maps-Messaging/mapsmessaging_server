# Define JDK path
$JDKPath = "E:\java\zulu17.34.19-ca-jdk17.0.3-win_x64"
$JModsPath = Join-Path $JDKPath "jmods"

# Output runtime directory
$RuntimeDir = "runtime"

# Required modules (adjust as needed)
$Modules = "java.base,java.logging"

# Clean previous runtime if it exists
if (Test-Path $RuntimeDir) {
    Remove-Item -Recurse -Force $RuntimeDir
}

# Run jlink
jlink `
  --module-path $JModsPath `
  --add-modules $Modules `
  --output $RuntimeDir `
  --strip-debug `
  --no-header-files `
  --no-man-pages `
  --compress=2

Write-Host "Custom Java runtime created at '$RuntimeDir'"
