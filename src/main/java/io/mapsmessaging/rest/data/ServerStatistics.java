package io.mapsmessaging.rest.data;

import io.mapsmessaging.engine.destination.DestinationStats;
import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.EndPointServerStatus;
import io.mapsmessaging.utilities.stats.LinkedMovingAverageRecord;
import java.util.Map;
import lombok.Getter;

public class ServerStatistics {

  @Getter
  private final long packetsSent;
  @Getter
  private final long packetsReceived;
  @Getter
  public final long totalReadBytes;
  @Getter
  public final long totalWriteBytes;
  @Getter
  public final long totalConnections;
  @Getter
  public final long totalDisconnections;
  @Getter
  private final long totalNoInterestMessages;
  @Getter
  private final long totalSubscribedMessages;
  @Getter
  private final long totalPublishedMessages;
  @Getter
  private final long totalRetrievedMessages;
  @Getter
  private final long totalExpiredMessages;
  @Getter
  private final long totalDeliveredMessages;

  @Getter
  private final Map<String, LinkedMovingAverageRecord> stats;


  public ServerStatistics() {
    packetsReceived = EndPointServerStatus.SystemTotalPacketsReceived.sum();
    packetsSent = EndPointServerStatus.SystemTotalPacketsSent.sum();

    totalConnections = EndPoint.totalConnections.sum();
    totalReadBytes = EndPoint.totalReadBytes.sum();
    totalWriteBytes = EndPoint.totalWriteBytes.sum();
    totalDisconnections = EndPoint.totalDisconnections.sum();

    totalNoInterestMessages = DestinationStats.getTotalNoInterestMessages();
    totalSubscribedMessages = DestinationStats.getTotalSubscribedMessages();
    totalPublishedMessages = DestinationStats.getTotalPublishedMessages();
    totalRetrievedMessages = DestinationStats.getTotalExpiredMessages();
    totalDeliveredMessages = DestinationStats.getTotalDeliveredMessages();
    totalExpiredMessages = DestinationStats.getTotalExpiredMessages();
    stats = DestinationStats.getGlobalStats();
  }
}
