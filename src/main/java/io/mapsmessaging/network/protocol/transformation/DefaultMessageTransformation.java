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

package io.mapsmessaging.network.protocol.transformation;

import io.mapsmessaging.network.protocol.ProtocolMessageTransformation;

public class DefaultMessageTransformation implements ProtocolMessageTransformation {

  public DefaultMessageTransformation() {
    // Used by the java services
  }

  @Override
  public int getId() {
    return 2;
  }


  @Override
  public String getName() {
    return "Default";
  }

  @Override
  public String getDescription() {
    return "Default Transformation, does nothing to the messages";
  }

}
