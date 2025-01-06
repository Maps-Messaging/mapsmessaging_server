/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 * Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
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

import static io.mapsmessaging.logging.ServerLogMessages.REST_CACHE_HIT;
import static io.mapsmessaging.logging.ServerLogMessages.REST_CACHE_MISS;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.rest.api.Constants;
import io.mapsmessaging.rest.auth.AuthenticationContext;
import io.mapsmessaging.rest.auth.RestAccessControl;
import io.mapsmessaging.rest.auth.RestAclMapping;
import io.mapsmessaging.rest.cache.CacheKey;
import io.mapsmessaging.security.access.Identity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import javax.security.auth.Subject;

public class BaseRestApi {
  @Context protected HttpServletRequest request;

  @Context protected HttpServletResponse response;

  @Context protected UriInfo uriInfo;

  private final Logger logger = LoggerFactory.getLogger(BaseRestApi.class);

  protected HttpSession getSession() {
    return request.getSession(true);
  }

  protected Subject getSubject() {
    return (Subject) getSession().getAttribute("subject");
  }

  private void checkAuthentication() {
    if (AuthManager.getInstance().isAuthorisationEnabled()) {
      HttpSession session = getSession();
      if (session == null || session.getAttribute("uuid") == null) {
        throw new WebApplicationException(401);
      }
    }
  }


  protected void hasAccess(String resource) {
    checkAuthentication();
    String method = request.getMethod();
    Subject subject = (Subject) getSession().getAttribute("subject");
    boolean access = true;

    if (AuthManager.getInstance().isAuthorisationEnabled()) {
      Identity userIdMap = AuthManager.getInstance().getUserIdentity((String) getSession().getAttribute("username"));
      RestAccessControl accessControl = AuthenticationContext.getInstance().getAccessControl();
      if (accessControl != null) {
        access = (userIdMap != null && accessControl.hasAccess(resource, subject, computeAccess(method)));
      }
    }
    if(!access) {
      throw new WebApplicationException("Access denied", Response.Status.FORBIDDEN);
    }
  }

  private long computeAccess(String method) {
    switch (method) {
      case "GET":
      case "HEAD":
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

  protected <T> T getFromCache(CacheKey key, Class<T> type) {
    Object cachedResponse = Constants.getCentralCache().get(key);
    T typedResponse = type.isInstance(cachedResponse) ? type.cast(cachedResponse) : null;
    if (typedResponse != null) {
      logger.log(REST_CACHE_HIT, key);
    } else {
      logger.log(REST_CACHE_MISS, key);
    }
    return typedResponse;
  }

  protected void putToCache(CacheKey key, Object value) {
    Constants.getCentralCache().put(key, value);
  }

  protected void removeFromCache(CacheKey key) {
    Constants.getCentralCache().remove(key);
  }
}
