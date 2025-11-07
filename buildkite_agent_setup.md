# Buildkite Agent Setup


## OS

Ubuntu 

### Tools required in the build

```shell
apt-get install vim wget httpie curl jq gh maven openjdk-21-jdk-headless protobuf-compiler rpm unzip
```

Install the SonarScanner CLI by following the [official installation guide](https://docs.sonarsource.com/sonarqube/latest/analyzing-source-code/scanners/sonarscanner/).

### AWS Command line

```shell
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

### Services Required

#### Docker

```properties
for pkg in docker.io docker-doc docker-compose docker-compose-v2 podman-docker containerd runc; do sudo apt-get remove $pkg; done
apt-get update
apt-get install ca-certificates
install -m 0755 -d /etc/apt/keyrings  
curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
chmod a+r /etc/apt/keyrings/docker.asc
echo   "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" |   sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

```

#### BuildKite Agent
```shell
curl -fsSL https://keys.openpgp.org/vks/v1/by-fingerprint/32A37959C2FA5C3C99EFBC32A79206696452D198 | sudo gpg --dearmor -o /usr/share/keyrings/buildkite-agent-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/buildkite-agent-archive-keyring.gpg] https://apt.buildkite.com/buildkite-agent stable main" | sudo tee /etc/apt/sources.list.d/buildkite-agent.list
sudo apt-get update && sudo apt-get install -y buildkite-agent
```
At this point we need to edit the buildkite-agent.cfg

```properties
token="%%BuildKite-Token%%"
name="%hostname-%spawn"
spawn=2
tags="queue=java_build_queue,tag=java"
build-path="/var/lib/buildkite-agent/builds"
hooks-path="/etc/buildkite-agent/hooks"
plugins-path="/etc/buildkite-agent/plugins"
```

You will then need to edit the buildkite maven settings /var/lib/buildkite-agent/.m2/settings.xml
```xml
<settings>
  <servers>
    <server>
      <id>mapsmessaging.io</id>
      <username>matthew.buckton</username>
      <password>*****************</password>
    </server>
      <server>
          <id>maps_release</id>
          <username>buildkite</username>
          <password>*****************</password>
      </server>
     <server>
      <id>maps_snapshots</id>
      <username>buildkite</username>
      <password>*****************</password>
    </server>
    <server>
      <id>maps_test_libs</id>
      <username>buildkite</username>
      <password>*****************</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>matthew.buckton@mapsmessaging.io</id>
      <activation>
        <activeByDefault>true</activeByDefault>
      </activation>
      <properties>
        <gpg.executable>gpg2</gpg.executable>
        <gpg.passphrase>*****************</gpg.passphrase>
      </properties>
    </profile>
  </profiles>
</settings>
```


#### Consul

```shell
wget -O- https://apt.releases.hashicorp.com/gpg | sudo gpg --dearmor -o /usr/share/keyrings/hashicorp-archive-keyring.gpg
echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
sudo apt update && sudo apt install consul

```

Edit /etc/consul.d/consul.hcl and ensure
```properties
server = true
data_dir = "/opt/consul"
client_addr = "127.0.0.1"
advertise_addr = "127.0.0.1"
bootstrap_expect=1
retry_join = ["127.0.0.1"]
```

Finally, the buildkite agent requires sudo at times for the build so need to

```bash
visudo

buildkite-agent ALL=(ALL) NOPASSWD: ALL

```

## SoftHSM for authentication and authorisation library testing

```bash
apt-get install softhsm2
softhsm2-util --init-token --slot 0 --label "testToken"
```