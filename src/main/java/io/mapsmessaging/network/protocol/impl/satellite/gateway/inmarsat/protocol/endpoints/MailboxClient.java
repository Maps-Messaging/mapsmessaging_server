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

package io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.endpoints;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model.*;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

public final class MailboxClient extends BaseInmarsatClient {
  private static final Type MAILBOX_LIST = new TypeToken<List<Mailbox>>() {
  }.getType();

  public MailboxClient(URI base, HttpClient http, Gson gson) {
    super(base, http, gson);
  }

  // X-Mailbox endpoints
  public Mailbox getMailbox(String bearer, String xMailbox) {
    return get("mailbox", Map.of(), bearer, xMailbox, Mailbox.class);
  }

  public MailboxCodecAck uploadCodec(String bearer, String xMailbox, MailboxCodecUploadRequest reqBody) {
    return postJson("mailbox/codec", reqBody, bearer, xMailbox, MailboxCodecAck.class);
  }

  public void deleteCodec(String bearer, String xMailbox) {
    // reuse postJson with Void? -> just call sendVoid via a DELETE helper if you add one; for now, inline minimal:
    var req = java.net.http.HttpRequest.newBuilder(base.resolve("mailbox/codec"))
        .header("Authorization", "Bearer " + bearer)
        .header("X-Mailbox", xMailbox)
        .DELETE().build();
    sendVoid(req);
  }

  public MailboxPasswordResponse changePassword(String bearer, String xMailbox, String newPasswordOrNull) {
    return postJson("mailbox/password/change", new MailboxPasswordChangeRequest(newPasswordOrNull),
        bearer, xMailbox, MailboxPasswordResponse.class);
  }

}
