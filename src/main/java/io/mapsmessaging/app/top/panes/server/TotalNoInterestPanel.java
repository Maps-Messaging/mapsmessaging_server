package io.mapsmessaging.app.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import io.mapsmessaging.app.top.formatters.DecimalSizeFormatter;

public class TotalNoInterestPanel extends ServerStatusUpdate {

  public TotalNoInterestPanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "NoInt: ", labelText, valueText, new DecimalSizeFormatter(6));
  }

  @Override
  public void update(StatusMessage statusMessage) {
    panel.update(statusMessage.getServerStatistics().getTotalNoInterestMessages());
  }
}