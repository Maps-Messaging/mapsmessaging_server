/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.session;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.ProtocolImpl;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class SessionContext {

  // <editor-fold desc="These fields are persisted and on reload describes the session">
  @Getter
  private final String id;

  @Getter
  @Setter
  private String uniqueId;

  @Getter
  @Setter
  private String willTopic;

  @Getter
  @Setter
  private Message willMessage;

  @Getter
  @Setter
  private long willDelay;

  @Getter
  @Setter
  private long expiry;
  // </editor-fold>

  // <editor-fold desc="These are volatile fields and must not be persisted since they change at run
  // time">

  @Getter
  private final ProtocolImpl protocol;


  @Getter
  @Setter
  private String authenticationMethod;

  @Getter
  @Setter
  private byte[] authenticationData;

  @Getter
  @Setter
  private String username;

  @Getter
  @Setter
  private char[] password;

  @Getter
  @Setter
  private int receiveMaximum;

  @Getter
  @Setter
  private int duration;

  @Getter
  @Setter
  private boolean isRestored;

  @Getter
  @Setter
  private boolean resetState;

  @Getter
  @Setter
  private boolean persistentSession;
  // </editor-fold>

  public SessionContext(String id, ProtocolImpl protocol) {
    this.id = id;
    this.protocol = protocol;
    expiry = -1;
    receiveMaximum = (1 << 16) - 1;
    isRestored = false;
    duration = -1;
    uniqueId = UUID.randomUUID().toString();
  }
}
