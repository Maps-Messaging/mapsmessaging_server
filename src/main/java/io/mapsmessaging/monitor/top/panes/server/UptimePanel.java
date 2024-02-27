package io.mapsmessaging.monitor.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import io.mapsmessaging.monitor.top.formatters.UptimeFormatter;

public class UptimePanel extends ServerStatusUpdate {

  public UptimePanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "Up ", labelText, valueText, new UptimeFormatter(20, false));
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getUptime());
  }
}