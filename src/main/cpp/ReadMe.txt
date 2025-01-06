#
#
# Copyright [ 2020 - 2024 ] [Matthew Buckton]
# Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#


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