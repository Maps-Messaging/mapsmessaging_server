/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.network.protocol.impl.mqtt5.packet;

public abstract class StatusPacket extends MQTTPacket5 {

  protected StatusCode statusCode;

  protected StatusPacket(int id) {
    super(id);
    statusCode = StatusCode.SUCCESS;
  }

  public StatusCode getStatusCode() {
    return statusCode;
  }

  public void setStatusCode(StatusCode response) {
    this.statusCode = response;
  }

  @Override
  public String toString() {
    return "Response:" + statusCode.toString();
  }
}
