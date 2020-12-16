Please ensure you add the following to /boot/config.txt

dtoverlay=gpio-no-irq


Failure to do so may result in the RaspberryPi hanging when attempting to access the LoRa device