package io.mapsmessaging.monitor.top.panes.server;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.engine.system.impl.server.StatusMessage;
import io.mapsmessaging.monitor.top.formatters.StringFormatter;

import java.time.LocalDateTime;

public class ServerNamePanel extends ServerStatusUpdate {

  public ServerNamePanel(int row, int col, TextGraphics labelText, TextGraphics valueText) {
    super(row, col, "", labelText, valueText, new StringFormatter(80, false));
  }

  @Override
  public void update(StatusMessage statusMessage) {
    LocalDateTime dateTime = LocalDateTime.now();
    String value = String.format("%02d:%02d:%02d Server:%s", dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond(), statusMessage.getServerName());
    panel.update(value);
  }
}
