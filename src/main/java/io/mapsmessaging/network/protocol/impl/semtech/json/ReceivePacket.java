/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.network.protocol.impl.semtech.json;

import lombok.Getter;
import lombok.Setter;

public class ReceivePacket {

  @Getter
  @Setter
  private String time;
  @Getter
  @Setter
  private long tmms;
  @Getter
  @Setter
  private long tmst;
  @Getter
  @Setter
  private double freq;
  @Getter
  @Setter
  private long chan;
  @Getter
  @Setter
  private long rfch;
  @Getter
  @Setter
  private long stat;
  @Getter
  @Setter
  private String modu;
  @Getter
  @Setter
  private String datr_s;
  @Getter
  @Setter
  private long datr;
  @Getter
  @Setter
  private String codr;
  @Getter
  @Setter
  private long rssi;
  @Getter
  @Setter
  private double lsnr;
  @Getter
  @Setter
  private long size;
  @Getter
  @Setter
  private String data;
}

