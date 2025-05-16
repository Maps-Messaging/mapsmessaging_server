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
import com.google.gson.annotations.SerializedName;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class ConsumerConfig {
  @SerializedName("deliver_subject")
  private String deliverSubject;
  @SerializedName("filter_subject")
  private String filterSubject;
  private String name;

  @SerializedName("ack_wait")
  private long ackWait =30000000000L;
  @SerializedName("max_waiting")
  private int maxWaiting=512;

  @SerializedName("inactive_threshold")
  private long inactiveThreshold = 5000000000L;

  @SerializedName("num_replicas")
  private int numReplicas = 0;

  @SerializedName("durable_name")
  private String durableName;
  @SerializedName("ack_policy")
  private AckPolicy ackPolicy;
  @SerializedName("deliver_policy")
  private DeliverPolicy deliverPolicy;
  @SerializedName("replay_policy")
  private ReplayPolicy replayPolicy;
  @SerializedName("flow_control")
  private Boolean flowControl;
  @SerializedName("max_ack_pending")
  private Integer maxAckPending;
  @SerializedName("max_deliver")
  private int maxDeliver;

  private Map<String, String> metadata = buildMetadata();


  private static Map<String, String> buildMetadata() {
    Map<String, String> metadata = new HashMap<>();
    metadata.put("_nats.level", "1");
    metadata.put("_nats.req.level", "0");
    metadata.put("_nats.ver", "2.11.3");

    return metadata;
  }

}
