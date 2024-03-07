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

package io.mapsmessaging.app.top.panes.destination.row;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.formatters.Formatter;
import io.mapsmessaging.app.top.formatters.StringFormatter;
import io.mapsmessaging.app.top.panes.destination.DestinationStatusUpdate;
import io.mapsmessaging.rest.data.destination.DestinationStatus;

public class DestinationDiskPanel extends DestinationStatusUpdate {

  private final Formatter formatter;

  protected DestinationDiskPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "", labelText, valueText, new StringFormatter(24, false));
    formatter = new StringFormatter(7, true);
  }

  @Override
  public void update( DestinationStatus statusMessage) {
    StringBuilder value = new StringBuilder();
    value.append(formatNanoseconds(statusMessage.getReadTimeAve_ns())).append(" ");
    value.append(formatNanoseconds(statusMessage.getWriteTimeAve_ns())).append(" ");
    value.append(formatNanoseconds(statusMessage.getDeleteTimeAve_ns()));
    panel.update(value.toString());
  }

  public String formatNanoseconds(long nanoseconds) {
    String val = "";
    if (nanoseconds < 1000) { // Nanoseconds
      val = String.format("%d ns", nanoseconds);
    } else if (nanoseconds < 1_000_000) { // Microseconds
      val = String.format("%d µs", nanoseconds / 1000);
    } else if (nanoseconds < 1_000_000_000) { // Milliseconds
      val = String.format("%d ms", nanoseconds / 1_000_000);
    } else { // Seconds
      val = String.format("%d s", nanoseconds / 1_000_000_000);
    }
    return formatter.pad(val, 7, true);
  }
}
