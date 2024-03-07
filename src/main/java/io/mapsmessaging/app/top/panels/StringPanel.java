package io.mapsmessaging.app.top.panels;

import com.googlecode.lanterna.graphics.TextGraphics;
import io.mapsmessaging.app.top.formatters.Formatter;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;

@Getter
public class StringPanel extends Panel {
  private final Formatter formatter;

  public StringPanel(int row, int column, String label, @NotNull TextGraphics labelText, @NotNull TextGraphics valueText, @NotNull Formatter formatter) {
    super(row, column, label, labelText, valueText);
    this.formatter = formatter;
  }

  public void update(Object update) {
    super.draw(formatter.format(update));
  }
}
