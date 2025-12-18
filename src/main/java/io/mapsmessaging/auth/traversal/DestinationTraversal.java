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

package io.mapsmessaging.auth.traversal;

import io.mapsmessaging.security.authorisation.ProtectedResource;
import io.mapsmessaging.security.authorisation.ResourceTraversal;

public class DestinationTraversal implements ResourceTraversal {

  private ProtectedResource current;

  public DestinationTraversal(ProtectedResource protectedResource){
    this.current = protectedResource;

  }

  @Override
  public ProtectedResource current() {
    return current;
  }

  @Override
  public boolean hasMore() {
    String resourceId = current.getResourceId();
    return (resourceId != null && !resourceId.isEmpty());
  }

  @Override
  public void moveToParent() {
    String resourceId = current.getResourceId();
    int pos = resourceId.lastIndexOf('/');
    if(pos > 0){ // Walking down
      resourceId = resourceId.substring(0, pos);
    }
    else if(resourceId.length() > 1){
      resourceId = "/";
    }
    else{
      resourceId = "";
    }

    current = new ProtectedResource(
        current.getResourceType(),
        resourceId,
        current.getTenant()
    );

  }
}
