/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2024 ] [Maps Messaging]
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
import io.mapsmessaging.app.top.panes.destination.DestinationStatusUpdate;
import io.mapsmessaging.dto.rest.destination.DestinationStatusDTO;
import java.util.ArrayList;
import java.util.List;

public class DestinationStatusRowPanel {

  private final List<DestinationStatusUpdate> rowItems;
  public DestinationStatusRowPanel(int row, TextGraphics labelText, TextGraphics valueText){
    rowItems = new ArrayList<>();
    rowItems.add(new DestinationNamePanel(0, row, labelText, valueText));
    rowItems.add(new DestinationMetricsPanel(16, row, labelText, valueText));
    rowItems.add(new DestinationDiskPanel(55, row, labelText, valueText));

  }

  public void update(DestinationStatusDTO statusMessage){
    for(DestinationStatusUpdate update:rowItems){
      update.update(statusMessage);
    }
  }
}