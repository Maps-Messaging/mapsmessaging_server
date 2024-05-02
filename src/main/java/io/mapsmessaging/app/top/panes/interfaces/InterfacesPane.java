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

package io.mapsmessaging.app.top.panes.interfaces;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.panes.PaneUpdate;
import io.mapsmessaging.app.top.panes.interfaces.row.InterfacesStatusRowPanel;
import io.mapsmessaging.engine.system.impl.server.InterfaceStatusTopic;
import io.mapsmessaging.rest.data.interfaces.InterfaceStatus;

import java.util.ArrayList;
import java.util.List;

public class InterfacesPane extends PaneUpdate {

  private final int startRow;
  private final List<InterfacesStatusRowPanel> rowList;
  private final TextGraphics headerText;
  private final TextGraphics labelText;

  public InterfacesPane(int startRow, int maxRows, TextGraphics labelText, TextGraphics valueText, TextGraphics headerText) {
    this.startRow = startRow + 1;
    this.headerText = headerText;
    this.labelText = labelText;

    rowList = new ArrayList<>();
    for (int x = 0; x < maxRows - this.startRow; x++) {
      rowList.add(new InterfacesStatusRowPanel(this.startRow + x, labelText, valueText));
    }
  }

  @Override
  public void update(Object obj) {
    if (!enabled) return;
    if (!(obj instanceof InterfaceStatusTopic.InterfaceStatusMessage)) {
      return;
    }
    clear(labelText, startRow - 1, startRow + rowList.size());
    InterfaceStatusTopic.InterfaceStatusMessage statusMessage = (InterfaceStatusTopic.InterfaceStatusMessage) obj;
    List<InterfaceStatus> statusList = statusMessage.getInterfaceStatusList();
    sort(statusList);                         //  "12345678901234567890123456789012345"
    headerText.putString(0, startRow - 1, "        NAME                   |CONN|ERRS|PKTS IN |PKTS OUT|BYTES IN|BYTES OUT|");
    int len = Math.min(rowList.size(), statusList.size());
    for (int x = 0; x < len; x++) {
      InterfaceStatus status = statusList.get(x);
      InterfacesStatusRowPanel statusRow = rowList.get(x);
      statusRow.update(status);
    }
  }

  private void sort(List<InterfaceStatus> list) {
    list.sort((o1, o2) -> {
      long val1 = o1.getBytesReceived() + o1.getBytesSent() + o1.getMessagesSent() + o1.getTotalMessagesReceived() + o1.getConnections();
      long val2 = o2.getBytesReceived() + o2.getBytesSent() + o2.getMessagesSent() + o2.getTotalMessagesReceived() + o2.getConnections();
      return Long.compare(val2, val1);
    });
  }

}
