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

package io.mapsmessaging.network.protocol.impl.stomp.frames;

public class Connected extends ServerFrame {

  private static final byte[] COMMAND = "CONNECTED".getBytes();

  public Connected() {
    super();
  }

  @Override
  public Frame instance() {
    return new Connected();
  }

  public void setVersion(String version) {
    putHeader("version", version);
  }

  public void setSession(String session) {
    putHeader("session", session);
  }

  public void setServer(String server) {
    putHeader("server", server);
  }

  public void setHeartBeat(HeartBeat heartBeat) {
    putHeader("heart-beat", heartBeat.toString());
  }

  byte[] getCommand() {
    return COMMAND;
  }

  @Override
  public String toString() {
    return "STOMP Connected[ Header:" + getHeaderAsString() + "]";
  }

}
