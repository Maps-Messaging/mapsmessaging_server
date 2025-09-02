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

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.sun.security.auth.UserPrincipal;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.rest.responses.LoginResponse;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.rest.responses.UpdateCheckResponse;
import io.mapsmessaging.security.identity.principals.UniqueIdentifierPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.UUID;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;
import static io.mapsmessaging.rest.auth.SessionTokenHandler.*;
import static io.mapsmessaging.rest.handler.SessionTracker.clearSession;

@Tag(name = "User Authentication")
@Path(URI_PATH)
public class AccessRequestApi extends BaseRestApi {

  private static final String SUCCESS = "Success";
  private static final String FAILURE = "Failure";

  protected static final String USERNAME = "username";

  @GET
  @Path("/session")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Returns the current authentication session",
      description = "Returns information about the current user authentication session, can be used to see if the user is logged in",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Returns if there have been updates",
              content = @Content(
                  mediaType = "application/json",
                  schema = @Schema(implementation = UpdateCheckResponse.class)
              )
          ),
          @ApiResponse(responseCode = "400", description = "Bad request")
      }
  )
  public LoginResponse getUserSession () {

    HttpSession session = request.getSession(false);
    if (session == null) {
      return new LoginResponse("Auth not enforced");
    }
    Subject subject = (Subject) session.getAttribute("subject");
    if (subject == null) {
      return new LoginResponse("Auth not enforced");
    }
    String username = session.getAttribute(USERNAME).toString();
    return new LoginResponse(SUCCESS, subject, username);
  }


  @GET
  @Path("/refreshToken")
  @Operation(
      summary = "Refreshes the users JWT",
      description = "Refreshes the current JWT cookie used for auth.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Refresh was successful or not required",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access")
      }
  )
  public String refreshToken() throws IOException {
    String accessToken = getAccessCookie(request);
    if (accessToken == null) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return FAILURE;
    }

    try {
      HttpSession session = request.getSession(false);
      if (session == null || session.getAttribute(USERNAME) == null) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return FAILURE;
      }
      validateToken(accessToken, session, request, response);
      renewSession(session, request, response);
      return "ok";

    } catch (JWTVerificationException ex) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      return FAILURE;
    }
  }

  @POST
  @Path("/login")
  @Produces({MediaType.APPLICATION_JSON})
  @Consumes({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "User login",
      description = "Allows a user to log in and obtain an authentication token. This endpoint does not require authentication and overrides global security settings.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Login successful or not required",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = LoginResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access")
      }
  )
  public LoginResponse login(LoginRequest loginRequest) throws IOException {
    boolean persistentSession = loginRequest.isPersistent();
    String sessionId = loginRequest.getSessionId();
    HttpSession session = request.getSession(false);

    if (session != null) {
      session.invalidate();
    }
    int maxAge = (AuthManager.getInstance().isAuthenticationEnabled() && loginRequest.isLongLived()) ? 7 * 24 * 60 * 60 : 15 * 60;

    if (AuthManager.getInstance().isAuthenticationEnabled()){
      if (AuthManager.getInstance().validate(loginRequest.getUsername(), loginRequest.getPassword().toCharArray())) {
        Subject subject = AuthManager.getInstance().getUserSubject(loginRequest.getUsername());
        session = setupCookieAndSession(loginRequest.getUsername(), subject, request, response, maxAge);
      }
      else{
        throw new IOException("Invalid username or password");
      }
    }
    else{
      loginRequest.setUsername("anonymous");
      loginRequest.setPassword("");
      Subject subject = new Subject();
      subject.getPrincipals().add(new UserPrincipal(loginRequest.getUsername()));
      subject.getPrincipals().add(new UniqueIdentifierPrincipal(UUID.randomUUID()));
      session = setupCookieAndSession(loginRequest.getUsername(), subject, request, response, maxAge);
    }

    if (persistentSession) {
      session.setAttribute("persistentSession", true);
    }
    if (sessionId != null && !sessionId.isEmpty()) {
      session.setAttribute("sessionId", sessionId);
    }
    hasAccess("root");
    Subject subject = (Subject) getSession().getAttribute("subject");
    String username = (String) getSession().getAttribute(USERNAME);
    return new LoginResponse(SUCCESS, subject, username);
  }

  @POST
  @Path("/logout")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "User logout",
      description = "Logs out the currently authenticated user by invalidating their session.",
      responses = {
          @ApiResponse(responseCode = "200", description = "Logout successful"),
          @ApiResponse(responseCode = "400", description = "Bad request or invalid session state")
      }
  )
  public StatusResponse logout() {
    HttpSession session = request.getSession(false);
    if (session != null) {
      clearSession(session);
      session.invalidate();
    }
    clearToken(request, response);
    return new StatusResponse(SUCCESS);
  }
}
