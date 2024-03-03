package io.mapsmessaging.app.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.formatters.ByteSizeFormatter;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;

public class TotalBytesSentPanel extends ServerStatusUpdate {

  public TotalBytesSentPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "Out: ", labelText, valueText, new ByteSizeFormatter(7));
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getServerStatistics().getTotalWriteBytes());
  }
}