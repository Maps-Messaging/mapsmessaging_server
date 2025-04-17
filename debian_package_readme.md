To use the debian package manager to install maps messaging, simply


Create a file called  /etc/apt/sources.list.d/mapsmessaging.list and add this in


```shell
echo "deb [arch=all] https://repository.mapsmessaging.io/repository/maps_apt_snapshot/ snapshot main" | sudo tee /etc/apt/sources.list.d/mapsmessaging.list
```


Next you need to install the public key from the repo

```shell
sudo curl -fsSL https://repository.mapsmessaging.io/repository/public_key/keys/public.gpg.key -o /etc/apt/trusted.gpg.d/maps-pgp-key.gpg
```

This will enable apt to update and install the message daemon.

To Install the server then

```shell
sudo apt-get update
sudo apt-get install maps
```

or

```shell
sudo apt-get update
sudo apt-get install maps-ml
```

