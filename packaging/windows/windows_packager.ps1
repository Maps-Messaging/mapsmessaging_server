param (
    [string]$Version = "3.3.7-SNAPSHOT",
    [string]$AppName = "MapsMessaging",
    [string]$ZipRepo = "maps-snapshot",
    [string]$PushRepo = "maps-windows",
    [string]$Username = $env:NEXUS_USER,
    [string]$Password = $env:NEXUS_PASSWORD
)

$ZipName     = "maps-$Version-install.zip"
$ZipUrl      = "https://github.com/Maps-Messaging/mapsmessaging_server/releases/download/$Version/$ZipName"
$PushUrl     = "https://repository.mapsmessaging.io/service/rest/v1/components?repository=$PushRepo"

# Download the ZIP (anonymous or basic auth)
Invoke-WebRequest -Uri $ZipUrl -OutFile $ZipName -UseBasicParsing

# Unzip
$InputDir = "maps-$Version"

Expand-Archive -Path $ZipName -DestinationPath "." -Force

# Build the installer
$BaseDir = Join-Path (Get-Location) $InputDir
.\build-windows-package.ps1 -Version $Version -AppName $AppName -BaseDir $BaseDir

# Find the built installer
$InstallerPath = Get-ChildItem -Path "out\win" -Filter "$AppName*.exe" | Select-Object -First 1

# Upload installer to Nexus
if ($InstallerPath) {
    $boundary = [System.Guid]::NewGuid().ToString()
    $fileBytes = [System.IO.File]::ReadAllBytes($InstallerPath.FullName)
    $encodedCreds = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$Username`:$Password"))

    $bodyLines = @(
        "--$boundary",
        "Content-Disposition: form-data; name=`"raw.asset1`"; filename=`"$($InstallerPath.Name)`"",
        "Content-Type: application/octet-stream",
        "",
        [System.Text.Encoding]::ASCII.GetString($fileBytes),
        "--$boundary--"
    )

    $body = [System.Text.Encoding]::ASCII.GetBytes(($bodyLines -join "`r`n"))

    Invoke-WebRequest -Uri $PushUrl `
                    -Method Post `
                    -Headers @{ Authorization = "Basic $encodedCreds"; "Content-Type" = "multipart/form-data; boundary=$boundary" } `
                    -Body $body

    Write-Host "✅ Uploaded $($InstallerPath.Name) to Nexus repo $PushRepo"
} else {
    Write-Error "❌ Installer not found."
}
