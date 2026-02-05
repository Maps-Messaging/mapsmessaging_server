package io.mapsmessaging.rest.auth;

import io.mapsmessaging.rest.api.impl.BaseRestApi;
import io.mapsmessaging.rest.responses.StatusResponse;
import jakarta.annotation.Priority;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;

import static io.mapsmessaging.rest.auth.SessionTokenHandler.getAccessCookie;
import static io.mapsmessaging.rest.auth.SessionTokenHandler.validateToken;

@Priority(Priorities.AUTHENTICATION)
public abstract class BaseAuthenticationFilter implements ContainerRequestFilter {

  @Context
  private HttpServletRequest httpRequest;

  @Getter
  @Setter
  protected static int maxInactiveInterval = 600;

  private static final String[] OPEN_PATHS =
      new String[] { "openapi.json", "/health", "/api/v1/ping", "/api/v1/login", "/api/v1/server/schema/impl/*" };

  private static final String[] FULL_PATHS =
      new String[] { "/api/v1/server/log/sse/stream/", "/api/v1/messaging/sse/stream" };

  @Override
  public void filter(ContainerRequestContext containerRequest) throws IOException {
    String requestPath = containerRequest.getUriInfo().getRequestUri().getPath();

    for (String path : OPEN_PATHS) {
      if (requestPath.endsWith(path)) {
        return;
      }
      if (path.endsWith("*")) {
        String prefix = path.substring(0, path.length() - 1);
        if (requestPath.contains(prefix)) {
          return;
        }
      }
    }

    for (String path : FULL_PATHS) {
      if (requestPath.contains(path)) {
        return;
      }
    }

    processAuthentication(containerRequest);
  }

  protected void processAuthentication(ContainerRequestContext containerRequest) throws IOException {
    if (!BaseRestApi.AUTH_ENABLED) {
      httpRequest.getSession(true);
      return;
    }

    String accessToken = getAccessCookie(httpRequest);
    if (accessToken == null) {
      invalidateSessionIfPresent();
      abortUnauthorized(containerRequest, "invalid_request");
      return;
    }

    HttpSession session = httpRequest.getSession(false);
    if (session == null) {
      abortUnauthorized(containerRequest, "invalid_token");
      return;
    }

    String usernameFromToken = validateToken(accessToken, session, httpRequest, null);
    Object sessionUsername = session.getAttribute("username");

    if (sessionUsername == null || !sessionUsername.equals(usernameFromToken)) {
      abortUnauthorized(containerRequest, "invalid_token");
    }
  }

  private void invalidateSessionIfPresent() {
    HttpSession session = httpRequest.getSession(false);
    if (session != null) {
      session.invalidate();
    }
  }

  private void abortUnauthorized(ContainerRequestContext containerRequest, String error) {
    Response response =
        Response.status(Response.Status.UNAUTHORIZED)
            .header(HttpHeaders.WWW_AUTHENTICATE, buildWwwAuthenticateHeader(error))
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .header("Pragma", "no-cache")
            .header(HttpHeaders.CONTENT_LENGTH, "0")
            .type(MediaType.APPLICATION_JSON)
            .entity(new StatusResponse("User not authenticated"))
            .build();

    containerRequest.abortWith(response);
  }

  private String buildWwwAuthenticateHeader(String error) {
    return "Bearer realm=\"maps\", error=\"" + error + "\"";
  }
}
