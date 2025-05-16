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


docker run --rm \
        --user root \
        --mount source=dataVolume,target=/workspace/data \
        --tty \
        --env JAVA_TOOL_OPTIONS='-XX:+UseParallelGC -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -DConsulHost=consul' \
        --env BPL_JVM_THREAD_COUNT=100 \
        -p 1883:1883/tcp \
        -p 1884:1884/udp \
        -p 5683:5683/udp \
        -p 8675:8675/tcp \
        -p 1700:1700/udp \
        -p 8080:8080/tcp \
        maps-server
