# syntax=docker/dockerfile:1

#
#
# Copyright [ 2020 - 2024 ] [Matthew Buckton]
# Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#

#----------------------------------------------------------------------------------------------
# Base: Alpine (ARM64)
#----------------------------------------------------------------------------------------------
FROM arm64v8/alpine:3.22.1

#----------------------------------------------------------------------------------------------
# Locale + Java home + PATH
#----------------------------------------------------------------------------------------------
ENV LANG=en_US.UTF-8 \
    LANGUAGE=en_US:en \
    LC_ALL=en_US.UTF-8 \
    JAVA_HOME=/usr/lib/jvm/zulu-21 \
    PATH=/usr/lib/jvm/zulu-21/bin:$PATH

#----------------------------------------------------------------------------------------------
# Single-layer build: install Zulu JDK 21, server payload, and configure everything
#----------------------------------------------------------------------------------------------
RUN set -eux; \
    ZULU_ARCH=zulu21.44.17-ca-jdk21.0.8-linux_musl_aarch64.tar.gz; \
    ZULU_DIR="${ZULU_ARCH%.tar.gz}"; \
    INSTALL_DIR="$(dirname "$JAVA_HOME")"; \
    apk add --no-cache ca-certificates wget tar gzip coreutils dos2unix; \
    update-ca-certificates; \
    \
    # Install Zulu JDK
    wget -q "https://cdn.azul.com/zulu/bin/${ZULU_ARCH}"; \
    mkdir -p "${INSTALL_DIR}"; \
    tar -xf "${ZULU_ARCH}" -C "${INSTALL_DIR}"; \
    rm -f "${ZULU_ARCH}"; \
    mv "${INSTALL_DIR}/${ZULU_DIR}" "${JAVA_HOME}"; \
    chmod +x "${JAVA_HOME}/bin/"*; \
    cd /usr/bin; find "${JAVA_HOME}/bin" -type f -perm -a=x -exec ln -s {} . \;; \
    \
    # Install Maps Messaging
    cd /; \
    wget -q "https://github.com/Maps-Messaging/mapsmessaging_server/releases/download/%%MAPS_VERSION%%/maps-%%MAPS_VERSION%%-install.tar.gz"; \
    tar -xf "maps-%%MAPS_VERSION%%-install.tar.gz"; \
    rm "maps-%%MAPS_VERSION%%-install.tar.gz"; \
    \
    # Configure scripts and configs
    chmod +x "maps-%%MAPS_VERSION%%/bin/startDocker.sh"; \
    dos2unix "maps-%%MAPS_VERSION%%/bin/startDocker.sh"; \
    mv "maps-%%MAPS_VERSION%%/conf/logback.xml" "maps-%%MAPS_VERSION%%/conf/logback.xml_orig"; \
    mv "maps-%%MAPS_VERSION%%/conf/docker_logback.xml" "maps-%%MAPS_VERSION%%/conf/logback.xml"; \
    mv "maps-%%MAPS_VERSION%%/conf/NetworkManager.yaml" "maps-%%MAPS_VERSION%%/conf/NetworkManager.yaml_orig"; \
    mv "maps-%%MAPS_VERSION%%/conf/NetworkManagerDocker.yaml" "maps-%%MAPS_VERSION%%/conf/NetworkManager.yaml"; \
    \
    # Static files + data dir
    mkdir -p /data; \
    \
    # Runtime user
    addgroup -S messaginggroup; \
    adduser -S messaginguser -G messaginggroup -s /bin/sh -h "/maps-%%MAPS_VERSION%%"; \
    chown -R messaginguser:messaginggroup /data "/maps-%%MAPS_VERSION%%"; \
    chmod -R 755 "/maps-%%MAPS_VERSION%%"

#----------------------------------------------------------------------------------------------
# Ports
#----------------------------------------------------------------------------------------------
EXPOSE 9000/tcp 8080/tcp 8778/tcp 4222/tcp  1883/tcp \
       1884/udp 5683/udp 2442/udp
#----------------------------------------------------------------------------------------------
# Data volume
#----------------------------------------------------------------------------------------------
VOLUME /data

#----------------------------------------------------------------------------------------------
# Run as non-root from install dir
#----------------------------------------------------------------------------------------------
USER messaginguser
WORKDIR /maps-%%MAPS_VERSION%%

# Increase file descriptors and start
CMD sh -lc 'ulimit -n 100000 && ./bin/startDocker.sh'
