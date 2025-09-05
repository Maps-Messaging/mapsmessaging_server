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

package io.mapsmessaging.network.io;

import io.mapsmessaging.dto.rest.config.network.EndPointServerConfigDTO;
import io.mapsmessaging.dto.rest.stats.LinkedMovingAverageRecordDTO;
import io.mapsmessaging.network.EndPointURL;
import io.mapsmessaging.utilities.stats.*;
import lombok.Getter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;

public abstract class EndPointServerStatus {

  public static final LongAdder SystemTotalPacketsSent = new LongAdder();
  public static final LongAdder SystemTotalPacketsReceived = new LongAdder();
  public static final LongAdder SystemTotalBytesReceived = new LongAdder();
  public static final LongAdder SystemTotalBytesSent = new LongAdder();
  public static final LongAdder SystemTotalFailedConnections = new LongAdder();

  private final LongAdder totalErrors;
  private final LongAdder totalPacketsSent;
  private final LongAdder totalPacketsRead;
  private final LongAdder totalBytesSent;
  private final LongAdder totalBytesRead;

  private final Stats averageBytesSent;
  private final Stats averageBytesRead;
  private final Stats averagePacketsSent;
  private final Stats averagePacketsRead;

  @Getter
  protected final EndPointURL url;

  protected EndPointServerStatus(EndPointURL url, StatsType type) {
    this.url = url;
    totalPacketsSent = new LongAdder();
    totalPacketsRead = new LongAdder();
    totalBytesSent = new LongAdder();
    totalBytesRead = new LongAdder();
    totalErrors = new LongAdder();
    averageBytesSent = StatsFactory.create(type, "Total Sent Bytes",  "Bytes",  MovingAverageFactory.ACCUMULATOR.ADD, 1, 5, 4, TimeUnit.MINUTES);
    averageBytesRead = StatsFactory.create(type, "Total Read Bytes", "Bytes", MovingAverageFactory.ACCUMULATOR.ADD,1, 5, 4, TimeUnit.MINUTES );
    averagePacketsSent = StatsFactory.create(type, "Total Sent Packets", "Packets", MovingAverageFactory.ACCUMULATOR.ADD,1, 5, 4, TimeUnit.MINUTES );
    averagePacketsRead = StatsFactory.create(type, "Total Read Packets","Packets",  MovingAverageFactory.ACCUMULATOR.ADD,1, 5, 4, TimeUnit.MINUTES);
  }

  public abstract EndPointServerConfigDTO getConfig();

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
    SystemTotalBytesReceived.add(count);
  }

  public void updateBytesRead(int count) {
    totalBytesRead.add(count);
    averageBytesRead.add(count);
    SystemTotalBytesSent.add(count);
  }


  public void incrementError() {
    totalErrors.increment();
  }

  public long getTotalErrors() {
    return totalErrors.sum();
  }

  public float getBytesSentPerSecond(){
    return averageBytesSent.getPerSecond();
  }


  public float getBytesReadPerSecond(){
    return averageBytesRead.getPerSecond();
  }

  public float getMessagesSentPerSecond(){
    return averagePacketsSent.getPerSecond();
  }

  public float getMessagesReadPerSecond(){
    return averagePacketsRead.getPerSecond();
  }

  public LinkedMovingAverageRecordDTO getAverageBytesSent() {
    return averageBytesSent.supportMovingAverage()? ((LinkedMovingAverages)averageBytesSent).getRecord():null;
  }

  public LinkedMovingAverageRecordDTO getAverageBytesRead() {
    return averageBytesRead.supportMovingAverage()? ((LinkedMovingAverages)averageBytesSent).getRecord():null;
  }

  public LinkedMovingAverageRecordDTO getAveragePacketsSent() {
    return averagePacketsSent.supportMovingAverage()? ((LinkedMovingAverages)averageBytesSent).getRecord():null;
  }

  public LinkedMovingAverageRecordDTO getAveragePacketsRead() {
    return averagePacketsRead.supportMovingAverage()? ((LinkedMovingAverages)averageBytesSent).getRecord():null;
  }

}

