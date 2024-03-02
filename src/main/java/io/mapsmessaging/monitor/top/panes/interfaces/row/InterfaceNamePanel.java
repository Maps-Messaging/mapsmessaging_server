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

package io.mapsmessaging.monitor.top.panes.interfaces.row;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.monitor.top.formatters.StringFormatter;
import io.mapsmessaging.monitor.top.panes.destination.DestinationStatusUpdate;
import io.mapsmessaging.monitor.top.panes.interfaces.InterfacesStatusUpdate;
import io.mapsmessaging.rest.data.destination.DestinationStatus;
import io.mapsmessaging.rest.data.interfaces.InterfaceStatus;

public class InterfaceNamePanel extends InterfacesStatusUpdate {

  protected InterfaceNamePanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "", labelText, valueText, new StringFormatter(29, false));
  }

  @Override
  public void update(InterfaceStatus statusMessage) {
    String val = statusMessage.getInterfaceName();
    if (val.length() > 29) {
      val = "~" + val.substring(val.length() - 28);
    }
    panel.update(val);
  }
}
