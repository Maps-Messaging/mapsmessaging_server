Name:           message_daemon
Version:        3.3.7
Release:        1%{?dist}
Summary:        A multi adapter and protocol server

License:        Apache License 2.0
URL:            http://www.mapsmessaging.io
Source0:        %{name}-3.3.7-SNAPSHOT-install.tar.gz

BuildArch:      noarch
Requires:       java-17-openjdk

%description
A multi adapter and protocol server for handling messaging protocols.

%prep
%setup -q -n %{name}-3.3.7-SNAPSHOT

%build

%install
rm -rf $RPM_BUILD_ROOT
mkdir -p $RPM_BUILD_ROOT/opt/message_daemon
mkdir -p $RPM_BUILD_ROOT/etc/message_daemon
mkdir -p $RPM_BUILD_ROOT/usr/local/bin
mkdir -p $RPM_BUILD_ROOT/var/log/message-daemon
mkdir -p $RPM_BUILD_ROOT/usr/lib/systemd/system

# Extract the tar.gz file into the install directory
tar -xzf %{SOURCE0} --strip-components=1 -C $RPM_BUILD_ROOT/opt/message_daemon

chmod +x $RPM_BUILD_ROOT/opt/message_daemon/bin/start.sh
chmod +x $RPM_BUILD_ROOT/opt/message_daemon/bin/message_daemon

# Copy the etc files
cp $RPM_BUILD_ROOT/opt/message_daemon/etc/message_daemon.env $RPM_BUILD_ROOT/etc/message_daemon/message_daemon.env
cp $RPM_BUILD_ROOT/opt/message_daemon/etc/message_daemon.service $RPM_BUILD_ROOT/usr/lib/systemd/system/message_daemon.service
rm $RPM_BUILD_ROOT/opt/message_daemon/lib/libLoRaDevice.so

# Create symlinks
ln -s /opt/message_daemon/bin/message_daemon $RPM_BUILD_ROOT/usr/local/bin/message-daemon
ln -s /opt/message_daemon/bin/start.sh $RPM_BUILD_ROOT/usr/local/bin/start

%pre
# Check if group exists, create if it doesn't
getent group mapsmessaging >/dev/null || groupadd -r mapsmessaging

# Check if user exists, create if it doesn't
getent passwd mapsmessaging >/dev/null || useradd -r -g mapsmessaging -d /opt/message_daemon -s /sbin/nologin -c "Maps Messaging Daemon User" mapsmessaging

%post
# Set permissions
chown -R mapsmessaging:mapsmessaging /opt/message_daemon
chmod -R 755 /opt/message_daemon/bin

if [ ! -f /var/log/message-daemon/message-daemon.log ]; then
    touch /var/log/message-daemon/message-daemon.log
    chown mapsmessaging:mapsmessaging /var/log/message-daemon/message-daemon.log
fi

# Enable and start the service
systemctl enable message_daemon.service
systemctl start message_daemon.service

%preun
if [ $1 -eq 0 ]; then
    # Stop and disable the service
    systemctl stop message_daemon.service
    systemctl disable message_daemon.service

    # Remove the symlinks
    rm -f /usr/local/bin/message-daemon
    rm -f /usr/local/bin/start
    rm -f /etc/message_daemon/message_daemon.env
    rm -f /usr/lib/systemd/system/message_daemon.service
fi

%postun
# Reload systemd to pick up changes
systemctl daemon-reload

%files
/opt/message_daemon
/etc/message_daemon/message_daemon.env
/usr/local/bin/message-daemon
/usr/local/bin/start
/usr/lib/systemd/system/message_daemon.service
%attr(0755, mapsmessaging, mapsmessaging) /opt/message_daemon/bin/*
%dir /var/log/message-daemon
%config(noreplace) /etc/message_daemon/message_daemon.env

%changelog
* Tue Jun 11 2024 Matthew Buckton <matthew.buckton@mapsmessaging.io> 3.3.7-1
- Initial RPM release.
