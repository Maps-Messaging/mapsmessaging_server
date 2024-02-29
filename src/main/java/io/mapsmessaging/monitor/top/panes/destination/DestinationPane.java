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

package io.mapsmessaging.monitor.top.panes.destination;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.engine.system.impl.server.DestinationStatusTopic;
import io.mapsmessaging.monitor.top.panes.PaneUpdate;
import io.mapsmessaging.monitor.top.panes.destination.row.DestinationStatusRowPanel;
import io.mapsmessaging.rest.data.destination.DestinationStatus;

import java.util.ArrayList;
import java.util.List;

public class DestinationPane implements PaneUpdate {
  private final int startRow;
  private final int maxRows;
  private final List<DestinationStatusRowPanel> rowList;

  public DestinationPane(int startRow, int maxRows, TextGraphics labelText, TextGraphics valueText, TextGraphics headerText){
    this.startRow = startRow+1;
    this.maxRows = maxRows - startRow;
    rowList = new ArrayList<>();
    for(int x=0;x<maxRows-this.startRow;x++){
      rowList.add(new DestinationStatusRowPanel(this.startRow+x, labelText, valueText));
    }
    headerText.putString(0, startRow, "     NAME      |PUB |SENT|DISK|DROP|PTX |RTV |EXP |DLY |I/O R S|I/O W S|I/O D S|");
  }



  @Override
  public void update(Object obj) {
    if (!(obj instanceof DestinationStatusTopic.DestinationStatusMessage)) {
      return;
    }
    DestinationStatusTopic.DestinationStatusMessage statusMessage = (DestinationStatusTopic.DestinationStatusMessage) obj;
    List<DestinationStatus> destinationStatusList = statusMessage.getDestinationStatusList();
    sort(destinationStatusList);

    int len = Math.min(rowList.size(), destinationStatusList.size());
    for(int x=0;x<len;x++) {
      DestinationStatus destinationStatus = destinationStatusList.get(x);
      DestinationStatusRowPanel statusRow = rowList.get(x);
      statusRow.update(destinationStatus);
    }
  }

  private  void sort( List<DestinationStatus> list){
    list.sort((o1, o2) -> {
      long val1 = o1.getPublishedMessages() + o1.getDeliveredMessages() + o1.getStoredMessages();
      long val2 = o2.getPublishedMessages() + o2.getDeliveredMessages() + o2.getStoredMessages();
      return Long.compare(val2, val1);
    });
  }

}
