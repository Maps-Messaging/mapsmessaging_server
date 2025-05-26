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

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.sun.security.auth.UserPrincipal;
import io.mapsmessaging.BuildInfo;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.Session;
import io.mapsmessaging.api.SessionManager;
import io.mapsmessaging.auth.AuthManager;
import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.rest.api.impl.messaging.impl.RestMessageListener;
import io.mapsmessaging.rest.auth.BaseAuthenticationFilter;
import io.mapsmessaging.rest.responses.LoginResponse;
import io.mapsmessaging.rest.responses.StatusResponse;
import io.mapsmessaging.rest.responses.UpdateCheckResponse;
import io.mapsmessaging.security.SubjectHelper;
import io.mapsmessaging.security.identity.principals.UniqueIdentifierPrincipal;
import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

@OpenAPIDefinition(
    info =
    @Info(
        description =
            "Maps Messaging Server Rest API, provides simple Rest API to manage and interact with the server",
        version = BuildInfo.BUILD_VERSION,
        title = "Maps Messaging Rest Server",
        contact =
        @Contact(
            name = "Matthew Buckton",
            email = "info@mapsmessaging.io",
            url = "http://mapsmessaging.io"),
        license = @License(name = "Apache 2.0", url = "http://www.apache.org/licenses/LICENSE-2.0")),
    servers = {
        @io.swagger.v3.oas.annotations.servers.Server(
            description = "Default Server",
            url = "http://localhost:8080"
        )
    },
    tags = {
        @Tag(
            name = "Authentication and Authorisation Management",
            description = "Provides endpoints for managing user authentication and authorisation, including login, logout, token management, and role-based access control to ensure secure interactions with the server."
        ),
        @Tag(
            name = "Destination Management",
            description = "Facilitates the management of destinations such as topics and queues. Includes operations for creating, updating, deleting, and querying destinations, as well as managing subscriptions."
        ),
        @Tag(
            name = "Messaging Interface",
            description = "Offers APIs for sending and receiving messages, enabling communication between clients and the server. Supports various messaging protocols and real-time event handling."
        ),
        @Tag(
            name = "Server Health",
            description = "Includes endpoints for monitoring the server's health and operational status, providing simple and detailed responses for status checks and diagnostics."
        ),
        @Tag(
            name = "Server Interface Management",
            description = "Manages the server's network interfaces, including configuration, monitoring, and troubleshooting of connections to ensure optimal performance and reliability."
        ),
        @Tag(
            name = "Schema Management",
            description = "Provides functionality to configure, manage, and query schemas used by the server, enabling seamless integration with structured data formats and validation mechanisms."
        ),
        @Tag(
            name = "Server Management",
            description = "Includes operations for monitoring and managing the server's status, configurations, and performance metrics to ensure smooth and efficient operation."
        ),
        @Tag(
            name = "Server Integration Management",
            description = "Manages the server's integrations with other messaging brokers, enabling interoperability and seamless data exchange across distributed systems."
        ),
        @Tag(
            name = "Server Integration Status",
            description = "Retrieves the current status of the server to server integration."
        ),
        @Tag(
            name = "Connection Management",
            description = "Handles client connections to the server, offering endpoints for monitoring, managing, and troubleshooting active connections and session details."
        ),
        @Tag(
            name = "Discovery Management",
            description = "Provides mechanisms for managing the server's discovery agents, allowing automated detection and configuration of network services and resources."
        ),
        @Tag(
            name = "Hardware Management",
            description = "Enables the management of hardware devices integrated with the server, including configuration, monitoring, and diagnostics for seamless hardware-software interaction."
        ),
        @Tag(
            name = "LoRa Device Management",
            description = "Offers APIs for managing LoRa devices, including adding, updating, retrieving configurations, monitoring device statistics, and managing endpoint connections."
        ),
        @Tag(
            name = "Logging Monitor",
            description = "Offers simple API to retrieve server logs or to stream server logs via SSE"
        )
    },

    externalDocs =
    @ExternalDocumentation(
        description = "Maps Messaging",
        url = "https://www.mapsmessaging.io/"),
    security = {
        @SecurityRequirement(name = "basicAuth"),
        @SecurityRequirement(name = "authScheme")
    }

)

