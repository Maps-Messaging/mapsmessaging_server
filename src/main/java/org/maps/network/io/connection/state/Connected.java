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

import java.util.List;
import org.maps.network.io.connection.EndPointConnection;
import org.maps.utilities.configuration.ConfigurationProperties;

public class Connected extends State {

  public Connected(EndPointConnection connection) {
    super(connection);
  }

  @Override
  public void execute() {
    try {
      List<ConfigurationProperties> properties = endPointConnection.getDestinationMappings();
      for(ConfigurationProperties property:properties) {
        String direction = property.getProperty("direction");
        String local =  property.getProperty("local_namespace");
        String remote =  property.getProperty("remote_namespace");
        if (direction.equalsIgnoreCase("pull")) {
          endPointConnection.getConnection().subscribeRemote(remote, local);
        } else if (direction.equalsIgnoreCase("push")) {
          endPointConnection.getConnection().subscribeLocal(local, remote);
        }
      }
      setState(new Established(endPointConnection));
    }
    catch (Exception exception){
      exception.printStackTrace();
      endPointConnection.close();
      setState(new Disconnected(endPointConnection));
    }
  }

}
