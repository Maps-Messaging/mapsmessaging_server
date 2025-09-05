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
import io.mapsmessaging.dto.rest.ServerStatisticsDTO;

import java.util.ArrayList;
import java.util.List;

public class ServerStatusPane extends PaneUpdate {

  private final List<ServerStatsUpdate> panes;

  public ServerStatusPane(TextGraphics labelText, TextGraphics valueText) {
    panes = new ArrayList<>();
    panes.add(new TotalPublishedPanel(14, 3, labelText, valueText));
    panes.add(new TotalSubscribedPanel(25, 3, labelText, valueText));
    panes.add(new TotalNoInterestPanel(37, 3, labelText, valueText));
    panes.add(new TotalDeliveredPanel(49, 3, labelText, valueText));
    panes.add(new TotalRetreivedPanel(59, 3, labelText, valueText));

    panes.add(new TotalPacketsReceivedPanel(0, 4, labelText, valueText));
    panes.add(new TotalPacketsSentPanel(20, 4, labelText, valueText));
    panes.add(new TotalBytesReceivedPanel(40, 4, labelText, valueText));
    panes.add(new TotalBytesSentPanel(60, 4, labelText, valueText));
  }

  public void update(Object obj) {
    if (!(obj instanceof ServerStatisticsDTO)) {
      return;
    }
    ServerStatisticsDTO statusMessage = (ServerStatisticsDTO) obj;
    for (ServerStatsUpdate serverStatusUpdate : panes) {
      serverStatusUpdate.update(statusMessage);
    }
  }
}