@SecurityScheme(
    name = "basicAuth",
    type = SecuritySchemeType.HTTP,
    scheme = "basic"
)
@SecurityScheme(
    name = "authScheme",
    type = SecuritySchemeType.HTTP,
    scheme = "bearer",
    bearerFormat = "JWT"
)


@Tag(name = "Server Health")
@Path(URI_PATH)
public class MapsRestServerApi extends BaseRestApi {

  protected static int maxInactiveInterval = 600;
  protected static final String USERNAME = "username";


  private static final String secret = "very-secret-key-that-should-be-strong";
  private static final Algorithm algorithm = Algorithm.HMAC256(secret);

  @GET
  @Path("/ping")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Ping the server",
      description = "A simple endpoint to verify that the server is operational and responsive.",
      security = {}, // Overrides global security to make this endpoint accessible without authentication
      responses = {
          @ApiResponse(responseCode = "200", description = "Server is operational"),
          @ApiResponse(responseCode = "400", description = "Bad request")
      }
  )
  public StatusResponse getPing() {
    return new StatusResponse("Success");
  }

  @GET
  @Path("/name")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Retrieve the server's unique name",
      description = "Returns the unique identifier of the server instance.",
      responses = {
          @ApiResponse(
              responseCode = "200",
              description = "Get server name was successful",
              content = @Content(mediaType = "application/json", schema = @Schema(implementation = StatusResponse.class))
          ),
          @ApiResponse(responseCode = "400", description = "Bad request"),
          @ApiResponse(responseCode = "401", description = "Invalid credentials or unauthorized access"),
          @ApiResponse(responseCode = "403", description = "User is not authorised to access the resource"),
      }
  )
  public StatusResponse getName() {
    return new StatusResponse(MessageDaemon.getInstance().getId());
  }

  @GET
  @Path("/updates")
  @Produces({MediaType.APPLICATION_JSON})
  @Operation(
      summary = "Check for configuration updates",
      description = "Provides information about any changes in the server's configuration update counts.",
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
  public UpdateCheckResponse checkForUpdates() {
    long schema = SchemaManager.getInstance().getUpdateCount();
    return new UpdateCheckResponse(schema, 0, 0);
  }


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
    LoginResponse loginResponse = new LoginResponse("Success", subject, username);
    return loginResponse;
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
      Subject subject = (Subject) session.getAttribute("subject");
      if (subject != null && session.getAttribute(USERNAME) != null && session.getAttribute(USERNAME).equals(loginRequest.getUsername())) {
        return new LoginResponse("Already authenticated");
      }
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
    return new LoginResponse("Success", subject, username);
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
  public StatusResponse logout(@Context HttpServletResponse httpResponse) throws IOException {
    HttpSession session = request.getSession(false);
    String response = "Success";
    if (session != null) {
      Session msgSession = (Session) getSession().getAttribute("authenticatedSession");
      if (msgSession != null) {
        try {
          SessionManager.getInstance().close(msgSession, true);
        } catch (IOException e) {
          response = "Failure : " + e.getMessage();
        }
        msgSession.removeSubscription("authenticatedSession");
      }
      RestMessageListener msgListener = (RestMessageListener) getSession().getAttribute("restListener");
      if (msgListener != null) {
        msgListener.close();
        session.removeAttribute("restListener");
      }
      session.invalidate();
    }
    Cookie cookie = new Cookie("access_token", "");
    cookie.setPath("/");
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setMaxAge(0);
    httpResponse.addCookie(cookie);
    return new StatusResponse(response);
  }

  private HttpSession setupCookieAndSession(String username, Subject subject, HttpServletRequest httpRequest, HttpServletResponse httpResponse,int maxAge)  {
    String token = generateToken(username, maxAge);
    UUID uuid = SubjectHelper.getUniqueId(subject);

    Cookie cookie = new Cookie("access_token", token);
    cookie.setHttpOnly(true);
    cookie.setSecure(true);
    cookie.setPath("/");
    cookie.setMaxAge(maxAge);
    httpResponse.addCookie(cookie);
    return BaseAuthenticationFilter.setupSession(httpRequest, username, uuid, subject);
  }

  public static String generateToken(String username, int age) {
    return JWT.create()
        .withSubject(username)
        .withIssuedAt(new Date())
        .withExpiresAt(new Date(System.currentTimeMillis() + (age*1000L)))
        .sign(algorithm);
  }


}
