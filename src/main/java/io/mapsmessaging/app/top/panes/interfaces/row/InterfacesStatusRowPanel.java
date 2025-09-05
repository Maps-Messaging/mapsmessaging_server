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

package io.mapsmessaging.app.top.panes.interfaces.row;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.panes.interfaces.InterfacesStatusUpdate;
import io.mapsmessaging.dto.rest.interfaces.InterfaceStatusDTO;

import java.util.ArrayList;
import java.util.List;

public class InterfacesStatusRowPanel {

  private final List<InterfacesStatusUpdate> rowItems;

  public InterfacesStatusRowPanel(int row, TextGraphics labelText, TextGraphics valueText) {
    rowItems = new ArrayList<>();
    rowItems.add(new InterfaceNamePanel(0, row, labelText, valueText));
    rowItems.add(new InterfaceMetricsPanel(32, row, labelText, valueText));
  }

  public void update(InterfaceStatusDTO statusMessage) {
    for (InterfacesStatusUpdate update : rowItems) {
      update.update(statusMessage);
    }
  }
}