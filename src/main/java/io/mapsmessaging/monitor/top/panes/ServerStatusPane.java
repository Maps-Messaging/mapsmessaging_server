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

package io.mapsmessaging.monitor.top.panes;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import io.mapsmessaging.monitor.top.panes.server.*;

import java.util.ArrayList;
import java.util.List;

public class ServerStatusPane extends PaneUpdate {

  private final List<ServerStatusUpdate> panes;

  public ServerStatusPane(TextGraphics labelText, TextGraphics valueText) {
    panes = new ArrayList<>();
    panes.add(new TimePanel(0, 0, labelText, labelText));
    panes.add(new UptimePanel(10, 0, labelText, valueText));
    panes.add(new CpuTimePanel(28, 0, labelText, valueText));
    panes.add(new ConnectedPanel(48, 0, labelText, valueText));
    panes.add(new StorageSizePanel(60, 0, labelText, valueText));

    panes.add(new FreeMemoryPanel(0, 1, labelText, valueText));
    panes.add(new UsedMemoryPanel(18, 1, labelText, valueText));
    panes.add(new BuildVersionPanel(37, 1, labelText, labelText));
    panes.add(new BuildDatePanel(57, 1, labelText, labelText));

    panes.add(new ThreadStatusPanel(0, 2, labelText, valueText));

    panes.add(new TotalTopicsPanel(0, 3, labelText, valueText));
    panes.add(new TotalPublishedPanel(14, 3, labelText, valueText));
    panes.add(new TotalSubscribedPanel(25, 3, labelText, valueText));
    panes.add(new TotalNoInterestPanel(37, 3, labelText, valueText));
    panes.add(new TotalDeliveredPanel(49, 3, labelText, valueText));
    panes.add(new TotalRetreivedPanel(59, 3, labelText, valueText));

    panes.add(new TotalPacketsReceivedPanel(0, 4, labelText, valueText));
    panes.add(new TotalPacketsSentPanel(20, 4, labelText, valueText));
    panes.add(new TotalBytesReceivedPanel(40, 4, labelText, valueText));
    panes.add(new TotalBytesSentPanel(60, 4, labelText, valueText));

/*
    labelText.putString(0, 10, "         1         2         3         4         5         6         7         8");
    labelText.putString(0, 11, "12345678901234567890123456789012345678901234567890123456789012345678901234567890");

 */
  }

  public void update(Object obj) {
    if (!(obj instanceof StatusMessage)) {
      return;
    }
    StatusMessage statusMessage = (StatusMessage) obj;
    for (ServerStatusUpdate serverStatusUpdate : panes) {
      serverStatusUpdate.update(statusMessage);
    }
  }
}
