/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging B.V.]
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
import io.mapsmessaging.app.top.formatters.DecimalSizeFormatter;
import io.mapsmessaging.dto.rest.StatusMessageDTO;

public class TotalRetreivedPanel extends ServerStatusUpdate {

  public TotalRetreivedPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "Rtv: ", labelText, valueText, new DecimalSizeFormatter(6));
  }

  @Override
  public void update(StatusMessageDTO statusMessage) {
    panel.update(statusMessage.getServerStatistics().getTotalRetrievedMessages());
  }
}