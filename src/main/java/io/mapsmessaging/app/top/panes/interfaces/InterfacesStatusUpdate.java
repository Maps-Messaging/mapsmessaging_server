package io.mapsmessaging.app.top.panes.interfaces;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.formatters.Formatter;
import io.mapsmessaging.app.top.panels.Panel;
import io.mapsmessaging.app.top.panels.StringPanel;
import io.mapsmessaging.rest.data.interfaces.InterfaceStatus;

public abstract class InterfacesStatusUpdate {

  protected final Panel panel;

  protected InterfacesStatusUpdate(int row, int col, String label, TextGraphics labelText, TextGraphics valueText, Formatter formatter) {
    panel = new StringPanel(row, col, label, labelText, valueText, formatter);
  }

  public abstract void update(InterfaceStatus statusMessage);

}
