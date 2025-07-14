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
$items = Get-ChildItem -Path $jdkHome -Recurse -Force
$total = $items.Count
$count = 0

foreach ($item in $items) {
    $dest = $item.FullName.Replace($jdkHome, $runtimeDir)
    Write-Progress -Activity "Copying JDK" -Status $item.FullName -PercentComplete (($count / $total) * 100)

    if ($item.PSIsContainer) {
        New-Item -ItemType Directory -Path $dest -Force | Out-Null
    } else {
        Copy-Item -Path $item.FullName -Destination $dest -Force
    }

    $count++
}