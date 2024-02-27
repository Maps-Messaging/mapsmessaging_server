package io.mapsmessaging.monitor.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import io.mapsmessaging.monitor.top.formatters.DecimalSizeFormatter;
import io.mapsmessaging.monitor.top.formatters.SizeFormatter;

public class TotalNoInterestPanel extends ServerStatusUpdate {

  public TotalNoInterestPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "NoInt: ", labelText, valueText, new DecimalSizeFormatter());
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getServerStatistics().getTotalNoInterestMessages());
  }
}