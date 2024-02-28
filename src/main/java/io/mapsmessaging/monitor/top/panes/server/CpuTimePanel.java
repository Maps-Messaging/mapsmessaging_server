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

package io.mapsmessaging.monitor.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import io.mapsmessaging.monitor.top.formatters.UptimeFormatter;

public class CpuTimePanel extends ServerStatusUpdate {

  public CpuTimePanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "Cpu ", labelText, valueText, new UptimeFormatter(20, false));
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getCpuTime());
  }
}