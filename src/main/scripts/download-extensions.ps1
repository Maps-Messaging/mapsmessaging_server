Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RepoBase = "https://repository.mapsmessaging.io/repository/maps_snapshots"

function Get-LatestSnapshotJarValue {
    param(
        [Parameter(Mandatory=$true)][string]$MetadataXml,
        [string]$Classifier = $null
    )

    [xml]$xml = $MetadataXml
    $snapshotVersions = $xml.metadata.versioning.snapshotVersions.snapshotVersion
    if (-not $snapshotVersions) {
        throw "No snapshotVersions found in maven-metadata.xml"
    }

    if ([string]::IsNullOrWhiteSpace($Classifier)) {
        $match = $snapshotVersions | Where-Object {
            $_.extension -eq "jar" -and (-not $_.classifier -or $_.classifier -eq "")
        } | Select-Object -First 1
    } else {
        $match = $snapshotVersions | Where-Object {
            $_.extension -eq "jar" -and $_.classifier -eq $Classifier
        } | Select-Object -First 1
    }

    if (-not $match) {
        throw "No matching jar snapshotVersion found (classifier='$Classifier')"
    }

    return $match.value
}

function Download-LatestSnapshotJarAndRename {
    param(
        [Parameter(Mandatory=$true)][string]$GroupId,
        [Parameter(Mandatory=$true)][string]$ArtifactId,
        [Parameter(Mandatory=$true)][string]$Version,     # e.g. 1.0.0-SNAPSHOT
        [string]$Classifier = $null
    )

    $groupPath = $GroupId -replace "\.", "/"
    $artifactDir = "$RepoBase/$groupPath/$ArtifactId/$Version"
    $metadataUrl = "$artifactDir/maven-metadata.xml"

    Write-Host "==> $GroupId`:$ArtifactId`:$Version$([string]::IsNullOrWhiteSpace($Classifier) ? "" : ":$Classifier")"
    Write-Host "    metadata: $metadataUrl"

    $metadataXml = (Invoke-WebRequest -Uri $metadataUrl -UseBasicParsing).Content
    $jarValue = Get-LatestSnapshotJarValue -MetadataXml $metadataXml -Classifier $Classifier

    $timestampedFile = "$ArtifactId-$jarValue.jar"
    $jarUrl = "$artifactDir/$timestampedFile"

    $finalFile = "$ArtifactId-$Version.jar"   # <-- the name you want

    Write-Host "    download: $jarUrl"
    Invoke-WebRequest -Uri $jarUrl -OutFile $timestampedFile -UseBasicParsing

    if (Test-Path $finalFile) { Remove-Item -Force $finalFile }
    Rename-Item -Path $timestampedFile -NewName $finalFile

    Write-Host "    saved as: $finalFile"
    Write-Host ""
}


# ---- Examples (fill in the exact artifactIds/versions you use) ----
Download-LatestSnapshotJarAndRename -GroupId "io.mapsmessaging" -ArtifactId "aws-sns-extension" -Version "1.0.0-SNAPSHOT"
Download-LatestSnapshotJarAndRename -GroupId "io.mapsmessaging" -ArtifactId "ibm-mq-extension" -Version "1.0.0-SNAPSHOT"
Download-LatestSnapshotJarAndRename -GroupId "io.mapsmessaging" -ArtifactId "pulsar-extension" -Version "1.0.0-SNAPSHOT"
Download-LatestSnapshotJarAndRename -GroupId "io.mapsmessaging" -ArtifactId "v2x-step-extension" -Version "1.0.0-SNAPSHOT"
