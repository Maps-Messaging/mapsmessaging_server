POM_VERSION=$(cat pom.xml | grep -m 1 "<version>.*</version>$" | awk -F'[><]' '{print $3}')
echo $POM_VERSION
cd src/main/docker
# Build the new Docker image

mv Dockerfile Dockerfile.orig
sed s/%%MAPS_VERSION%%/$POM_VERSION/g Dockerfile.orig > Dockerfile

docker build -t mapsmessaging-daemon --label $POM_VERSION .
