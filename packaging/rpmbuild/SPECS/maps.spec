#
#
#  Copyright [ 2020 - 2024 ] Matthew Buckton
#  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
#
#  Licensed under the Apache License, Version 2.0 with the Commons Clause
#  (the "License"); you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at:
#
#      http://www.apache.org/licenses/LICENSE-2.0
#      https://commonsclause.com/
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#

Name:           maps
Version:        %%VERSION%%
Release:        1%{?dist}
Summary:        A multi adapter and protocol server

License:        Apache License 2.0
URL:            https://www.mapsmessaging.io
Source0:        %%SOURCE_FILE%%

BuildArch:      noarch
Requires:       java-17-openjdk

%description
A multi adapter and protocol server for handling messaging protocols.

%prep
%setup -q -n %%SETUP_DIR%%

%build

%install
rm -rf %{buildroot}
mkdir -p %{buildroot}/opt/maps
mkdir -p %{buildroot}/etc/maps
mkdir -p %{buildroot}/usr/local/bin
mkdir -p %{buildroot}/var/log/maps
mkdir -p %{buildroot}/usr/lib/systemd/system
mkdir -p %{buildroot}/opt/maps_data

# Extract payload
tar -xzf %{SOURCE0} --strip-components=1 -C %{buildroot}/opt/maps

# Ensure executables
chmod +x %{buildroot}/opt/maps/bin/start.sh
chmod +x %{buildroot}/opt/maps/bin/maps
[ -f %{buildroot}/opt/maps/bin/generator.sh ] && chmod +x %{buildroot}/opt/maps/bin/generator.sh

# Copy/etc
cp %{buildroot}/opt/maps/etc/maps.env %{buildroot}/etc/maps/maps.env
cp %{buildroot}/opt/maps/etc/maps.service %{buildroot}/usr/lib/systemd/system/maps.service
rm -f %{buildroot}/opt/maps/lib/libLoRaChipDevice.so

# Symlinks
ln -s /opt/maps/bin/maps %{buildroot}/usr/local/bin/maps
ln -s /opt/maps/bin/start.sh %{buildroot}/usr/local/bin/start

%pre
# Ensure group/user
getent group mapsmessaging >/dev/null || groupadd -r mapsmessaging
getent passwd mapsmessaging >/dev/null || useradd -r -g mapsmessaging -d /opt/maps -s /sbin/nologin -c "Maps Messaging Server User" mapsmessaging

%post
# Ownership & perms
chown -R mapsmessaging:mapsmessaging /opt/maps
[ -d /opt/maps/bin ] && chmod -R 755 /opt/maps/bin
mkdir -p /var/log/maps
[ -f /var/log/maps/maps.log ] || { touch /var/log/maps/maps.log; }
chown -R mapsmessaging:mapsmessaging /var/log/maps
chown mapsmessaging:mapsmessaging /opt/maps_data

# Generate self-signed keystores once, as service user, into /opt/maps
if command -v keytool >/dev/null 2>&1; then
  MAPS_HOME="${MAPS_HOME:-/opt/maps}"
  if [ ! -r "${MAPS_HOME}/my-keystore.jks" ] || [ ! -r "${MAPS_HOME}/my-truststore.jks" ]; then
    umask 027
    if [ -x "${MAPS_HOME}/bin/generator.sh" ]; then
      echo "Generating keystores in ${MAPS_HOME}..."
      runuser -u mapsmessaging -- bash -lc 'cd "${MAPS_HOME:-/opt/maps}" && ./bin/generator.sh'
      chown mapsmessaging:mapsmessaging "${MAPS_HOME}/"*.jks 2>/dev/null || true
      chmod 640 "${MAPS_HOME}/"*.jks 2>/dev/null || true
    else
      echo "generator.sh not found or not executable; skipping keystore generation."
    fi
  else
    echo "Keystores already present; skipping generation."
  fi
else
  echo "keytool not found; skipping keystore generation."
fi

# Systemd
if command -v systemctl >/dev/null 2>&1; then
  systemctl daemon-reload
  systemctl enable maps.service >/dev/null 2>&1 || true
  systemctl start maps.service >/dev/null 2>&1 || true
fi

%preun
if [ $1 -eq 0 ]; then
  if command -v systemctl >/dev/null 2>&1; then
    systemctl stop maps.service >/dev/null 2>&1 || true
    systemctl disable maps.service >/dev/null 2>&1 || true
  fi
  rm -f /usr/local/bin/maps
  rm -f /usr/local/bin/start
  rm -f /etc/maps/maps.env
  rm -f /usr/lib/systemd/system/maps.service
fi

%postun
if command -v systemctl >/dev/null 2>&1; then
  systemctl daemon-reload
fi

%files
/opt/maps
/etc/maps/maps.env
/usr/local/bin/maps
/usr/local/bin/start
/usr/lib/systemd/system/maps.service
%attr(0755, mapsmessaging, mapsmessaging) /opt/maps/bin/*
%dir /var/log/maps
%config(noreplace) /etc/maps/maps.env

%changelog
* Tue Apr 14 2025 Matthew Buckton <matthew.buckton@mapsmessaging.io> %%VERSION%%-1
- Initial RPM release.
