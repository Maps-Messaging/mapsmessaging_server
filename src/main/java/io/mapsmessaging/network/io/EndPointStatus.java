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

import io.mapsmessaging.utilities.stats.*;

import java.util.concurrent.TimeUnit;

public class EndPointStatus {


  private static final String MESSAGES = "Messages";
  private static final String PACKETS = "Packets";
  private static final String BYTES = "Bytes";


  private final Stats readByteAverages;
  private final Stats writeByteAverages;
  private final Stats bufferOverFlow;
  private final Stats bufferUnderFlow;

  private final Stats sentMessageAverages;
  private final Stats receivedMessageAverages;


  public EndPointStatus(StatsType type){
    readByteAverages = create( type, MovingAverageFactory.ACCUMULATOR.ADD, "Read Bytes", BYTES);
    writeByteAverages = create( type,MovingAverageFactory.ACCUMULATOR.ADD, "Write Bytes",  BYTES);
    bufferOverFlow = create( type,MovingAverageFactory.ACCUMULATOR.ADD, "Buffer Overflow", PACKETS);
    bufferUnderFlow = create( type,MovingAverageFactory.ACCUMULATOR.ADD, "Buffer Underflow", PACKETS);
    sentMessageAverages = create( type,MovingAverageFactory.ACCUMULATOR.ADD, "Sent Packets", MESSAGES);
    receivedMessageAverages = create( type,MovingAverageFactory.ACCUMULATOR.ADD, "Received Packets", MESSAGES);
  }


  private Stats create(StatsType type, MovingAverageFactory.ACCUMULATOR accumulator, String name, String units) {
    return StatsFactory.create(type, name, units, accumulator, 1, 5, 4, TimeUnit.MINUTES);
  }


  public void updateReadBytes(int read) {
    readByteAverages.add(read);
  }


  public void updateWriteBytes(int wrote) {
    writeByteAverages.add(wrote);
  }


  public long getReadBytesTotal() {
    return readByteAverages.getTotal();
  }


  public long getWriteBytesTotal() {
    return writeByteAverages.getTotal();
  }


  public long getOverFlowTotal() {
    return bufferOverFlow.getTotal();
  }


  public long getUnderFlowTotal() {
    return bufferUnderFlow.getTotal();
  }


  public void incrementOverFlow() {
    bufferOverFlow.increment();
  }


  public void incrementUnderFlow() {
    bufferUnderFlow.increment();
  }


  public boolean supportsMovingAverages() {
    return bufferOverFlow instanceof LinkedMovingAverages;
  }


  public void incrementReceivedMessages() {
    receivedMessageAverages.increment();
  }


  public void incrementSentMessages() {
    sentMessageAverages.increment();
  }


  public long getReceivedMessagesTotal() {
    return receivedMessageAverages.getTotal();
  }


  public long getSentMessagesTotal() {
    return sentMessageAverages.getTotal();
  }


  public LinkedMovingAverages getBufferOverFlow() {
    return supportsMovingAverages() ? (LinkedMovingAverages)bufferOverFlow : null;
  }


  public LinkedMovingAverages getBufferUnderFlow() {
    return supportsMovingAverages() ? (LinkedMovingAverages)bufferUnderFlow : null;
  }


  public LinkedMovingAverages getReceivedMessages() {
    return supportsMovingAverages() ? (LinkedMovingAverages)receivedMessageAverages : null;
  }


  public LinkedMovingAverages getSentMessages() {
    return supportsMovingAverages() ? (LinkedMovingAverages)sentMessageAverages : null;
  }


  public LinkedMovingAverages getReadByteAverages() {
    return supportsMovingAverages() ? (LinkedMovingAverages)readByteAverages : null;
  }


  public LinkedMovingAverages getWriteByteAverages() {
    return supportsMovingAverages() ? (LinkedMovingAverages)writeByteAverages : null;
  }
}
