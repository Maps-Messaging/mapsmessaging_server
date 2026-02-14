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

package io.mapsmessaging.api.transformers;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.network.protocol.Protocol;

import java.util.List;

public class InterServerPipelineTransformation implements InterServerTransformation {

  private final List<InterServerTransformation> pipeline;

  public InterServerPipelineTransformation(List<InterServerTransformation> pipeline){
    this.pipeline = pipeline;
  }

  @Override
  public ParsedMessage transform(String source, ParsedMessage message) {
    long processingTime = System.nanoTime();
    String init = source;
    for(InterServerTransformation transform: pipeline){
      message = transform.transform(init, message);
      init = message.getDestinationName();
    }
    processingTime = System.nanoTime() - processingTime;
    float ms = processingTime/1000000f;
    if(ms > 1.0f ){
    //  System.err.println("Transformers taking too long "+ms);
    }
    return message;
  }

  @Override
  public InterServerTransformation build(ConfigurationProperties properties) {
    return null;
  }

  @Override
  public String getName() {
    return "Pipeline";
  }

  @Override
  public String getDescription() {
    return "Internal pipeline transformer";
  }
}
