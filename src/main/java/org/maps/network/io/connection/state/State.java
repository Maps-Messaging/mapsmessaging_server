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

package org.maps.network.io.connection.state;

import org.maps.network.io.connection.EndPointConnection;

public abstract class State implements Runnable {

  protected final EndPointConnection endPointConnection;
  private final long timeout;

  public State(EndPointConnection connection){
    this.endPointConnection = connection;
    timeout = System.currentTimeMillis() + 60000;
  }

  public boolean hasTimedOut(){
    return timeout < System.currentTimeMillis();
  }

  public void setState(State state){
    endPointConnection.setState(state);
  }

  public abstract void execute();

  public void run(){
    execute();
  }
}
