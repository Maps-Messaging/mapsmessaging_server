
docker run --rm \
        --user root \
        --mount source=dataVolume,target=/workspace/data \
        --tty \
        --env JAVA_TOOL_OPTIONS='-XX:+UseParallelGC -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40' \
        --env BPL_JVM_THREAD_COUNT=100 \
        -p 1883:1883/tcp \
        -p 1884:1884/udp \
        -p 5683:5683/udp \
        -p 8675:8675/tcp \
        -p 1700:1700/udp \
        -p 8080:8080/tcp \
        maps-server
