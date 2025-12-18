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

package io.mapsmessaging.rest.api.impl;

import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.rest.api.Constants;
import io.mapsmessaging.rest.auth.AuthenticationContext;
import io.mapsmessaging.rest.auth.RestAccessControl;
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

import static io.mapsmessaging.logging.ServerLogMessages.REST_CACHE_HIT;
import static io.mapsmessaging.logging.ServerLogMessages.REST_CACHE_MISS;

public class BaseRestApi {

  public static boolean AUTH_ENABLED = true;

  private final Logger logger = LoggerFactory.getLogger(BaseRestApi.class);
  @Context
  protected HttpServletRequest request;
  @Context
  protected HttpServletResponse response;
  @Context
  protected UriInfo uriInfo;

  protected HttpSession getSession() {
    HttpSession session = request.getSession(false);
    if (session == null) {
      throw new WebApplicationException(401);
    }
    return session;
  }

  protected Subject getSubject() {
    return (Subject) getSession().getAttribute("subject");
  }

  private void checkAuthentication() {

    HttpSession session = getSession();
    if (session.getAttribute("uuid") == null) {
      throw new WebApplicationException(401);
    }
  }


  protected void hasAccess(String resource) {
    if(!AUTH_ENABLED){
      return;
    }
    checkAuthentication();
    String method = request.getMethod();
    Subject subject = (Subject) getSession().getAttribute("subject");
    boolean access = true;

    HttpSession session = getSession();
    Identity userIdMap = (Identity) session.getAttribute("userIdMap");
    if(userIdMap == null) {
      String username = (String) session.getAttribute("username");
      userIdMap = AuthManager.getInstance().getUserIdentity(username);
      if(userIdMap != null) {
        session.setAttribute("userIdMap", userIdMap);
      }
    }
    RestAccessControl accessControl = AuthenticationContext.getInstance().getAccessControl();
/*
    if (accessControl != null) {
      access = (userIdMap != null && accessControl.hasAccess(resource, subject, computeAccess(method)));
    }

*/
    if (!access) {
      throw new WebApplicationException("Access denied", Response.Status.FORBIDDEN);
    }
  }

  private long computeAccess(String method) {
    return switch (method) {
      case "GET", "HEAD" -> 0;
      case "POST" -> 1;
      case "PUT" -> 2;
      case "DELETE" -> 3;
      default -> 0;
    };
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
