package io.mapsmessaging.monitor.top.panels;

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
