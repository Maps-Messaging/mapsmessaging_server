package io.mapsmessaging.app.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import io.mapsmessaging.app.top.formatters.DecimalSizeFormatter;

public class TotalTopicsPanel extends ServerStatusUpdate {

  public TotalTopicsPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "Topics : ", labelText, valueText, new DecimalSizeFormatter(7));
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getDestinations());
  }
}