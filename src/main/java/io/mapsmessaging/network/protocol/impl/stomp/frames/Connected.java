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

package io.mapsmessaging.network.protocol.impl.stomp.frames;

import java.io.IOException;

public class Connected extends ServerFrame {

  private static final byte[] COMMAND = "CONNECTED".getBytes();

  private String version;
  private String session;
  private String server;
  private HeartBeat heartBeat = new HeartBeat(0, 0);

  public Connected() {
    super();
  }

  @Override
  public Frame instance() {
    return new Connected();
  }

  @Override
  protected boolean escapeHeaders() {
    return false;
  }

  public void setVersion(String version) {
    this.version = version;
    putHeader("version", version);
  }

  public void setSession(String session) {
    this.session = session;
    putHeader("session", session);
  }

  public void setServer(String server) {
    this.server = server;
    putHeader("server", server);
  }

  public void setHeartBeat(HeartBeat heartBeat) {
    this.heartBeat = heartBeat;
    putHeader("heart-beat", heartBeat.toString());
  }

  public String getVersion() {
    return version;
  }

  public String getSession() {
    return session;
  }

  public String getServer() {
    return server;
  }

  public HeartBeat getHeartBeat() {
    return heartBeat;
  }

  @Override
  public void parseCompleted() throws IOException {
    version = getHeader("version");
    session = getHeader("session");
    server = getHeader("server");
    String heartbeatValue = getHeader("heart-beat");
    heartBeat = heartbeatValue == null ? new HeartBeat(0, 0) : new HeartBeat(heartbeatValue);
    super.parseCompleted();
  }

  byte[] getCommand() {
    return COMMAND;
  }

  @Override
  public String toString() {
    return "STOMP Connected[ Header:" + getHeaderAsString() + "]";
  }
}
