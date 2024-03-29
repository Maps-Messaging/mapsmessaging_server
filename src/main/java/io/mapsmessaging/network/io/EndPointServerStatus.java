/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.network.io;

import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.network.NetworkConfig;
import io.mapsmessaging.utilities.stats.LinkedMovingAverageRecord;
import io.mapsmessaging.utilities.stats.LinkedMovingAverages;
import io.mapsmessaging.utilities.stats.MovingAverageFactory;
import lombok.Getter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public abstract class EndPointServerStatus {

  public static final LongAdder SystemTotalPacketsSent = new LongAdder();
  public static final LongAdder SystemTotalPacketsReceived = new LongAdder();

  private final LongAdder totalErrors;
  private final LongAdder totalPacketsSent;
  private final LongAdder totalPacketsRead;
  private final LongAdder totalBytesSent;
  private final LongAdder totalBytesRead;

  private final LinkedMovingAverages averageBytesSent = MovingAverageFactory.getInstance().createLinked(MovingAverageFactory.ACCUMULATOR.ADD, "Total Sent Bytes", 1, 5, 4, TimeUnit.MINUTES, "Bytes");
  private final LinkedMovingAverages averageBytesRead = MovingAverageFactory.getInstance().createLinked(MovingAverageFactory.ACCUMULATOR.ADD, "Total Read Bytes", 1, 5, 4, TimeUnit.MINUTES, "Bytes");
  private final LinkedMovingAverages averagePacketsSent = MovingAverageFactory.getInstance().createLinked(MovingAverageFactory.ACCUMULATOR.ADD, "Total Sent Packets", 1, 5, 4, TimeUnit.MINUTES, "Packets");
  private final LinkedMovingAverages averagePacketsRead = MovingAverageFactory.getInstance().createLinked(MovingAverageFactory.ACCUMULATOR.ADD, "Total Read Packets", 1, 5, 4, TimeUnit.MINUTES, "Packets");

  @Getter
  protected final EndPointURL url;

  protected EndPointServerStatus(EndPointURL url) {
    this.url = url;
    totalPacketsSent = new LongAdder();
    totalPacketsRead = new LongAdder();
    totalBytesSent = new LongAdder();
    totalBytesRead = new LongAdder();
    totalErrors = new LongAdder();
  }

  public abstract NetworkConfig getConfig();

  public abstract void handleNewEndPoint(EndPoint endPoint) throws IOException;

  public abstract void handleCloseEndPoint(EndPoint endPoint);

  public long getTotalPacketsRead() {
    return totalPacketsRead.sum();
  }

  public long getTotalPacketsSent() {
    return totalPacketsSent.sum();
  }

  public long getTotalBytesSent() {
    return totalBytesSent.sum();
  }

  public long getTotalBytesRead() {
    return totalBytesRead.sum();
  }

  public void incrementPacketsSent() {
    totalPacketsSent.increment();
    SystemTotalPacketsSent.increment();
    averagePacketsSent.increment();
  }

  public void incrementPacketsRead() {
    totalPacketsRead.increment();
    SystemTotalPacketsReceived.increment();
    averagePacketsRead.increment();
  }

  public void updateBytesSent(int count) {
    totalBytesSent.add(count);
    averageBytesSent.add(count);
  }

  public void updateBytesRead(int count) {
    totalBytesRead.add(count);
    averageBytesRead.add(count);
  }

  public LinkedMovingAverageRecord getAverageBytesSent() {
    return averageBytesSent.getRecord();
  }

  public LinkedMovingAverageRecord getAverageBytesRead() {
    return averageBytesRead.getRecord();
  }

  public LinkedMovingAverageRecord getAveragePacketsSent() {
    return averagePacketsSent.getRecord();
  }

  public LinkedMovingAverageRecord getAveragePacketsRead() {
    return averagePacketsRead.getRecord();
  }

  public void incrementError() {
    totalErrors.increment();
  }

  public long getTotalErrors() {
    return totalErrors.sum();
  }
}

