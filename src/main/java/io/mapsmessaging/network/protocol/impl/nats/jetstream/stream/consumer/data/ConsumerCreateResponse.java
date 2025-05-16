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

package io.mapsmessaging.network.protocol.impl.nats.jetstream.stream.consumer.data;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

@Data
public class ConsumerCreateResponse {
  private String type = "io.nats.jetstream.api.v1.consumer_create_response";
  private String stream_name;
  private String name;
  private Instant created;
  private ConsumerConfig config;
  private DeliveryInfo delivered;
  private AckFloor ack_floor;
  private long num_ack_pending;
  private long num_redelivered;
  private long num_waiting;
  private long num_pending;
  private Instant ts;

  @Data
  @AllArgsConstructor
  public static class DeliveryInfo {
    private long consumer_seq;
    private long stream_seq;
  }

  @Data
  @AllArgsConstructor
  public static class AckFloor {
    private long consumer_seq;
    private long stream_seq;
  }
}
