package io.mapsmessaging.app.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.formatters.ByteSizeFormatter;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;

public class FreeMemoryPanel extends ServerStatusUpdate {

  public FreeMemoryPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "Free ", labelText, valueText, new ByteSizeFormatter(7));
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getFreeMemory());
  }
}
