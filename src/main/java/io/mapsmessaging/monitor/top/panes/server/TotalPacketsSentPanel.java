package io.mapsmessaging.monitor.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import io.mapsmessaging.monitor.top.formatters.DecimalSizeFormatter;

public class TotalPacketsSentPanel extends ServerStatusUpdate {

  public TotalPacketsSentPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "Out: ", labelText, valueText, new DecimalSizeFormatter(6));
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getServerStatistics().getPacketsSent());
  }
}