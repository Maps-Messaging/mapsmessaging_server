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

package io.mapsmessaging.engine.session;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.dto.rest.session.SessionContextDTO;
import io.mapsmessaging.engine.session.security.SecurityContext;
import io.mapsmessaging.security.uuid.UuidGenerator;
import lombok.Data;
import lombok.ToString;

@ToString
@Data
public class SessionContext {

  // <editor-fold desc="These fields are persisted and on reload describes the session">
  private final String id;
  private String uniqueId;
  private long internalSessionId;
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
  private boolean isRestored;
  private boolean resetState;
  private boolean persistentSession;
  // </editor-fold>

  private SecurityContext securityContext;

  public SessionContext(String id, ClientConnection clientConnection) {
    this.id = id;
    this.clientConnection = clientConnection;
    expiry = -1;
    receiveMaximum = (1 << 16) - 1;
    isRestored = false;
    uniqueId = UuidGenerator.getInstance().generate().toString();
  }

  public void update(SessionPrivileges sessionPrivileges) {

  }

  public SessionContextDTO getDetails(){
    SessionContextDTO sessionContextDTO = new SessionContextDTO();
    sessionContextDTO.setId(id);
    sessionContextDTO.setUniqueId(uniqueId);
    sessionContextDTO.setPersistentSession(persistentSession);
    sessionContextDTO.setHasWill(willMessage != null);
    sessionContextDTO.setAuthorized(authorized);
    sessionContextDTO.setRestored(isRestored);
    sessionContextDTO.setResetState(resetState);
    sessionContextDTO.setExpiry(expiry);
    sessionContextDTO.setReceiveMaximum(receiveMaximum);
    return sessionContextDTO;
  }
}
