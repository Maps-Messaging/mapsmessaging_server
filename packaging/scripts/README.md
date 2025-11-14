# PostHog Installation Tracking

This directory contains scripts for tracking MAPS messaging installations using PostHog analytics.

## Overview

When users install MAPS messaging using the released packages (DEB, RPM, Windows MSI, macOS PKG), the installation process will automatically send an anonymous tracking event to PostHog to record the installation.

## Files

- `track_installation.sh` - Bash script for tracking installations on Linux/Unix systems
- `track_installation.ps1` - PowerShell script for tracking installations on Windows
- `posthog_config.sh` - Configuration file for PostHog settings
- `README.md` - This documentation file

## Configuration

### Environment Variables

Set these environment variables when building packages to enable PostHog tracking:

```bash
export POSTHOG_API_KEY="phc_your_api_key_here"
export POSTHOG_PROJECT_ID="your_project_id_here"  # Optional
```

### Disabling Tracking

To disable PostHog tracking, set:

```bash
export POSTHOG_TRACKING_ENABLED="false"
```

Tracking is automatically disabled in CI/CD environments (when `CI`, `BUILDKITE`, or `GITHUB_ACTIONS` environment variables are set).

## Event Data

The tracking event includes the following information:

- `package_type`: Type of package (deb, rpm, msi, pkg)
- `version`: Version of MAPS messaging being installed
- `distro`: Operating system distribution (for Linux packages)
- `arch`: System architecture
- `hostname`: System hostname (anonymized)
- `os`: Operating system name
- `os_version`: Operating system version
- `installation_timestamp`: When the installation occurred

## Privacy

- All tracking is anonymous - no personally identifiable information is collected
- The installation ID is generated from the hostname and timestamp, making it unique but not personally identifiable
- Tracking can be disabled by setting the appropriate environment variable
- The tracking script runs in the background and will not block the installation process

## Integration

The tracking scripts are automatically integrated into the package installation process:

- **DEB packages**: Called from the `postinst` script
- **RPM packages**: Called from the `%post` section of the spec file
- **Windows MSI**: Would need to be integrated into the MSI installer
- **macOS PKG**: Would need to be integrated into the PKG installer

## Manual Usage

You can also run the tracking script manually:

```bash
# Linux/Unix
./track_installation.sh deb 4.1.0 ubuntu x86_64

# Windows PowerShell
./track_installation.ps1 -PackageType "msi" -Version "4.1.0" -Distro "windows" -Arch "x64"
```

## Troubleshooting

- If tracking fails, it will not affect the installation process
- Check the system logs for any error messages from the tracking script
- Ensure the system has internet connectivity to reach PostHog
- Verify that curl (Linux/Unix) or PowerShell (Windows) is available
