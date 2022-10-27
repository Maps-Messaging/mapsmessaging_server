POM_VERSION=$(cat pom.xml | grep -m 1 "<version>.*</version>$" | awk -F'[><]' '{print $3}')

wget https://mapsmessaging.jfrog.io/artifactory/mapsmessaging-images-prod/message_daemon-%POM_VERSION%-jar-with-dependencies.jar
pack build maps-server --path message_daemon-%POM_VERSION%-jar-with-dependencies.jar  --builder paketobuildpacks/builder:tiny
docker run --rm --tty --env JAVA_TOOL_OPTIONS='-XX:+UseParallelGC -XX:GCTimeRatio=4 -XX:AdaptiveSizePolicyWeight=90 -XX:MinHeapFreeRatio=20 -XX:MaxHeapFreeRatio=40 -DConsulHost=localhost -DConsulPort=8500' --env BPL_JVM_THREAD_COUNT=100 maps-server
