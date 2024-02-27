package io.mapsmessaging.monitor.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import io.mapsmessaging.monitor.top.formatters.ByteSizeFormatter;

public class UsedMemoryPanel extends ServerStatusUpdate {

  public UsedMemoryPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "Used ", labelText, valueText, new ByteSizeFormatter());
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getTotalMemory() - statusMessage.getFreeMemory());
  }
}
