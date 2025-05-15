/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.network.io.impl.udp.session;

import io.mapsmessaging.network.io.Timeoutable;
import lombok.Getter;
import lombok.Setter;

public class UDPSessionState<T extends Timeoutable> {

  @Getter
  @Setter
  private String clientIdentifier;

  private T context;

  @Getter
  private long getLastAccess;


  public UDPSessionState(T context) {
    this.context = context;
    getLastAccess = System.currentTimeMillis();
  }

  public void updateTimeout() {
    getLastAccess = System.currentTimeMillis();
  }

  public T getContext() {
    return context;
  }

  public void setContext(T context) {
    this.context = context;
  }

}
