package io.mapsmessaging.app.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.formatters.Formatter;
import io.mapsmessaging.app.top.panels.StringPanel;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import io.mapsmessaging.app.top.panels.Panel;

public abstract class ServerStatusUpdate {

  protected final Panel panel;

  protected ServerStatusUpdate(int row, int col, String label, TextGraphics labelText, TextGraphics valueText, Formatter formatter) {
    panel = new StringPanel(row, col, label, labelText, valueText, formatter);
  }

  public abstract void update(StatusMessage statusMessage);

}
