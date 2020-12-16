/*
 * Copyright [2020] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.maps.network.protocol.impl.stomp.frames;

import java.io.IOException;

/**
 * Implements the STOMP Connect frame as per https://stomp.github.io/stomp-specification-1.2.html#CONNECT_or_STOMP_Frame
 */
public class Connect extends ClientFrame {

  private String login;
  private String passcode;
  private String acceptVersion;
  private HeartBeat heartBeat;
  private String host;

  public Connect() {
    super();
  }

  public Frame instance() {
    return new Connect();
  }

  public boolean isValid() {
    return true;
  }

  public String getLogin() {
    return login;
  }

  public String getPasscode() {
    return passcode;
  }

  public String getHost() {
    return host;
  }

  public String getAcceptedVersion() {
    return acceptVersion;
  }

  public HeartBeat getHeartBeat() {
    return heartBeat;
  }

  @Override
  public void parseCompleted() throws IOException {
    host = removeHeader("host");
    login = removeHeader("login");
    passcode = removeHeader("passcode");
    acceptVersion = removeHeader("accept-version");
    if (acceptVersion == null) {
      acceptVersion = "1.0"; // This is the only version that allows this
    }
    String tmp = removeHeader("heart-beat");
    if (tmp != null) {
      heartBeat = new HeartBeat(tmp);
    } else {
      heartBeat = new HeartBeat(0, 0); // default
    }
    super.parseCompleted();
  }

  @Override
  public String toString() {
    return "STOMP Connect[host:"
        + host
        + ", login:"
        + login
        + ", acceptVersion"
        + acceptVersion
        + ", HeartBeat:"
        + heartBeat
        + ", Header:"
        + getHeaderAsString()
        + "]";
  }
}
