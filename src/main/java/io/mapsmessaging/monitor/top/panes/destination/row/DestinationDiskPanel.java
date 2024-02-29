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

package io.mapsmessaging.monitor.top.panes.destination.row;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.monitor.top.formatters.StringFormatter;
import io.mapsmessaging.monitor.top.panes.destination.DestinationStatusUpdate;
import io.mapsmessaging.rest.data.destination.DestinationStatus;

public class DestinationDiskPanel extends DestinationStatusUpdate {

  protected DestinationDiskPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "", labelText, valueText, new StringFormatter(24, false));
  }

  @Override
  public void update( DestinationStatus statusMessage) {
    StringBuilder value = new StringBuilder();
    value.append(convertNanoToSecond(statusMessage.getReadTimeAve_ns())).append(" ");
    value.append(convertNanoToSecond(statusMessage.getWriteTimeAve_ns())).append(" ");
    value.append(convertNanoToSecond(statusMessage.getDeleteTimeAve_ns()));
    panel.update(value.toString());
  }

  private String convertNanoToSecond(long nano){
    float value = nano/1_000_000f;
    int integerPartLength = Integer.toString((int)value).length();
    int decimalPlaces = 6 - integerPartLength; // 5 characters - integer part length - 1 for the decimal point
    decimalPlaces = Math.max(decimalPlaces, 0);
    String format = "%." + decimalPlaces + "f";
    String formatted = String.format(format, value);
    if (formatted.length() > 7) {
      formatted = formatted.substring(0, 7);
    }
    return formatted;
  }
}
