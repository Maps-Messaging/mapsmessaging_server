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

package io.mapsmessaging.network.protocol.impl.amqp;

import io.mapsmessaging.api.Session;
import java.io.IOException;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

public class SessionManager {

  private final Session session;
  private int interestCount;

  public SessionManager(Session session){
    this.session = session;
    interestCount = 1;
  }

  public int increment(){
    return interestCount++;
  }

  public int decrement(){
    return interestCount--;
  }

  public@NonNull @NotNull Session getSession() {
    return session;
  }

  public void close() throws IOException {
    interestCount = 0;
    io.mapsmessaging.api.SessionManager.getInstance().close(session);
  }
}
