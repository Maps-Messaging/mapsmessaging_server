/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
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

package io.mapsmessaging.app.top.panes.interfaces.row;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.formatters.Formatter;
import io.mapsmessaging.app.top.formatters.StringFormatter;
import io.mapsmessaging.app.top.formatters.DecimalSizeFormatter;
import io.mapsmessaging.app.top.panes.interfaces.InterfacesStatusUpdate;
import io.mapsmessaging.rest.data.interfaces.InterfaceStatus;

public class InterfaceMetricsPanel extends InterfacesStatusUpdate {

  private final Formatter connectionFormatter;
  private final Formatter unitFormatter;

  protected InterfaceMetricsPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "", labelText, valueText, new StringFormatter(52, false));
    connectionFormatter = new DecimalSizeFormatter(4, true, false);
    unitFormatter = new DecimalSizeFormatter(8, true, false);
  }

  @Override
  public void update(InterfaceStatus statusMessage) {
    StringBuilder value = new StringBuilder();
    value.append(connectionFormatter.format(statusMessage.getConnections())).append(" ");
    value.append(connectionFormatter.format(statusMessage.getErrors())).append(" ");
    value.append(unitFormatter.format(statusMessage.getMessagesReceived())).append(" ");
    value.append(unitFormatter.format(statusMessage.getMessagesSent())).append(" ");
    value.append(unitFormatter.format(statusMessage.getBytesReceived())).append(" ");
    value.append(unitFormatter.format(statusMessage.getBytesSent())).append(" ");
    panel.update(value.toString());
  }
}
