$ErrorActionPreference = 'Stop'

$Username = $args[0]
$Password = $args[1]
$Version = $args[2]
$AppName = "MapsMessaging"
$PushRepo = "maps_windows_installer"
$RepoRoot = Resolve-Path (Join-Path $PSScriptRoot "..\..")

if ([string]::IsNullOrWhiteSpace($Version)) {
    if ($env:BUILDKITE_BRANCH -eq "development") {
        $PomPath = Join-Path $RepoRoot "pom.xml"
        [xml]$Pom = Get-Content -Path $PomPath -Raw
        $NamespaceManager = New-Object System.Xml.XmlNamespaceManager($Pom.NameTable)
        $NamespaceManager.AddNamespace("m", "http://maven.apache.org/POM/4.0.0")
        $VersionNode = $Pom.SelectSingleNode("/m:project/m:version", $NamespaceManager)
        if ($null -eq $VersionNode -or [string]::IsNullOrWhiteSpace($VersionNode.InnerText)) {
            throw "Unable to determine project version from $PomPath"
        }
        $Version = $VersionNode.InnerText.Trim()
        if (-not $Version.EndsWith("-SNAPSHOT")) {
            throw "Development branch must package a snapshot version, found '$Version'"
        }
        Write-Host "Development build: packaging snapshot $Version"
    }
    else {
        $MetadataUrl = "https://repository.mapsmessaging.io/repository/maps_releases/io/mapsmessaging/maps/maven-metadata.xml"
        $EncodedCreds = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$Username`:$Password"))
        [xml]$Metadata = (Invoke-WebRequest -Uri $MetadataUrl -Headers @{ Authorization = "Basic $EncodedCreds" } -UseBasicParsing).Content

        $ReleaseNode = $Metadata.SelectSingleNode("/metadata/versioning/release")
        if ($null -eq $ReleaseNode -or [string]::IsNullOrWhiteSpace($ReleaseNode.InnerText)) {
            $ReleaseNode = $Metadata.SelectSingleNode("/metadata/versioning/latest")
        }
        if ($null -eq $ReleaseNode -or [string]::IsNullOrWhiteSpace($ReleaseNode.InnerText)) {
            throw "Unable to determine latest MAPS release from Maven metadata at $MetadataUrl"
        }
        $Version = $ReleaseNode.InnerText.Trim()
        Write-Host "Non-development build: packaging latest Maven release $Version"
    }
}
else {
    Write-Host "Packaging explicitly requested version $Version"
}

$ZipName = "maps-$Version-install.zip"
$ZipUrl = "https://github.com/Maps-Messaging/mapsmessaging_server/releases/download/$Version/$ZipName"
$PushUrl = "https://repository.mapsmessaging.io/service/rest/v1/components?repository=$PushRepo"

# Keep extraction and staging paths short even under a deep Buildkite checkout.
# Each invocation owns its directory so parallel agents and retries cannot collide.
$WorkRoot = Join-Path $env:SystemDrive "b"
New-Item -ItemType Directory -Path $WorkRoot -Force | Out-Null
$WorkDir = Join-Path $WorkRoot ([Guid]::NewGuid().ToString("N"))
New-Item -ItemType Directory -Path $WorkDir | Out-Null
$LocationPushed = $false

try {
    foreach ($file in @("build-windows-package.ps1", "jdk_copy.ps1", "jpackage_script.ps1", "mapsTop.properties")) {
        Copy-Item -LiteralPath (Join-Path $PSScriptRoot $file) -Destination $WorkDir
    }
    Push-Location -LiteralPath $WorkDir
    $LocationPushed = $true
    Write-Host "Windows packaging workspace: $WorkDir"

    Write-Host "Downloading $ZipUrl"
    Invoke-WebRequest -Uri $ZipUrl -OutFile $ZipName -UseBasicParsing

    $InputDir = "maps-$Version"
    Expand-Archive -Path $ZipName -DestinationPath "." -Force

    .\build-windows-package.ps1 -Version $Version -AppName $AppName
    if ($LASTEXITCODE -ne 0) {
        throw "Windows packaging failed with exit code $LASTEXITCODE"
    }

    $InstallerPath = Get-ChildItem -Path "out\win" -Filter "$AppName*.msi" | Select-Object -First 1

    if ($InstallerPath) {
        # Retain the existing artifact location for Buildkite and local callers.
        $ArtifactDir = Join-Path $PSScriptRoot "out\win"
        New-Item -ItemType Directory -Path $ArtifactDir -Force | Out-Null
        Copy-Item -LiteralPath $InstallerPath.FullName -Destination $ArtifactDir -Force

        $encodedCreds = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("$Username`:$Password"))
        $filename = $InstallerPath.Name
        $directory = "windows/$Version"

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

} finally {
    if ($LocationPushed) {
        Pop-Location
    }
    try {
        Remove-Item -LiteralPath $WorkDir -Recurse -Force -ErrorAction Stop
    } catch {
        Write-Warning "Could not remove packaging workspace '$WorkDir': $_"
    }
}
