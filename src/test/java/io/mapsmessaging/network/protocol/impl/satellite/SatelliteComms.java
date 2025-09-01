/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
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

package io.mapsmessaging.network.protocol.impl.satellite;

import io.mapsmessaging.network.protocol.impl.satellite.gateway.InmarsatMockServer;
import io.mapsmessaging.network.protocol.impl.satellite.modem.ModemResponder;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SatelliteComms {

  private final Queue<byte[]> modemMessages;
  private final Queue<byte[]> satelliteMessages;
  private final InmarsatMockServer mockWebServer;
  private final ModemResponder modemResponder;

  public SatelliteComms(String comPort) throws IOException {
    modemMessages = new ConcurrentLinkedQueue<>();
    satelliteMessages = new ConcurrentLinkedQueue<>();
    mockWebServer = new InmarsatMockServer(modemMessages, satelliteMessages, 8085);
    modemResponder =new ModemResponder(satelliteMessages, modemMessages, comPort);
  }

  public void start(){
    modemResponder.start();
    mockWebServer.start();
  }

  public void stop(){
    modemResponder.stop();
    mockWebServer.stop();
  }

  public static void main(String[] args) throws IOException {
    SatelliteComms satelliteComms = new SatelliteComms("COM7");
    satelliteComms.start();
  }
}
