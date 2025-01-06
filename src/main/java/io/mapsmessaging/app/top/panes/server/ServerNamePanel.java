/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

package io.mapsmessaging.app.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.formatters.StringFormatter;
import io.mapsmessaging.dto.rest.ServerInfoDTO;
import java.time.LocalDateTime;

public class ServerNamePanel extends ServerDetailsUpdate {

  public ServerNamePanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "", labelText, valueText, new StringFormatter(80, false));
  }

  @Override
  public void update(ServerInfoDTO statusMessage) {
    LocalDateTime dateTime = LocalDateTime.now();
    String value = String.format("%02d:%02d:%02d Server:%s", dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), statusMessage.getServerName());
    panel.update(value);
  }
}
