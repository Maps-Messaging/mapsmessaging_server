#!/bin/bash

# Variables
export VERSION_NAME=3.3.7
export PROJECT_NAME=message_daemon

TAR_FILE="target/${PROJECT_NAME}-${VERSION_NAME}-install.tar.gz"

SOURCE_DIR="rpmbuild/SOURCES"
SPEC_DIR="rpmbuild/SPECS"

# Create source tarball
mkdir -p ${PROJECT_NAME}-${VERSION_NAME}/bin
mkdir -p ${PROJECT_NAME}-${VERSION_NAME}/etc
mkdir -p ${PROJECT_NAME}-${VERSION_NAME}/systemd

cp src/main/scripts/start.sh ${PROJECT_NAME}-${VERSION_NAME}/start.sh
cp src/main/scripts/message_daemon ${PROJECT_NAME}-${VERSION_NAME}/bin/message_daemon
cp packaging/deb_package/etc/message_daemon.env ${PROJECT_NAME}-${VERSION_NAME}/etc/message_daemon.env
cp packaging/deb_package/etc/message_daemon.service ${PROJECT_NAME}-${VERSION_NAME}/systemd/message_daemon.service

# Move the tarball to SOURCES
mkdir -p ${SOURCE_DIR}
mv ${TAR_FILE} ${SOURCE_DIR}

# Copy the SPEC file to SPECS
mkdir -p ${SPEC_DIR}
cp message_daemon.spec ${SPEC_DIR}

# Build the RPM package
rpmbuild -ba ${SPEC_DIR}/message_daemon.spec
