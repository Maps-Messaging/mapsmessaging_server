package io.mapsmessaging.app.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.formatters.UptimeFormatter;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;

public class UptimePanel extends ServerStatusUpdate {

  public UptimePanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "Up ", labelText, valueText, new UptimeFormatter(20, false));
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getUptime());
  }
}