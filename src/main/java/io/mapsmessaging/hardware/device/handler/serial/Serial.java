/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.hardware.device.handler.serial;


import com.fazecast.jSerialComm.SerialPort;
import io.mapsmessaging.devices.serial.devices.sensors.SerialDevice;

public class Serial implements SerialDevice {

  private final SerialPort serial;

  public Serial(SerialPort serial) {
    this.serial = serial;
  }

  @Override
  public boolean isOpen() {
    return serial.isOpen();
  }

  @Override
  public boolean openPort() {
    return serial.openPort();
  }

  @Override
  public String getSystemPortName() {
    return serial.getSystemPortName();
  }

  @Override
  public void closePort() {
    serial.closePort();
  }

  @Override
  public int writeBytes(byte[] request, int length) {
    return serial.writeBytes(request, length);
  }

  @Override
  public int readBytes(byte[] buffer, int remaining) {
    return serial.readBytes(buffer, remaining);
  }
}