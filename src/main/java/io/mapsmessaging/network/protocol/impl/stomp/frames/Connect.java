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

import java.io.IOException;

/**
 * Implements the STOMP Connect frame as per https://stomp.github.io/stomp-specification-1.2.html#CONNECT_or_STOMP_Frame
 */
public class Connect extends Frame {

  private String login;
  private String passcode;
  private String acceptVersion;
  private HeartBeat heartBeat;
  private String host;

  public Connect() {
    super();
  }

  @Override
  byte[] getCommand() {
    return "STOMP".getBytes();
  }

  public void setLogin(String login) {
    putHeader("login", login);
    this.login = login;
  }

  public void setPasscode(String passcode) {
    putHeader("passcode", passcode);
    this.passcode = passcode;
  }

  public void setAcceptVersion(String acceptVersion) {
    putHeader("accept-version", acceptVersion);
    this.acceptVersion = acceptVersion;
  }

  public void setHeartBeat(HeartBeat heartBeat) {
    this.heartBeat = heartBeat;
  }

  public void setHost(String host) {
    putHeader("host", host);
    this.host = host;
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
    if (login != null && login.equals("null")) {
      login = null;
    }
    if (passcode != null && passcode.equals("null")) {
      passcode = null;
    }
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
