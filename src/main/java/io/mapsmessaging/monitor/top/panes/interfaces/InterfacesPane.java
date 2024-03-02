package io.mapsmessaging.monitor.top.panes.interfaces;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.engine.system.impl.server.InterfaceStatusTopic;
import io.mapsmessaging.monitor.top.panes.PaneUpdate;
import io.mapsmessaging.monitor.top.panes.interfaces.row.InterfacesStatusRowPanel;
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
    sort(statusList);
    headerText.putString(0, startRow - 1, "        NAME                 | CONN |PKTS READ |PKTS SENT |BYTES READ|BYTES SENT|");
    int len = Math.min(rowList.size(), statusList.size());
    for (int x = 0; x < len; x++) {
      InterfaceStatus status = statusList.get(x);
      InterfacesStatusRowPanel statusRow = rowList.get(x);
      statusRow.update(status);
    }
  }

  private void sort(List<InterfaceStatus> list) {
    list.sort((o1, o2) -> {
      long val1 = o1.getBytesReceived() + o1.getBytesSent() + o1.getMessagesSent() + o1.getMessagesReceived() + o1.getConnections();
      long val2 = o2.getBytesReceived() + o2.getBytesSent() + o2.getMessagesSent() + o2.getMessagesReceived() + o2.getConnections();
      return Long.compare(val2, val1);
    });
  }

}
