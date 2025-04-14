Name:           maps-ml
Version:        %%VERSION%%
Release:        1%{?dist}
Summary:        A multi adapter and protocol server with machine learning

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
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT/opt/maps
mkdir -p $RPM_BUILD_ROOT/etc/maps
mkdir -p $RPM_BUILD_ROOT/usr/local/bin
mkdir -p $RPM_BUILD_ROOT/var/log/maps
mkdir -p $RPM_BUILD_ROOT/usr/lib/systemd/system
# Create the maps_data directory
mkdir -p $RPM_BUILD_ROOT/opt/maps_data

# Extract the tar.gz file into the install directory
tar -xzf %{SOURCE0} --strip-components=1 -C $RPM_BUILD_ROOT/opt/maps

chmod +x $RPM_BUILD_ROOT/opt/maps/bin/start.sh
chmod +x $RPM_BUILD_ROOT/opt/maps/bin/maps

# Copy the etc files
cp $RPM_BUILD_ROOT/opt/maps/etc/maps.env $RPM_BUILD_ROOT/etc/maps/maps.env
cp $RPM_BUILD_ROOT/opt/maps/etc/maps.service $RPM_BUILD_ROOT/usr/lib/systemd/system/maps.service
rm $RPM_BUILD_ROOT/opt/maps/lib/libLoRaDevice.so

# Create symlinks
ln -s /opt/maps/bin/maps $RPM_BUILD_ROOT/usr/local/bin/maps
ln -s /opt/maps/bin/start.sh $RPM_BUILD_ROOT/usr/local/bin/start

%pre
# Check if group exists, create if it doesn't
getent group mapsmessaging >/dev/null || groupadd -r mapsmessaging

# Check if user exists, create if it doesn't
getent passwd mapsmessaging >/dev/null || useradd -r -g mapsmessaging -d /opt/maps -s /sbin/nologin -c "Maps Messaging Server User" mapsmessaging

%post
# Set permissions
chown -R mapsmessaging:mapsmessaging /opt/maps
chmod -R 755 /opt/maps/bin

if [ ! -f /var/log/maps/maps.log ]; then
    touch /var/log/maps/maps.log
    chown mapsmessaging:mapsmessaging /var/log/maps/maps.log
fi


# Set ownership for maps_data directory
chown mapsmessaging:mapsmessaging /opt/maps_data


# Enable and start the service
systemctl enable maps.service
systemctl start maps.service

%preun
if [ $1 -eq 0 ]; then
    # Stop and disable the service
    systemctl stop maps.service
    systemctl disable maps.service

    # Remove the symlinks
    rm -f /usr/local/bin/maps
    rm -f /usr/local/bin/start
    rm -f /etc/maps/maps.env
    rm -f /usr/lib/systemd/system/maps.service
fi

%postun
# Reload systemd to pick up changes
systemctl daemon-reload

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
