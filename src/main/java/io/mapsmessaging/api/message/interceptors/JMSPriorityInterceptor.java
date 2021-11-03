/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.api.message.interceptors;

import io.mapsmessaging.api.features.Priority;
import io.mapsmessaging.api.message.Message;

public class JMSPriorityInterceptor implements Interceptor {

  @Override
  public Object get(Message message) {
    Priority priority = message.getPriority();
    if(priority == null){
      return 0;
    }
    return priority.getValue();
  }
}

