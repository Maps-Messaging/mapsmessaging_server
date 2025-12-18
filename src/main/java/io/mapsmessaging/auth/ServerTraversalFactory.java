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

package io.mapsmessaging.auth;

import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.auth.traversal.DestinationTraversal;
import io.mapsmessaging.auth.traversal.ServerTraversal;
import io.mapsmessaging.security.authorisation.DefaultResourceTraversal;
import io.mapsmessaging.security.authorisation.ProtectedResource;
import io.mapsmessaging.security.authorisation.ResourceTraversal;
import io.mapsmessaging.security.authorisation.ResourceTraversalFactory;

public class ServerTraversalFactory implements ResourceTraversalFactory {

  @Override
  public ResourceTraversal create(ProtectedResource resource){
    for(DestinationType type : DestinationType.values()){
      if(resource.getResourceType().equalsIgnoreCase(type.getName())){
        return new DestinationTraversal(resource);
      }
    }
    if(resource.getResourceType().equalsIgnoreCase("server")){
      return new ServerTraversal(resource);
    }
    return new DefaultResourceTraversal(resource); // No walking, fixed!
  }

}
