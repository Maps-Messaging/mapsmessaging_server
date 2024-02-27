package io.mapsmessaging.monitor.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import io.mapsmessaging.monitor.top.formatters.ByteSizeFormatter;
import io.mapsmessaging.monitor.top.formatters.DecimalSizeFormatter;

public class TotalBytesReceivedPanel extends ServerStatusUpdate {

  public TotalBytesReceivedPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "Bytes In: ", labelText, valueText, new ByteSizeFormatter(7));
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getServerStatistics().getTotalReadBytes());
  }
}