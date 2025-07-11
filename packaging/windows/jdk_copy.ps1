# Step 1: Get full path to java.exe
$javaPath = (Get-Command java).Source
$jdkHome = Resolve-Path (Join-Path (Split-Path $javaPath -Parent) "..")

# Step 2: Define target runtime directory
$runtimeDir = "build\runtime"

# Step 3: Clear target if needed
if (Test-Path $runtimeDir) {
    Remove-Item -Recurse -Force $runtimeDir
}

# Step 4: Copy the full JDK
Copy-Item -Recurse -Force $jdkHome $runtimeDir