/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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
  private static final double BYTES_PER_MIB = 1024.0 * 1024.0;

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
    if (endPoint == null) {
      return OptionalDouble.empty();
    }

    long now = System.currentTimeMillis();
    long total = endPoint.getEndPointStatus().getReadBytesTotal() + endPoint.getEndPointStatus().getWriteBytesTotal();
    if (lastUpdate == 0) {
      lastUpdate = now;
      lastThroughput = total;
      return OptionalDouble.empty();
    }

    long elapsedMillis = now - lastUpdate;
    if (elapsedMillis <= 0) {
      return OptionalDouble.empty();
    }

    long bytesTransferred = total - lastThroughput;
    lastUpdate = now;
    lastThroughput = total;

    double elapsedSeconds = elapsedMillis / 1000.0;
    return OptionalDouble.of((bytesTransferred / BYTES_PER_MIB) / elapsedSeconds);
  }

  @Override
  public Instant getLastUpdated() {
    if (getEndPoint() == null) {
      return Instant.now();
    }
    EndPoint endPoint = getEndPoint();
    long last = Math.max(endPoint.getLastRead(), endPoint.getLastWrite());
    return Instant.ofEpochMilli(last);
  }

  @Override
  public Duration getWindow() {
    return Duration.ofSeconds(2);
  }

  private EndPoint getEndPoint() {
    if (endPointConnection.getProtocol() == null) {
      return null;
    }
    return endPointConnection.getProtocol().getEndPoint();
  }
}
