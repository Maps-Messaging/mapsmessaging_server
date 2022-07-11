/*
 *
 *   Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.echo;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.Packet;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import io.mapsmessaging.network.protocol.detection.ScanningByteArrayDetection;
import java.io.IOException;

public class EchoProtocolFactory extends io.mapsmessaging.network.protocol.ProtocolImplFactory {

  private static final byte[] CHECK_CODE = "ECHO".getBytes();

  public EchoProtocolFactory() {
    super("ECHO", "Echo Protocol, Not for Production", new ScanningByteArrayDetection(CHECK_CODE, 0, 12));
  }

  @Override
  public ProtocolImpl connect(EndPoint endPoint, String sessionId, String username, String password) throws IOException {
    return null;

  }

  public void create(EndPoint endPoint, Packet packet) throws IOException {
    new EchoProtocol(endPoint, packet);
  }

  @Override
  public String getTransportType() {
    return "tcp";
  }

}
