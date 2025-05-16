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

package io.mapsmessaging.app.top.panes.destination.row;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.formatters.DecimalSizeFormatter;
import io.mapsmessaging.app.top.formatters.Formatter;
import io.mapsmessaging.app.top.formatters.StringFormatter;
import io.mapsmessaging.app.top.panes.destination.DestinationStatusUpdate;
import io.mapsmessaging.dto.rest.destination.DestinationDTO;

public class DestinationMetricsPanel extends DestinationStatusUpdate {

  private final Formatter unitFormatter;

  protected DestinationMetricsPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "", labelText, valueText, new StringFormatter(40, true));
    unitFormatter = new DecimalSizeFormatter(4, true, false);
  }

  @Override
  public void update(DestinationDTO statusMessage) {
    StringBuilder value = new StringBuilder();
    value.append(unitFormatter.format(statusMessage.getPublishedMessages())).append(" ");
    value.append(unitFormatter.format(statusMessage.getDeliveredMessages())).append(" ");
    value.append(unitFormatter.format(statusMessage.getStoredMessages())).append(" ");
    value.append(unitFormatter.format(statusMessage.getNoInterestMessages())).append(" ");
    value.append(unitFormatter.format(statusMessage.getPendingMessages())).append(" ");
    value.append(unitFormatter.format(statusMessage.getRetrievedMessages())).append(" ");
    value.append(unitFormatter.format(statusMessage.getExpiredMessages())).append(" ");
    value.append(unitFormatter.format(statusMessage.getDelayedMessages())).append(" ");
    panel.update(value.toString());
  }
}
