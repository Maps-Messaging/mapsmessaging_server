# syntax=docker/dockerfile:1

#
# Copyright [ 2020 - 2024 ] [Matthew Buckton]
# Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
#
# Licensed under the Apache License, Version 2.0 (the "License");
# http://www.apache.org/licenses/LICENSE-2.0
#

FROM alpine:3.22.1

ENV LANG=en_US.UTF-8 \
    LANGUAGE=en_US:en \
    LC_ALL=en_US.UTF-8 \
    JAVA_HOME=/usr/lib/jvm/zulu-21 \
    PATH=/usr/lib/jvm/zulu-21/bin:$PATH

# Install required tools and JDK
RUN apk add --no-cache dos2unix shadow wget && \
    ZULU_ARCH=zulu21.44.17-ca-jdk21.0.8-linux_musl_x64.tar.gz && \
    INSTALL_DIR=$(dirname "$JAVA_HOME") && \
    ZULU_DIR=$(basename ${ZULU_ARCH} .tar.gz) && \
    wget -q https://cdn.azul.com/zulu/bin/${ZULU_ARCH} && \
    mkdir -p ${INSTALL_DIR} && \
    tar -xf ${ZULU_ARCH} -C ${INSTALL_DIR} && \
    rm -f ${ZULU_ARCH} && \
    mv ${INSTALL_DIR}/${ZULU_DIR} ${JAVA_HOME} && \
    find ${JAVA_HOME}/bin -type f -perm -a=x -exec ln -s {} /usr/bin/ \; && \
\
# Download and extract the messaging server
    wget -q https://github.com/Maps-Messaging/mapsmessaging_server/releases/download/%%MAPS_VERSION%%/maps-%%MAPS_VERSION%%-install.tar.gz && \
    tar -xf maps-%%MAPS_VERSION%%-install.tar.gz && \
    rm maps-%%MAPS_VERSION%%-install.tar.gz && \
\
# Setup startup script
    chmod +x maps-%%MAPS_VERSION%%/bin/startDocker.sh && \
    dos2unix maps-%%MAPS_VERSION%%/bin/startDocker.sh && \
\
# Configure logging and network
    mv maps-%%MAPS_VERSION%%/conf/logback.xml maps-%%MAPS_VERSION%%/conf/logback.xml_orig && \
    mv maps-%%MAPS_VERSION%%/conf/docker_logback.xml maps-%%MAPS_VERSION%%/conf/logback.xml && \
    mv maps-%%MAPS_VERSION%%/conf/NetworkManager.yaml maps-%%MAPS_VERSION%%/conf/NetworkManager.yaml_orig && \
    mv maps-%%MAPS_VERSION%%/conf/NetworkManagerDocker.yaml maps-%%MAPS_VERSION%%/conf/NetworkManager.yaml && \
\
# Setup data volume and static files
    mkdir -p /opt/maps_data && \
    mv /maps-%%MAPS_VERSION%% /opt/maps  && \
\
# Add user and permissions
    addgroup -S messaginggroup && \
    adduser -S messaginguser -G messaginggroup -s /bin/sh -h /opt/maps && \
    chown -R messaginguser:messaginggroup /opt/maps_data /opt/maps && \
    chmod -R 755 /opt/maps

EXPOSE 9000/tcp 8080/tcp 8778/tcp 4222/tcp  1883/tcp \
       1884/udp 5683/udp 2442/udp

VOLUME /opt/maps_data

CMD su -c "ulimit -n 100000 && /opt/maps/bin/startDocker.sh" messaginguser