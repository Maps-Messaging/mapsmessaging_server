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

public final class MessagesClient extends BaseInmarsatClient {
  private static final Type MT_DETAIL_LIST = new TypeToken<List<MobileTerminatedDetail>>() {
  }.getType();

  public MessagesClient(URI base, HttpClient http, Gson gson) {
    super(base, http, gson);
  }

  public MobileOriginatedResponse getMobileOriginated(String bearer, String xMailbox, String startTimeIso) {
    try {
      Map<String, String> params = Map.of("includeRawPayload", "true","startTime", startTimeIso );
      return get("messages/mobileOriginated", params, bearer, xMailbox, MobileOriginatedResponse.class);
    } catch (Throwable e) {
      return null;
    }
  }

  public MobileTerminatedSubmitResponse submitMobileTerminated(String bearer, String xMailbox, MobileTerminatedSubmitRequest body) {
    return postJson("messages/mobileTerminated", body, bearer, xMailbox, MobileTerminatedSubmitResponse.class);
  }

  public MobileTerminatedStatusResponse getMobileTerminatedStatus(String bearer, String xMailbox, String startTimeIso) {
    return get("messages/mobileTerminated/status", Map.of("startTime", startTimeIso), bearer, xMailbox, MobileTerminatedStatusResponse.class);
  }

  public List<MobileTerminatedDetail> getMobileTerminatedDetails(String bearer, String xMailbox, List<String> messageIds) {
    String idList = String.join(",", messageIds);
    return get("messages/mobileTerminated", Map.of("idList", idList), bearer, xMailbox, MT_DETAIL_LIST);
  }
}
