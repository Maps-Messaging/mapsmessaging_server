param (
  [string]$Version,
  [string]$AppName,
  [string]$RuntimeDir
)

$Modules = "java.base,java.logging,java.xml,java.management,java.naming,java.prefs,jdk.management,jdk.crypto.ec,jdk.crypto.cryptoki,java.security.sasl,jdk.security.auth,java.net.http,jdk.unsupported"

$jmodsDir = "$Env:JAVA_HOME\jmods"

$AllModules = "$Modules,java.naming"

& "$Env:JAVA_HOME\bin\jlink" `
  --output $RuntimeDir `
  --add-modules $AllModules `
  --compress=2 `
  --strip-debug `
  --no-header-files `
  --no-man-pages `
  --module-path $jmodsDir
