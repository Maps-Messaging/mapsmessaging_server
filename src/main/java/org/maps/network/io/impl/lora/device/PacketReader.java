/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.network.io.impl.lora.device;

import java.util.concurrent.locks.LockSupport;
import org.maps.logging.LogMessages;

public class PacketReader implements Runnable {

  private final LoRaDevice device;
  private boolean isClosed;

  PacketReader(LoRaDevice device){
    this.device = device;
    isClosed = false;
    Thread reader = new Thread(this);
    reader.setDaemon(true);
    reader.setName("LoRa_Packet_Reader:"+device.getName());
    reader.start();
  }

  public void close(){
    isClosed = true;
  }

  @Override
  public void run() {
    Thread.currentThread().setPriority(Thread.MAX_PRIORITY); // We go high to ensure we don't drop packets
    byte[] workingBuffer = new byte[256];
    while(!isClosed){
      try{
        long flags = device.read(workingBuffer, workingBuffer.length);
        if(flags != 0){
          // byte[0] = length of buffer
          // byte[1] = to host
          // byte[2] = from host
          // byte[3] = RSSI value
          short len  = (short) (flags & 0xff);
          short from = (short) (flags >> 8 & 0xff);
          byte rssi = (byte) (flags>> 16 & 0xff);
          short to   = (short) (flags>> 24 & 0xff);
          short id   = (short) (flags>>32 & 0xff);

          if(len > 0){
            byte[] buffer = new byte[len];
            System.arraycopy(workingBuffer, 0, buffer, 0, len);
            LoRaDatagram datagram = new LoRaDatagram(to, from, rssi, buffer, id);
            device.handleIncomingPacket(datagram);
          }
        }
        else{
          LockSupport.parkNanos(1000000);
        }
      }
      catch(Exception ex){
        device.logger.log(LogMessages.LORA_DEVICE_READ_THREAD_ERROR, device.getName(), ex);
      }
    }
  }
}
