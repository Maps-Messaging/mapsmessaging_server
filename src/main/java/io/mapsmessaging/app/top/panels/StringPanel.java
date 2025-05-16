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

package io.mapsmessaging.app.top.panels;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.formatters.Formatter;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class StringPanel extends Panel {
  private final Formatter formatter;

  public StringPanel(int row, int column, String label, @NotNull TextGraphics labelText, @NotNull TextGraphics valueText, @NotNull Formatter formatter) {
    super(row, column, label, labelText, valueText);
    this.formatter = formatter;
  }

  public void update(Object update) {
    super.draw(formatter.format(update));
  }
}
