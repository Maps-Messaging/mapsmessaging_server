# syntax=docker/dockerfile:1
#----------------------------------------------------------------------------------------------
# Get Alpine linux image and add the Zulu JDK 13, as per
# https://github.com/zulu-openjdk/zulu-openjdk/blob/master/alpine/13.0.1-13.28/Dockerfile
#----------------------------------------------------------------------------------------------
FROM alpine
ENV LANG en_US.UTF-8
ENV LANGUAGE en_US:en
ENV LC_ALL en_US.UTF-8
ENV JAVA_HOME=/usr/lib/jvm/zulu-13
RUN ZULU_ARCH=zulu13.28.11-ca-jdk13.0.1-linux_musl_x64.tar.gz && \
    INSTALL_DIR=$( dirname $JAVA_HOME ) && \
    BIN_DIR=/usr/bin && \
    MAN_DIR=/usr/share/man/man1 && \
    ZULU_DIR=$( basename ${ZULU_ARCH} .tar.gz ) && \
    wget -q https://cdn.azul.com/zulu/bin/${ZULU_ARCH} && \
    mkdir -p ${INSTALL_DIR} && \
    tar -xf ./${ZULU_ARCH} -C ${INSTALL_DIR} && rm -f ${ZULU_ARCH} && \
    mv ${INSTALL_DIR}/${ZULU_DIR} ${JAVA_HOME} && \
    cd ${BIN_DIR} && find ${JAVA_HOME}/bin -type f -perm -a=x -exec ln -s {} . \; && \
    mkdir -p ${MAN_DIR} && \
    cd ${MAN_DIR} && find ${JAVA_HOME}/man/man1 -type f -name "*.1" -exec ln -s {} . \;

#----------------------------------------------------------------------------------------------
# We now have a JDK and Linux image. Now to
#   a) Expose all network ports the server may need
#   b) Install the latest image from deployments
#   c) Configure the scripts specific for docker
#   d) Configure Hawtio for JMX / Rest / Web management
#   e) Define a volume mount for the data directory
#   f) Register the command to start the server
#----------------------------------------------------------------------------------------------
# Expose the protocol ports
# MQTT      1882, 1883  TCP
# MQTT-SN   1884        UDP
# AMQP      5672        TCP
# Stomp     8675        TCP
# Admin     8080        TCP
# SemTech   1700        UDP
# CoAP      5683        UDP
#----------------------------------------------------------------------------------------------
EXPOSE 1882/tcp
EXPOSE 1883/tcp
EXPOSE 1884/udp
EXPOSE 5672/tcp
EXPOSE 8675/tcp
EXPOSE 8080/tcp
EXPOSE 1700/udp
EXPOSE 5683/udp

#----------------------------------------------------------------------------------------------
# Install the maps messaging daemon image
#----------------------------------------------------------------------------------------------
RUN wget -q https://mapsmessaging.jfrog.io/artifactory/mapsmessaging-images-prod/message_daemon-%%MAPS_VERSION%%-install.tar.gz && \
    tar -xf message_daemon-%%MAPS_VERSION%%-install.tar.gz  && \
    rm  message_daemon-%%MAPS_VERSION%%-install.tar.gz

#----------------------------------------------------------------------------------------------
# Now configure the scripts
#----------------------------------------------------------------------------------------------
RUN chmod +x message_daemon-%%MAPS_VERSION%%/bin/startDocker.sh && \
    dos2unix message_daemon-%%MAPS_VERSION%%/bin/startDocker.sh && \
    mv message_daemon-%%MAPS_VERSION%%/conf/logback.xml message_daemon-%%MAPS_VERSION%%/conf/logback.xml_orig && \
    mv message_daemon-%%MAPS_VERSION%%/conf/docker_logback.xml message_daemon-%%MAPS_VERSION%%/conf/logback.xml


#----------------------------------------------------------------------------------------------
# Now configure and download hawtio for web/JMX/Rest administration
#----------------------------------------------------------------------------------------------
RUN  cd ./message_daemon-%%MAPS_VERSION%% &&  \
    mv conf/hawtio.yaml conf/hawtio.yaml_old  && \
    sed 's|C:/Users/matthew/Downloads/hawtio-war-2.15.0.war|./message_daemon-%%MAPS_VERSION%%/hawtio-default-2.15.0.war|' conf/hawtio.yaml_old > conf/hawtio.yaml && \
    rm conf/hawtio.yaml_old && \
    wget https://repo1.maven.org/maven2/io/hawt/hawtio-default/2.15.0/hawtio-default-2.15.0.war

#----------------------------------------------------------------------------------------------
# Define the data directory in the install as a volume so destination data is stored external
#----------------------------------------------------------------------------------------------
VOLUME /data

#----------------------------------------------------------------------------------------------
# We can now simply start the server
# Increase the file descriptor count
#----------------------------------------------------------------------------------------------
CMD export MAPS_HOME=./message_daemon-%%MAPS_VERSION%%/  && \
    ulimit -n 100000 && \
    message_daemon-%%MAPS_VERSION%%/bin/startDocker.sh