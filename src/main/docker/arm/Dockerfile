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
# Get Alpine linux image and add the Zulu JDK 13, as per
# https://github.com/zulu-openjdk/zulu-openjdk/blob/master/alpine/13.0.1-13.28/Dockerfile
#----------------------------------------------------------------------------------------------
FROM arm64v8/alpine:latest

#----------------------------------------------------------------------------------------------
# setup env variables
#----------------------------------------------------------------------------------------------
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
ENV JAVA_HOME=/usr/lib/jvm/zulu-17

#----------------------------------------------------------------------------------------------
# Setup the repos
#----------------------------------------------------------------------------------------------
ENV ZULU_ARCH=zulu17.28.13-ca-jdk17.0.0-linux_musl_aarch64.tar.gz

RUN wget -q https://cdn.azul.com/zulu/bin/${ZULU_ARCH}

RUN export INSTALL_DIR=$(dirname $JAVA_HOME) && \
    mkdir -p ${INSTALL_DIR}

RUN export ZULU_DIR=$(basename ${ZULU_ARCH} .tar.gz) && \
    export INSTALL_DIR=$(dirname $JAVA_HOME) && \
    tar -xf ./${ZULU_ARCH} -C ${INSTALL_DIR} && rm -f ${ZULU_ARCH} && \
    mv ${INSTALL_DIR}/${ZULU_DIR} ${JAVA_HOME}


RUN ZULU_ARCH=zulu16.32.15-ca-jdk16.0.2-linux_musl_aarch64.tar.gz && \
    INSTALL_DIR=$( dirname $JAVA_HOME ) && \
    MAN_DIR=/usr/share/man/man1 && \
    ZULU_DIR=$( basename ${ZULU_ARCH} .tar.gz ) && \
    wget -q https://cdn.azul.com/zulu/bin/${ZULU_ARCH} && \
    mkdir -p ${INSTALL_DIR} && \
    tar -xf ./${ZULU_ARCH} -C ${INSTALL_DIR} && rm -f ${ZULU_ARCH} && \
    mv ${INSTALL_DIR}/${ZULU_DIR} ${JAVA_HOME}

# Setup the bin directory
RUN  chmod +x ${JAVA_HOME}/bin/* && \
     BIN_DIR=/usr/bin && \
     chmod +x ${JAVA_HOME}/bin/* && \
     cd ${BIN_DIR} && find ${JAVA_HOME}/bin -type f -perm -a=x -exec ln -s {} . \;

#----------------------------------------------------------------------------------------------
# We now have a JDK and Linux image. Now to
#   a) Expose all network ports the server may need
#   b) Install the latest image from deployments
#   c) Configure the scripts specific for docker
#   d) Configure for JMX / Rest / Web management
#   e) Define a volume mount for the data directory
#   f) Register the command to start the server
# Expose the protocol ports
# -------------------------
#
# ALL         9000        TCP - Includes MQTT, AMQP and Stomp and WebSockets
# Admin       8080        TCP - Native RestAPI
# Admin       8778        TCP - Jolokia
#
# SemTech     1700        UDP
# MQTT-SN     1884        UDP
# CoAP        5683        UDP
# MQTT-SN V2  2442        UDP
#----------------------------------------------------------------------------------------------
EXPOSE 9000/tcp
EXPOSE 8080/tcp
EXPOSE 8778/tcp

EXPOSE 1700/udp
EXPOSE 1884/udp
EXPOSE 5683/udp
EXPOSE 2442/udp

#----------------------------------------------------------------------------------------------
# Install the maps messaging daemon image
#----------------------------------------------------------------------------------------------
RUN wget -q https://github.com/Maps-Messaging/mapsmessaging_server/releases/download/%%MAPS_VERSION%%/maps-%%MAPS_VERSION%%-install.tar.gz && \
    tar -xf maps-%%MAPS_VERSION%%-install.tar.gz  && \
    rm  maps-%%MAPS_VERSION%%-install.tar.gz

#----------------------------------------------------------------------------------------------
# Now configure the scripts
#----------------------------------------------------------------------------------------------
RUN chmod +x maps-%%MAPS_VERSION%%/bin/startDocker.sh && \
    dos2unix maps-%%MAPS_VERSION%%/bin/startDocker.sh

#Set the docker log configuration
RUN mv maps-%%MAPS_VERSION%%/conf/logback.xml maps-%%MAPS_VERSION%%/conf/logback.xml_orig && \
    mv maps-%%MAPS_VERSION%%/conf/docker_logback.xml maps-%%MAPS_VERSION%%/conf/logback.xml

# Set the network configuration
RUN mv maps-%%MAPS_VERSION%%/conf/NetworkManager.yaml maps-%%MAPS_VERSION%%/conf/NetworkManager.yaml_orig && \
    mv maps-%%MAPS_VERSION%%/conf/NetworkManagerDocker.yaml maps-%%MAPS_VERSION%%/conf/NetworkManager.yaml

#----------------------------------------------------------------------------------------------
# Set up the static files for the rest api engine
#----------------------------------------------------------------------------------------------
RUN mkdir /data && \
    mv /maps-%%MAPS_VERSION%%/www /data/www

#----------------------------------------------------------------------------------------------
# Define the data directory in the install as a volume so destination data is stored external
#----------------------------------------------------------------------------------------------
VOLUME /data

#----------------------------------------------------------------------------------------------
# Create a new user and group specifically for messaging
#----------------------------------------------------------------------------------------------
RUN addgroup -S messaginggroup && \
    adduser -S messaginguser -G messaginggroup -s /bin/sh -h /maps-%%MAPS_VERSION%%

#----------------------------------------------------------------------------------------------
# Change ownership of the /data directory and the installation directory
#----------------------------------------------------------------------------------------------
RUN chown -R messaginguser:messaginggroup /data && \
    chown -R messaginguser:messaginggroup /maps-%%MAPS_VERSION%%

#----------------------------------------------------------------------------------------------
# Optionally set permissions on the installation directory, if necessary
#----------------------------------------------------------------------------------------------
RUN chmod -R 755 /maps-%%MAPS_VERSION%%

#----------------------------------------------------------------------------------------------
# We can now simply start the server
# Increase the file descriptor count
#----------------------------------------------------------------------------------------------
CMD export MAPS_HOME=./maps-%%MAPS_VERSION%%/  && \
    ulimit -n 100000 && \
    export PATH=${JAVA_HOME}/bin:${PATH} && \
    su -c "maps-%%MAPS_VERSION%%/bin/startDocker.sh" messaginguser
