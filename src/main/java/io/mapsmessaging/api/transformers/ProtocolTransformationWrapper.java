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

package io.mapsmessaging.api.transformers;

import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.network.protocol.Protocol;
import io.mapsmessaging.network.protocol.transformation.ProtocolMessageTransformation;

public class ProtocolTransformationWrapper implements InterServerTransformation {


  private final ProtocolMessageTransformation  protocolMessageTransformation;

  public ProtocolTransformationWrapper(ProtocolMessageTransformation protocolMessageTransformation) {
    this.protocolMessageTransformation = protocolMessageTransformation;
  }

  @Override
  public Protocol.ParsedMessage transform(String source, Protocol.ParsedMessage message) {

    message.setMessage(protocolMessageTransformation.outgoing(message.getMessage(), source));
    return message;
  }

  @Override
  public InterServerTransformation build(ConfigurationProperties properties) {
    return null;
  }

  @Override
  public String getName() {
    return protocolMessageTransformation.getName();
  }

  @Override
  public String getDescription() {
    return protocolMessageTransformation.getDescription();
  }
}
