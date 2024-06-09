To use the debian package manager to install maps messaging, simply


Create a file called  /etc/apt/sources.list.d/mapsmessaging.list and add this in


```shell
deb [arch=all]  https://repository.mapsmessaging.io:8081/repository/maps_messaging_daemon/ snapshot main
```


Next you need to install the public key from the repo

```shell
wget https://repository.mapsmessaging.io:8081/repository/public_key/keys/public.gpg.key
apt-key add public.gpg.key
```

This will enable apt to update and install the message daemon.

To Install the server then

```shell
apt-get update
apt-get install message-daemon
```
