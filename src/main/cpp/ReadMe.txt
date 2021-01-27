Please ensure you add the following to /boot/config.txt

dtoverlay=gpio-no-irq


Failure to do so may result in the RaspberryPi hanging when attempting to access the LoRa device


You may also need to build the BCM2835 lib required for the LoRa devices


cd ~
wget http://www.airspayce.com/mikem/bcm2835/bcm2835-1.68.tar.gz
tar zxvf bcm2835-1.68.tar.gz
cd bcm2835-1.68
./configure
make
sudo make check
sudo make install