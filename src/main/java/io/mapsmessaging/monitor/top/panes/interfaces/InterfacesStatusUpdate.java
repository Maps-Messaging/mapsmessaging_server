package io.mapsmessaging.monitor.top.panes.interfaces;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.monitor.top.formatters.Formatter;
import io.mapsmessaging.monitor.top.panels.Panel;
import io.mapsmessaging.monitor.top.panels.StringPanel;
import io.mapsmessaging.rest.data.interfaces.InterfaceStatus;

public abstract class InterfacesStatusUpdate {

  protected final Panel panel;

  protected InterfacesStatusUpdate(int row, int col, String label, TextGraphics labelText, TextGraphics valueText, Formatter formatter) {
    panel = new StringPanel(row, col, label, labelText, valueText, formatter);
  }

  public abstract void update(InterfaceStatus statusMessage);

}
