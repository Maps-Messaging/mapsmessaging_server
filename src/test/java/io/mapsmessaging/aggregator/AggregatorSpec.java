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

package io.mapsmessaging.aggregator;

import lombok.Data;

@Data
public class AggregatorSpec {

  private final String outputTopic;
  private final String in1;
  private final String in2;
  private final String in3;

  private AggregatorSpec(String outputTopic, String in1, String in2, String in3) {
    this.outputTopic = outputTopic;
    this.in1 = in1;
    this.in2 = in2;
    this.in3 = in3;
  }

  public static AggregatorSpec aggregator1() {
    return new AggregatorSpec(
        "/aggregator1/out1",
        "/aggregator1/in1",
        "/aggregator1/in2",
        "/aggregator1/in3"
    );
  }

  public static AggregatorSpec aggregator2() {
    return new AggregatorSpec(
        "/aggregator2/out1",
        "/aggregator2/in1",
        "/aggregator2/in2",
        "/aggregator2/in3"
    );
  }
}