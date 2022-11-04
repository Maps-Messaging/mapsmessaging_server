package io.mapsmessaging.utilities.stats;

import java.util.Map;
import lombok.Getter;

public class LinkedMovingAverageRecord {

  @Getter
  private final String name;
  @Getter
  private final String unitName;
  @Getter
  private final long timeSpan;
  @Getter
  private final long current;

  @Getter
  private final Map<String, Long> stats;

  public LinkedMovingAverageRecord(String name, String unitName, long timeSpan, long current, Map<String, Long> stats ){
    this.name = name;
    this.unitName = unitName;
    this.timeSpan = timeSpan;
    this.current = current;
    this.stats = stats;
  }

}
