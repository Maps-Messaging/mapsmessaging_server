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

package io.mapsmessaging.app.top.panes;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.panes.server.*;
import io.mapsmessaging.dto.rest.ServerInfoDTO;

import java.util.ArrayList;
import java.util.List;

public class ServerDetailsPane extends PaneUpdate {

  private final List<ServerDetailsUpdate> panes;

  public ServerDetailsPane(TextGraphics labelText, TextGraphics valueText) {
    panes = new ArrayList<>();
    panes.add(new TimePanel(0, 0, labelText, labelText));
    panes.add(new UptimePanel(10, 0, labelText, valueText));
    panes.add(new CpuTimePanel(30, 0, labelText, valueText));
    panes.add(new ConnectedPanel(48, 0, labelText, valueText));
    panes.add(new StorageSizePanel(60, 0, labelText, valueText));

    panes.add(new FreeMemoryPanel(0, 1, labelText, valueText));
    panes.add(new UsedMemoryPanel(18, 1, labelText, valueText));
    panes.add(new BuildVersionPanel(37, 1, labelText, labelText));
    panes.add(new BuildDatePanel(57, 1, labelText, labelText));

    panes.add(new ThreadStatusPanel(0, 2, labelText, valueText));

    panes.add(new TotalTopicsPanel(0, 3, labelText, valueText));
  }

  public void update(Object obj) {
    if (!(obj instanceof ServerInfoDTO)) {
      return;
    }
    ServerInfoDTO statusMessage = (ServerInfoDTO) obj;
    for (ServerDetailsUpdate serverStatusUpdate : panes) {
      serverStatusUpdate.update(statusMessage);
    }
  }
}
