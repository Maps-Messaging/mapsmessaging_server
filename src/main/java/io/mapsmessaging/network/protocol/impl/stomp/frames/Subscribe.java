/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.stomp.frames;

import java.io.IOException;

/**
 * Implements the Subscribe Frame as per https://stomp.github.io/stomp-specification-1.2.html#SUBSCRIBE
 */
public class Subscribe extends Frame {

  private String id;
  private String destination;
  private String ack;
  private String shareName;
  private boolean isValid;
  private boolean isShared;

  @Override
  public boolean isValid() {
    return isValid;
  }

  public String getDestination() {
    return destination.trim();
  }

  public String getId() {
    return id.trim();
  }

  public String getAck() {
    return ack;
  }

  public String getSelector() {
    return getHeader("selector");
  }

  public boolean isShared() {
    return isShared;
  }

  public String getShareName() {
    return shareName;
  }

  public void setId(String id) {
    this.id = id;
    getHeader().put("id", id);
  }

  public void setDestination(String destination) {
    this.destination = destination;
    getHeader().put("destination", destination);
  }

  public void setAck(String ack) {
    this.ack = ack;
    getHeader().put("ack", ack);
  }

  public void setShareName(String shareName) {
    this.shareName = shareName;
  }

  @Override
  public void parseCompleted() throws IOException {
    isShared = false;
    shareName = "";
    destination = getHeader("destination");
    id = getHeader("id");
    isValid = (destination != null && id != null);
    ack = getHeader("ack");
    if (ack != null) {
      ack = ack.toLowerCase().trim();
      if (!(ack.equals("auto") || ack.equals("client") || ack.equals("client-individual"))) {
        isValid = false;
      }
    } else {
      ack = "auto";
    }
    if (headerContainsKey("shared")) {
      shareName = getHeader("shared");
      isShared = true;
    }
    super.parseCompleted();
  }

  @Override
  byte[] getCommand() {
    return "SUBSCRIBE".getBytes();
  }

  @Override
  public Frame instance() {
    return new Subscribe();
  }

  @Override
  public String toString() {
    return "STOMP Subscribe[ Destination:"
        + destination
        + " id:"
        + id
        + ", Ack:"
        + ack
        + ", Header:"
        + getHeaderAsString()
        + "]";
  }
}
