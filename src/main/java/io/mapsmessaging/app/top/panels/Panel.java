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

public abstract class Panel {

  private final TextGraphics labelText;
  private final TextGraphics valueText;
  private final int row;
  private final int column;
  private final String label;

  protected Panel(int row, int column, String label, TextGraphics labelText, TextGraphics valueText) {
    this.row = row;
    this.column = column;
    this.labelText = labelText;
    this.valueText = valueText;
    this.label = label;
  }

  public abstract void update(Object object);

  protected void draw(String value) {
    labelText.putString(row, column, label);
    valueText.putString(row + label.length(), column, value);
  }

}
