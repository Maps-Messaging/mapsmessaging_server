# Maps Messaging -- Debian/Ubuntu APT Setup

## 1) Prerequisites

-   Debian/Ubuntu with `apt` (HTTPS supported by default).

-   `curl` and `gpg` installed:

    ``` bash
    sudo apt-get update && sudo apt-get install -y curl gpg
    ```

## 2) Choose a channel (Release vs Snapshot)

### Option A --- Release (recommended)

Create the source list and import the signing key:

``` bash
echo 'deb [arch=all] https://repository.mapsmessaging.io/repository/maps_apt_release/ stable main' | sudo tee /etc/apt/sources.list.d/mapsmessaging-release.list

sudo curl -fsSL https://repository.mapsmessaging.io/repository/public_key/daily/apt_daily_key.gpg | sudo gpg --dearmor -o /etc/apt/trusted.gpg.d/mapsmessaging-apt.gpg

sudo apt-get update
```

### Option B --- Snapshot (daily builds)

Create the source list and reuse the same signing key:

``` bash
echo 'deb [arch=all] https://repository.mapsmessaging.io/repository/maps_apt_daily/ development main' | sudo tee /etc/apt/sources.list.d/mapsmessaging-daily.list

# Key is already installed (mapsmessaging-apt.gpg)
sudo apt-get update
```

## 3) (Optional) Add **both** channels with pinning (prefer Release)

If you want Release by default but keep Snapshot available for explicit
installs/upgrades:

``` bash
# Add BOTH lists (key already installed), then set pinning:
sudo tee /etc/apt/preferences.d/mapsmessaging <<'EOF'
Package: maps
Pin: release o=mapsmessaging-release
Pin-Priority: 900

Package: maps-ml
Pin: release o=mapsmessaging-release
Pin-Priority: 900

Package: *
Pin: release o=mapsmessaging-daily
Pin-Priority: 400
EOF
```

> Notes:\
> - `o=` (Origin) is derived from repo metadata; if apt doesn't match,
    > use `apt-cache policy` to see the exact `o=` value and adjust.\
> - With these priorities, Release is preferred; Snapshot can still be
    > installed explicitly (see below).

## 4) Install Maps Messaging

Standard edition:

``` bash
sudo apt-get update
sudo apt-get install maps
```

ML-enabled edition:

``` bash
sudo apt-get update
sudo apt-get install maps-ml
```

If both channels are configured and you need **Snapshot** specifically:

``` bash
sudo apt-get install -t development maps
# or
sudo apt-get install -t development maps-ml
```

## 5) Service management (systemd)

``` bash
# Standard
sudo systemctl enable --now maps

# ML
sudo systemctl enable --now maps-ml

# Check status/logs
systemctl status maps
journalctl -u maps -n 200 --no-pager
```

## 6) Upgrades

-   Regular upgrades (per your channel/pinning):

    ``` bash
    sudo apt-get update
    sudo apt-get upgrade
    ```

-   Force upgrade from Release to Snapshot (one-off):

    ``` bash
    sudo apt-get install -t development maps
    ```

## 7) Switching channels later

-   **Release â†’ Snapshot**: add snapshot list, then `apt-get update` and
    install with `-t development`.

-   **Snapshot â†’ Release**: remove snapshot list (or lower its
    priority), ensure release list exists, `apt-get update`, then:

    ``` bash
    sudo apt-get install maps/stable
    # or maps-ml/stable (use the release pocket name shown by `apt-cache policy`)
    ```

## 8) Verify repository & package selection

``` bash
apt-cache policy maps
apt-cache policy maps-ml
```

This shows the versions, priorities, and which repo apt will choose.

## 9) Uninstall

``` bash
sudo systemctl disable --now maps || true
sudo systemctl disable --now maps-ml || true
sudo apt-get remove maps maps-ml
# Optional: purge config
sudo apt-get purge maps maps-ml
```

## 10) Troubleshooting

-   **GPG/Signature issues**: re-import the correct key, confirm the
    `.gpg` file exists in `/etc/apt/trusted.gpg.d/`, then:

    ``` bash
    sudo apt-get clean
    sudo apt-get update -o Acquire::https::Verify-Peer=true
    ```

-   **Pinning doesn't apply**: check exact origins/labels:

    ``` bash
    apt-cache policy | sed -n '/maps_apt_/,+5p'
    ```

    Adjust `o=` in `/etc/apt/preferences.d/mapsmessaging` accordingly.

-   **Behind a proxy**: set `Acquire::http::Proxy` /
    `Acquire::https::Proxy` in `/etc/apt/apt.conf.d/01proxy`.

------------------------------------------------------------------------

### Packages

-   `maps`: Maps Messaging Server (standard).
-   `maps-ml`: Maps Messaging Server with ML features included.
