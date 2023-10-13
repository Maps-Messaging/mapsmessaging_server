/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.engine.session;

import io.mapsmessaging.api.message.Message;
import lombok.Data;
import lombok.ToString;

import java.util.UUID;

@ToString
@Data
public class SessionContext {

  // <editor-fold desc="These fields are persisted and on reload describes the session">
  private final String id;
  private String uniqueId;
  private String willTopic;
  private Message willMessage;
  private long willDelay;
  private long expiry;
  // </editor-fold>

  // <editor-fold desc="These are volatile fields and must not be persisted since they change at run
  // time">
  private final ClientConnection clientConnection;
  private boolean authorized;
  private String username;
  private char[] password;
  private int receiveMaximum;
  private int duration;
  private boolean isRestored;
  private boolean resetState;
  private boolean persistentSession;
  // </editor-fold>

  public SessionContext(String id, ClientConnection clientConnection) {
    this.id = id;
    this.clientConnection = clientConnection;
    expiry = -1;
    receiveMaximum = (1 << 16) - 1;
    isRestored = false;
    duration = -1;
    uniqueId = UUID.randomUUID().toString();
  }
}
