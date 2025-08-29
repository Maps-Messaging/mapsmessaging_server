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

package io.mapsmessaging.network.protocol.impl.satellite.ogx;


import io.mapsmessaging.network.protocol.impl.satellite.idp.MoEntry;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.checkerframework.checker.units.qual.N;

@Getter
@Setter
public class OgxMoEntry extends MoEntry {
  private final int serviceClass;     // from %MOMT
  private final int lifetimeMins;     // from %MOMT
  private final int type;             // 1 if length <= 1024, else 2
  private String utcFirstSeen;        // "yyyy-MM-dd HH:mm:ss" UTC
  private int state = TX_SENDING; // start at 4 (sending)
  private int closed = 0;             // 0 open, 1 final (when completed)
  private int bytesAck = 0;           // progress to length
  private boolean completedEmittedOnce = false;

  public OgxMoEntry(int messageNo, int serviceClass, int lifetimeMins, int length) {
    super(messageNo, length);
    this.serviceClass = serviceClass;
    this.lifetimeMins = lifetimeMins;
    this.type = (length <= 1024) ? 1 : 2;
  }
}