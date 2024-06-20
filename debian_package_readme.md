To use the debian package manager to install maps messaging, simply


Create a file called  /etc/apt/sources.list.d/mapsmessaging.list and add this in


```shell
echo "deb [arch=all] https://repository.mapsmessaging.io:8081/repository/maps_messaging_daemon/ snapshot main" | sudo tee /etc/apt/sources.list.d/mapsmessaging.list
```


Next you need to install the public key from the repo

```shell
wget -qO- https://repository.mapsmessaging.io:8081/repository/public_key/keys/public.gpg.key | gpg --dearmor -o /etc/apt/trusted.gpg.d/mapsmessaging.gpg
```

This will enable apt to update and install the message daemon.

To Install the server then

```shell
sudo apt-get update
sudo apt-get install message-daemon
```



Complete script
```shell
# Create the APT source list file
echo "deb [arch=all] https://repository.mapsmessaging.io:8081/repository/maps_messaging_daemon/ snapshot main" | sudo tee /etc/apt/sources.list.d/mapsmessaging.list

# Download and add the public key
wget -O- https://repository.mapsmessaging.io:8081/repository/public_key/keys/public.gpg.key | sudo apt-key add -

# Update package list and install the message daemon
sudo apt-get update
sudo apt-get install message-daemon

```