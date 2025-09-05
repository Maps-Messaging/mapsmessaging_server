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

package io.mapsmessaging.app.top.panes.destination;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.panes.PaneUpdate;
import io.mapsmessaging.app.top.panes.destination.row.DestinationStatusRowPanel;
import io.mapsmessaging.dto.rest.destination.DestinationDTO;
import io.mapsmessaging.rest.responses.DestinationResponse;

import java.util.ArrayList;
import java.util.List;

public class DestinationPane extends PaneUpdate {

  private final int startRow;
  private final List<DestinationStatusRowPanel> rowList;
  private final TextGraphics headerText;
  private final TextGraphics labelText;

  public DestinationPane(int startRow, int maxRows, TextGraphics labelText, TextGraphics valueText, TextGraphics headerText){
    this.startRow = startRow+1;
    this.headerText = headerText;
    rowList = new ArrayList<>();
    this.labelText = labelText;
    for(int x=0;x<maxRows-this.startRow;x++){
      rowList.add(new DestinationStatusRowPanel(this.startRow+x, labelText, valueText));
    }
  }

  public void update(Object obj) {
    if (!enabled) return;
    if (!(obj instanceof DestinationResponse)) {
      return;
    }
    clear(labelText, startRow - 1, startRow + rowList.size());
    DestinationResponse statusMessage = (DestinationResponse) obj;
    List<DestinationDTO> destinationStatusList = statusMessage.getData();
    sort(destinationStatusList);
    headerText.putString(0, startRow-1, "     NAME      |PUB |SENT|DISK|DROP|PTX |RTV |EXP |DLY | Read | Write |Delete |");
    int len = Math.min(rowList.size(), destinationStatusList.size());
    for(int x=0;x<len;x++) {
      DestinationDTO destinationStatus = destinationStatusList.get(x);
      DestinationStatusRowPanel statusRow = rowList.get(x);
      statusRow.update(destinationStatus);
    }
  }

  private  void sort( List<DestinationDTO> list){
    list.sort((o1, o2) -> {
      long val1 = o1.getPublishedMessages() + o1.getDeliveredMessages() + o1.getStoredMessages();
      long val2 = o2.getPublishedMessages() + o2.getDeliveredMessages() + o2.getStoredMessages();
      return Long.compare(val2, val1);
    });
  }

}
