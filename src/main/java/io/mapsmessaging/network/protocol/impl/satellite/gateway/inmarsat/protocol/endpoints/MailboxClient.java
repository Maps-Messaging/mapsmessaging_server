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
import io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.Map;

public final class MailboxClient extends BaseInmarsatClient {

  public MailboxClient(URI base, HttpClient http, Gson gson, AuthReset authReset) {
    super(base, http, gson, authReset);
  }

  // X-Mailbox endpoints
  public Mailbox getMailbox(String bearer, String xMailbox) {
    return get("mailbox", Map.of(), bearer, xMailbox, Mailbox.class);
  }

}
