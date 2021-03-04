rem
rem    Copyright [ 2020 - 2021 ] [Matthew Buckton]
rem
rem    Licensed under the Apache License, Version 2.0 (the "License");
rem    you may not use this file except in compliance with the License.
rem    You may obtain a copy of the License at
rem
rem        http://www.apache.org/licenses/LICENSE-2.0
rem
rem    Unless required by applicable law or agreed to in writing, software
rem    distributed under the License is distributed on an "AS IS" BASIS,
rem    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem    See the License for the specific language governing permissions and
rem    limitations under the License.
rem
rem
rem

set JAVA_HOME=e:\java\zulu13.28.11-ca-jdk13.0.1-win_x64
set MAVEN_HOME=d:\apache-maven-3.5.2
set PATH=PATH=%JAVA_HOME%\bin;%MAVEN_HOME%\bin;%PATH%
call mvn clean install --file eclipse-paho-mqtt-sn-udp-client/pom.xml -Dmaven.compiler.source=1.7 -Dmaven.compiler.target=1.7
call mvn clean install