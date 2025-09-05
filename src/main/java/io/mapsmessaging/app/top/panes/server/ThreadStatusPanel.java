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

package io.mapsmessaging.app.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.formatters.StringFormatter;
import io.mapsmessaging.app.top.panels.Panel;
import io.mapsmessaging.dto.rest.ServerInfoDTO;

import java.util.Map;

public class ThreadStatusPanel extends ServerDetailsUpdate {

  private final Panel[] values = new Panel[6];

  public ThreadStatusPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "Threads ", labelText, valueText, new StringFormatter(10, true));
    values[0] = new ThreadStatePanel(row + 8, col, "run:", labelText, valueText);
    values[1] = new ThreadStatePanel(row + 16, col, "wait:", labelText, valueText);
    values[2] = new ThreadStatePanel(row + 25, col, "time:", labelText, valueText);
    values[3] = new ThreadStatePanel(row + 34, col, "new:", labelText, valueText);
    values[4] = new ThreadStatePanel(row + 43, col, "block:", labelText, valueText);
    values[5] = new ThreadStatePanel(row + 52, col, "term:", labelText, valueText);
  }

  @Override
  public void update(ServerInfoDTO statusMessage) {
    panel.update("");
    Map<String, Integer> map = statusMessage.getThreadState();
    values[0].update(map.getOrDefault("RUNNABLE", 0));
    values[1].update(map.getOrDefault("WAITING", 0));
    values[2].update(map.getOrDefault("TIMED_WAITING", 0));
    values[3].update(map.getOrDefault("NEW", 0));
    values[4].update(map.getOrDefault("BLOCKED", 0));
    values[5].update(map.getOrDefault("TERMINATED", 0));
  }

  private static class ThreadStatePanel extends Panel {

    protected ThreadStatePanel(int row, int column, String label, TextGraphics labelText, TextGraphics valueText) {
      super(row, column, label, labelText, valueText);
    }

    @Override
    public void update(Object object) {
      draw(object.toString());
    }
  }

}