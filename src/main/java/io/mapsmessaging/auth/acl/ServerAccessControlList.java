/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.auth.acl;

import io.mapsmessaging.security.access.AccessControlList;
import io.mapsmessaging.security.access.AccessControlListParser;
import io.mapsmessaging.security.access.AccessControlMapping;
import io.mapsmessaging.security.access.AclEntry;
import java.util.List;

public class ServerAccessControlList extends BaseAccessControlList {

  public ServerAccessControlList() {
  }

  public ServerAccessControlList(List<AclEntry> aclEntries) {
    super(aclEntries);
  }

  @Override
  public String getName() {
    return "ServerACL";
  }

  @Override
  public AccessControlList create(AccessControlMapping accessControlMapping, List<String> config) {
    AccessControlListParser parser = new AccessControlListParser();
    return new ServerAccessControlList(parser.createList(accessControlMapping, config));
  }

}
