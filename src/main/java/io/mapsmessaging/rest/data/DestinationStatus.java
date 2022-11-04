package io.mapsmessaging.rest.data;

import io.mapsmessaging.engine.destination.DestinationImpl;
import io.mapsmessaging.utilities.stats.LinkedMovingAverageRecord;
import java.util.Map;
import lombok.Getter;

public class DestinationStatus {

  @Getter
  private final String name;

  @Getter
  private final Map<String, LinkedMovingAverageRecord> statistics;

  @Getter
  private final Map<String, Map<String, LinkedMovingAverageRecord>> storeageStatistics;

  public DestinationStatus(DestinationImpl destinationImpl) {
    this.name = destinationImpl.getFullyQualifiedNamespace();
    statistics = destinationImpl.getStats().getStatistics();
    if(destinationImpl.getResourceStatistics() != null) {
      storeageStatistics = destinationImpl.getResourceStatistics().getStatistics();
    }
    else{
      storeageStatistics = null;
    }
  }
}