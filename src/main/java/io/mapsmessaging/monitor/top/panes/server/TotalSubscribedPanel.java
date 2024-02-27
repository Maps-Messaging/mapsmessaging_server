package io.mapsmessaging.monitor.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import io.mapsmessaging.monitor.top.formatters.ByteSizeFormatter;
import io.mapsmessaging.monitor.top.formatters.DecimalSizeFormatter;
import io.mapsmessaging.monitor.top.formatters.SizeFormatter;

public class TotalSubscribedPanel extends ServerStatusUpdate {

  public TotalSubscribedPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "Sub: ", labelText, valueText, new DecimalSizeFormatter());
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getServerStatistics().getTotalSubscribedMessages());
  }
}