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

import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.route.link.Link;
import io.mapsmessaging.network.route.link.LinkId;
import io.mapsmessaging.network.route.link.LinkMetrics;
import io.mapsmessaging.network.route.link.LinkState;
import lombok.Getter;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.OptionalDouble;

@Getter
public class EndPointLink implements Link {

  private final EndPointConnection endPointConnection;

  public EndPointLink(final EndPointConnection endPointConnection) {
    this.endPointConnection = endPointConnection;
  }

  @Override
  public LinkId getLinkId() {
    return new LinkId(endPointConnection.getConfigName());
  }

  @Override
  public URI getRemoteUri() {
    return URI.create(endPointConnection.getConnection().getEndPoint().getConfig().getUrl());
  }

  @Override
  public LinkState getState() {
    return endPointConnection.getState().getLinkState();
  }

  @Override
  public LinkMetrics getMetrics() {
    return new LinkMetrics() {
      @Override
      public OptionalDouble getLatencyMillisEma() {
        return OptionalDouble.empty();
      }

      @Override
      public OptionalDouble getJitterMillisEma() {
        return OptionalDouble.empty();
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
        return OptionalDouble.empty();
      }

      @Override
      public Instant getLastUpdated() {
        return Instant.now();
      }

      @Override
      public Duration getWindow() {
        return Duration.ofSeconds(60);
      }
    };
  }

  @Override
  public OptionalDouble getBaseCost() {
    return OptionalDouble.of(endPointConnection.getProperties().getCost());
  }

  @Override
  public boolean isAvailable() {
    return getState().equals(LinkState.CONNECTED);
  }

  @Override
  public void connect() {
    // will need to do things with subscriptions here
  }

  @Override
  public void disconnect() {
    // will need to do things with subscriptions here
  }

}