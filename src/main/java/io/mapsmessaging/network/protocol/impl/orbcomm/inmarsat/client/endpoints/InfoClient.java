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

package io.mapsmessaging.network.protocol.impl.orbcomm.inmarsat.client.endpoints;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.mapsmessaging.network.protocol.impl.orbcomm.inmarsat.client.model.ErrorDef;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;

public final class InfoClient extends BaseInmarsatClient {
  private static final Type ERROR_LIST = new TypeToken<List<ErrorDef>>() {
  }.getType();

  public InfoClient(URI base, HttpClient http, Gson gson) {
    super(base, http, gson);
  }

  /**
   * VAR / global: no X-Mailbox header
   */
  public List<ErrorDef> getErrorCodes(String bearer) {
    return getNoMailbox("info/errors", Map.of(), bearer, ERROR_LIST);
  }
}
