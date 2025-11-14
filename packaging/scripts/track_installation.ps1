# PostHog installation tracking script for Windows
# Usage: track_installation.ps1 <package_type> <version> [distro] [arch]

param(
    [string]$PackageType = "unknown",
    [string]$Version = "unknown",
    [string]$Distro = "windows",
    [string]$Arch = "unknown"
)

# Configuration
$PostHogApiUrl = if ($env:POSTHOG_API_URL) { $env:POSTHOG_API_URL } else { "https://eu.i.posthog.com/capture/" }
$PostHogApiKey = $env:POSTHOG_API_KEY
$PostHogProjectId = if ($env:POSTHOG_PROJECT_ID) { $env:POSTHOG_PROJECT_ID } else { "45683" }

# Generate a unique installation ID (based on hostname + timestamp)
$Hostname = $env:COMPUTERNAME
$Timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$InstallationId = [System.Security.Cryptography.SHA256]::Create().ComputeHash([System.Text.Encoding]::UTF8.GetBytes("${Hostname}-${Timestamp}")) | ForEach-Object { $_.ToString("x2") } | Join-String

# Get OS information
$OsInfo = Get-WmiObject -Class Win32_OperatingSystem
$OsName = $OsInfo.Caption
$OsVersion = $OsInfo.Version

# Create the JSON payload for PostHog
$Payload = @{
    api_key = $PostHogApiKey
    event = "maps_installation"
    distinct_id = $InstallationId
    timestamp = [DateTimeOffset]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
    properties = @{
        package_type = $PackageType
        version = $Version
        distro = $Distro
        arch = $Arch
        project_id = $PostHogProjectId
        hostname = $Hostname
        os = "Windows"
        os_version = $OsVersion
        os_name = $OsName
        installation_timestamp = [DateTimeOffset]::UtcNow.ToString("yyyy-MM-ddTHH:mm:ss.fffZ")
        '$lib' = "maps-packaging"
        '$lib_version' = $Version
    }
} | ConvertTo-Json -Depth 3

# Function to send the event to PostHog
function Send-ToPostHog {
    if ([string]::IsNullOrEmpty($PostHogApiKey)) {
        Write-Host "PostHog API key not configured, skipping installation tracking"
        return
    }

    try {
        # Send the event (non-blocking)
        $WebRequest = [System.Net.WebRequest]::Create($PostHogApiUrl)
        $WebRequest.Method = "POST"
        $WebRequest.ContentType = "application/json"
        $WebRequest.Timeout = 10000  # 10 seconds
        
        $RequestBody = [System.Text.Encoding]::UTF8.GetBytes($Payload)
        $WebRequest.ContentLength = $RequestBody.Length
        
        # Write the request body asynchronously
        $RequestStream = $WebRequest.GetRequestStream()
        $RequestStream.Write($RequestBody, 0, $RequestBody.Length)
        $RequestStream.Close()
        
        # Get the response asynchronously
        $Response = $WebRequest.GetResponse()
        $Response.Close()
        
        Write-Host "Installation tracking event sent to PostHog"
    }
    catch {
        Write-Host "Failed to send installation tracking event: $($_.Exception.Message)"
    }
}

# Only run if not in a CI/CD environment or if explicitly enabled
if ($env:CI -eq "true" -or $env:BUILDKITE -eq "true" -or $env:GITHUB_ACTIONS -eq "true") {
    Write-Host "Running in CI/CD environment, skipping installation tracking"
    exit 0
}

# Send the tracking event
Send-ToPostHog
