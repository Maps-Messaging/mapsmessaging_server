package io.mapsmessaging.monitor.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import io.mapsmessaging.monitor.top.formatters.StringFormatter;

public class ConnectedPanel extends ServerStatusUpdate {

  public ConnectedPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "Active : ", labelText, valueText, new StringFormatter(6, true));
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getConnections());
  }
}