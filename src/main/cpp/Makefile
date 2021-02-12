#
#   Copyright [ 2020 - 2021 ] [Matthew Buckton]
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

# Makefile
# Sample for RH_RF95 (client and server) on Raspberry Pi
# Caution: requires bcm2835 library to be already installed
# http://www.airspayce.com/mikem/bcm2835/

CC            = g++
CFLAGS        = -fPIC  -DRASPBERRY_PI -I"/usr/lib/jvm/java-11-openjdk-armhf/include/" -I"/usr/lib/jvm/java-11-openjdk-armhf/include/linux/" -DBCM2835_NO_DELAY_COMPATIBILITY -D__BASEFILE__=\"$*\"
LDFLAGS       = -shared
LIBS          = -lbcm2835
RADIOHEADBASE = ./RadioHead/
INCLUDE       = -I$(RADIOHEADBASE)

all: libLoRaDevice.so

org_maps_network_io_impl_lora_device_LoRaDevice.o: org_maps_network_io_impl_lora_device_LoRaDevice.cpp
				$(CC) $(CFLAGS) -c $(INCLUDE) $<

RH_RF95.o: $(RADIOHEADBASE)/RH_RF95.cpp
				$(CC) $(CFLAGS) -c $(INCLUDE) $<

RHDatagram.o: $(RADIOHEADBASE)/RHDatagram.cpp
				$(CC) $(CFLAGS) -c $(INCLUDE) $<

RHHardwareSPI.o: $(RADIOHEADBASE)/RHHardwareSPI.cpp
				$(CC) $(CFLAGS) -c $(INCLUDE) $<

RHSPIDriver.o: $(RADIOHEADBASE)/RHSPIDriver.cpp
				$(CC) $(CFLAGS) -c $(INCLUDE) $<

RHGenericDriver.o: $(RADIOHEADBASE)/RHGenericDriver.cpp
				$(CC) $(CFLAGS) -c $(INCLUDE) $<

RHGenericSPI.o: $(RADIOHEADBASE)/RHGenericSPI.cpp
				$(CC) $(CFLAGS) -c $(INCLUDE) $<

RHReliableDatagram.o: $(RADIOHEADBASE)/RHReliableDatagram.cpp
				$(CC) $(CFLAGS) -c $(INCLUDE) $<

RasPi.o: $(RADIOHEADBASE)/RHutil/RasPi.cpp
				$(CC) $(CFLAGS) -c $(RADIOHEADBASE)/RHutil/RasPi.cpp $(INCLUDE) $<

libLoRaDevice.so: org_maps_network_io_impl_lora_device_LoRaDevice.o RHDatagram.o RHReliableDatagram.o RH_RF95.o RasPi.o RHHardwareSPI.o RHGenericDriver.o RHGenericSPI.o RHSPIDriver.o
				$(CC) $^ $(LIBS) -o libLoRaDevice.so $(LDFLAGS)

clean:
				rm -rf *.o *.so
