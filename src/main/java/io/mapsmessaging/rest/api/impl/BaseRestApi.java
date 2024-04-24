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

package io.mapsmessaging.rest.api.impl;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.rest.auth.AuthenticationContext;
import io.mapsmessaging.rest.auth.RestAccessControl;
import io.mapsmessaging.rest.auth.RestAclMapping;
import io.mapsmessaging.security.access.mapping.UserIdMap;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.core.Context;

import javax.security.auth.Subject;

public class BaseRestApi {
  @Context
  protected HttpServletRequest request;

  @Context
  protected HttpServletResponse response;

  protected HttpSession getSession() {
    return request.getSession(true);
  }

  protected Subject getSubject() {
    return (Subject) getSession().getAttribute("subject");
  }

  protected boolean hasAccess(){
    String method = request.getMethod();
    String uri = request.getRequestURI();
    Subject subject = (Subject) getSession().getAttribute("subject");

    if(AuthManager.getInstance().isAuthorisationEnabled()) {
      UserIdMap userIdMap = AuthManager.getInstance().getUserIdentity((String) getSession().getAttribute("username"));
      RestAccessControl accessControl = AuthenticationContext.getInstance().getAccessControl();
      if(userIdMap != null) {
        boolean hasAccess = accessControl.hasAccess(uri, subject, computeAccess(method));
        System.err.println(hasAccess);
      }
      else{
        return false;
      }
    }
    return true;
  }

  private long computeAccess(String method){
    switch(method){
      case "GET":
        return RestAclMapping.READ_VALUE;
      case "POST":
        return RestAclMapping.CREATE_VALUE;
      case "PUT":
        return RestAclMapping.UPDATE_VALUE;
      case "DELETE":
        return RestAclMapping.DELETE_VALUE;
      default:
        return RestAclMapping.READ_VALUE;
    }
  }
}
