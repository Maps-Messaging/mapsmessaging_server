$Username = $args[0]
$Password = $args[1]
$Version = "4.1.0-SNAPSHOT"
$AppName = "MapsMessaging"
$ZipRepo = "maps-snapshot"
$PushRepo = "maps_windows_installer"



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
$InstallerPath = Get-ChildItem -Path "out\win" -Filter "$AppName*.msi" | Select-Object -First 1

if ($InstallerPath) {
    $encodedCreds = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$Username`:$Password"))
    $filename = $InstallerPath.Name
    $directory = "windows/$Version"

    # DELETE existing component from Nexus
    $searchUrl = "https://repository.mapsmessaging.io/service/rest/v1/search?repository=$PushRepo&name=$($InstallerPath.BaseName)&version=$Version"
    try {
        $response = Invoke-RestMethod -Uri $searchUrl -Headers @{ Authorization = "Basic $encodedCreds" } -Method Get
        if ($response.items.Count -gt 0) {
            $componentId = $response.items[0].id
            $deleteUrl = "https://repository.mapsmessaging.io/service/rest/v1/components/$componentId"
            Invoke-RestMethod -Uri $deleteUrl -Headers @{ Authorization = "Basic $encodedCreds" } -Method Delete
            Write-Host "Deleted existing component ID: $componentId"
        } else {
            Write-Host "No existing package found to delete."
        }
    } catch {
        Write-Warning "Failed to query/delete existing component: $_"
    }

    # UPLOAD the new installer to Nexus
    $boundary = [System.Guid]::NewGuid().ToString()
    $fileBytes = [System.IO.File]::ReadAllBytes($InstallerPath.FullName)
    $crlf = "`r`n"

    $stream = New-Object System.IO.MemoryStream
    $writer = New-Object System.IO.StreamWriter($stream, [System.Text.Encoding]::ASCII)

    $writer.Write("--$boundary$crlf")
    $writer.Write("Content-Disposition: form-data; name=`"raw.directory`"$crlf$crlf")
    $writer.Write("$directory$crlf")

    $writer.Write("--$boundary$crlf")
    $writer.Write("Content-Disposition: form-data; name=`"raw.asset1`"; filename=`"$filename`"$crlf")
    $writer.Write("Content-Type: application/octet-stream$crlf$crlf")
    $writer.Flush()

    $stream.Write($fileBytes, 0, $fileBytes.Length)

    $writer = New-Object System.IO.StreamWriter($stream, [System.Text.Encoding]::ASCII, 1024, $true)
    $writer.Write("$crlf--$boundary$crlf")
    $writer.Write("Content-Disposition: form-data; name=`"raw.asset1.filename`"$crlf$crlf")
    $writer.Write("$filename$crlf")
    $writer.Write("--$boundary--$crlf")
    $writer.Flush()

    $body = $stream.ToArray()

    Invoke-WebRequest -Uri $PushUrl `
                      -Method Post `
                      -Headers @{ Authorization = "Basic $encodedCreds"; "Content-Type" = "multipart/form-data; boundary=$boundary" } `
                      -Body $body

    Write-Host "Uploaded $filename to Nexus repo $PushRepo"
} else {
    Write-Error "Installer not found."
}