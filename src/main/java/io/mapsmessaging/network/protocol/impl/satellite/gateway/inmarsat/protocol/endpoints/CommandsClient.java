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
import io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model.ChangeModeCommand;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model.MobileTerminatedSubmitResponse;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model.MuteCommand;
import io.mapsmessaging.network.protocol.impl.satellite.gateway.inmarsat.protocol.model.ResetCommand;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;

public final class CommandsClient extends BaseInmarsatClient {
  public CommandsClient(URI base, HttpClient http, Gson gson, AuthReset authReset) {
    super(base, http, gson, authReset);
  }

  public MobileTerminatedSubmitResponse mute(String bearer, String xMailbox, List<MuteCommand> cmds, boolean usePost) {
    return usePost
        ? postJson("messages/mobileTerminated/mute", cmds, bearer, xMailbox, MobileTerminatedSubmitResponse.class)
        : getWithBodyJson("messages/mobileTerminated/mute", cmds, bearer, xMailbox, MobileTerminatedSubmitResponse.class);
  }

  public MobileTerminatedSubmitResponse mute(String b, String x, MuteCommand c, boolean post) {
    return mute(b, x, List.of(c), post);
  }
}
