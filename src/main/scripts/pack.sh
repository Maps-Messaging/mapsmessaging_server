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

POM_VERSION=$(cat pom.xml | grep -m 1 "<version>.*</version>$" | awk -F'[><]' '{print $3}')

wget https://github.com/Maps-Messaging/mapsmessaging_server/releases/download/v%POM_VERSION%/message_daemon-%POM_VERSION%-jar-with-dependencies.jar
pack build maps-server --path message_daemon-%POM_VERSION%-jar-with-dependencies.jar  --builder paketobuildpacks/builder:tiny
docker run --rm --tty --env JAVA_TOOL_OPTIONS='-XX:+UseParallelGC -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -DConsulHost=localhost -DConsulPort=8500' --env BPL_JVM_THREAD_COUNT=100 maps-server
