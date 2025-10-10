/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.network.io.connection.route;

import io.mapsmessaging.network.io.EndPoint;
import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.route.link.LinkMetrics;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalDouble;

public class Metrics implements LinkMetrics {
  private final EndPointConnection endPointConnection;
  private long lastUpdate;
  private long lastThroughput;

  public Metrics(EndPointConnection endPointConnection) {
    this.endPointConnection = endPointConnection;
  }

  @Override
  public OptionalDouble getLatencyMillisEma() {
    return OptionalDouble.of(0.0);
  }

  @Override
  public OptionalDouble getJitterMillisEma() {
    return OptionalDouble.of(0.0);
  }

  @Override
  public double getLossRatio() {
    return 0;
  }

  @Override
  public double getErrorRate() {
    return 0;
  }

  @Override
  public int getOutboundQueueDepth() {

    return 0;
  }

  @Override
  public OptionalDouble getThroughputMibPerSecond() {
    EndPoint endPoint = getEndPoint();
    if(endPoint == null){
      return OptionalDouble.empty();
    }

    long total = endPoint.getEndPointStatus().getReadBytesTotal() +endPoint.getEndPointStatus().getWriteBytesTotal();
    if(lastUpdate == 0){
      lastUpdate = System.currentTimeMillis();
      lastThroughput = total;
    }
    long throughput = (total - lastThroughput) / ((System.currentTimeMillis() - lastUpdate)/1000);
    lastThroughput = System.currentTimeMillis();
    return OptionalDouble.of(throughput);
  }

  @Override
  public Instant getLastUpdated() {
    if(getEndPoint() == null){
      return Instant.now();
    }
    EndPoint endPoint = getEndPoint();
    long last = Math.max(endPoint.getLastRead(), endPoint.getLastWrite());
    return Instant.ofEpochSecond(last);
  }

  @Override
  public Duration getWindow() {
    return Duration.ofSeconds(2);
  }

  private EndPoint getEndPoint() {
    if(endPointConnection.getConnection() == null){
      return null;
    }
    return endPointConnection.getConnection().getEndPoint();
  }
}
