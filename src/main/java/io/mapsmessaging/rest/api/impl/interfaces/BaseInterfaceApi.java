/*
 * Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.rest.api.impl.interfaces;

import io.mapsmessaging.network.EndPointManager;
import io.mapsmessaging.rest.api.impl.BaseRestApi;

public class BaseInterfaceApi extends BaseRestApi {
  protected boolean isMatch(String name, EndPointManager endPointManager) {
    return (endPointManager.getEndPointServer().getConfig().getProperties().getProperty("name").equals(name));
  }

}
