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

package io.mapsmessaging.license.features;

import lombok.Data;

@Data
public class Protocols {
  private boolean mqtt;
  private boolean amqp;
  private boolean nats;
  private boolean mqtt_sn;
  private boolean coap;
  private boolean nmea_0183;
  private boolean semtech;
  private boolean extensions;
  private boolean stomp;
  private boolean rest;
  private boolean lora;
  private boolean ws;
  private boolean wss;
  private boolean stogi;
}
