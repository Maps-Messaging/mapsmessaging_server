package io.mapsmessaging.app.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.formatters.StringFormatter;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;

import java.time.LocalDateTime;

public class TimePanel extends ServerStatusUpdate {

  public TimePanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "", labelText, valueText, new StringFormatter(10, false));
  }

  @Override
  public void update(StatusMessage statusMessage) {
    LocalDateTime dateTime = LocalDateTime.now();
    panel.update(String.format("%02d:%02d:%02d", dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond()));
  }
}
