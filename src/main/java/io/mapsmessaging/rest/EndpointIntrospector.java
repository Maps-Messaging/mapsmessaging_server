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

package io.mapsmessaging.rest;


import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.glassfish.jersey.server.model.Invocable;

public class EndpointIntrospector implements ApplicationEventListener {

  @Override
  public void onEvent(ApplicationEvent applicationEvent) {
    if (applicationEvent.getType() != ApplicationEvent.Type.INITIALIZATION_FINISHED) {
      return;
    }

    ResourceModel resourceModel = applicationEvent.getResourceModel();
    if (resourceModel == null) {
      return;
    }

    List<EndpointInfo> endpointInfos = new ArrayList<>();
    for (Resource resource : resourceModel.getResources()) {
      collectEndpoints(resource, "", endpointInfos);
    }

    EndpointRegistry.getInstance().setEndpoints(endpointInfos);
  }

  @Override
  public RequestEventListener onRequest(RequestEvent requestEvent) {
    return null;
  }

  private void collectEndpoints(Resource resource, String parentPath, List<EndpointInfo> endpointInfos) {
    String resourcePath = resource.getPath();
    String fullPath;

    if (resourcePath == null || resourcePath.isEmpty()) {
      fullPath = parentPath.isEmpty() ? "/" : parentPath;
    } else if (parentPath == null || parentPath.isEmpty() || "/".equals(parentPath)) {
      fullPath = resourcePath.startsWith("/") ? resourcePath : "/" + resourcePath;
    } else {
      if (parentPath.endsWith("/")) {
        fullPath = parentPath + resourcePath;
      } else {
        fullPath = parentPath + resourcePath;
      }
    }

    for (ResourceMethod resourceMethod : resource.getResourceMethods()) {
      String httpMethod = resourceMethod.getHttpMethod();
      if (httpMethod == null|| httpMethod.equalsIgnoreCase("options")) {
        continue;
      }

      Invocable invocable = resourceMethod.getInvocable();
      Class<?> handlerClass = invocable.getHandler().getHandlerClass();
      Method handlingMethod = invocable.getHandlingMethod();

      EndpointInfo endpointInfo = EndpointInfo.builder()
          .httpMethod(httpMethod)
          .path(fullPath)
          .resourceClassName(handlerClass.getName())
          .resourceMethodName(handlingMethod.getName())
          .build();

      endpointInfos.add(endpointInfo);
    }

    for (Resource child : resource.getChildResources()) {
      collectEndpoints(child, fullPath, endpointInfos);
    }
  }
}
