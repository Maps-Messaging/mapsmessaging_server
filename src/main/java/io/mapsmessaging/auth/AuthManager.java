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

package io.mapsmessaging.auth;

import com.sun.security.auth.UserPrincipal;
import io.mapsmessaging.MessageDaemon;
import io.mapsmessaging.api.features.DestinationType;
import io.mapsmessaging.auth.priviliges.SessionPrivileges;
import io.mapsmessaging.auth.registry.AuthenticationStorage;
import io.mapsmessaging.auth.registry.GroupDetails;
import io.mapsmessaging.auth.registry.PasswordGenerator;
import io.mapsmessaging.auth.registry.UserDetails;
import io.mapsmessaging.config.AuthManagerConfig;
import io.mapsmessaging.configuration.ConfigurationProperties;
import io.mapsmessaging.dto.rest.system.Status;
import io.mapsmessaging.dto.rest.system.SubSystemStatusDTO;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.security.access.AuthContext;
import io.mapsmessaging.security.access.Group;
import io.mapsmessaging.security.access.Identity;
import io.mapsmessaging.security.access.mapping.GroupIdMap;
import io.mapsmessaging.security.authorisation.*;
import io.mapsmessaging.security.identity.IdentityLookupFactory;
import io.mapsmessaging.security.identity.principals.UniqueIdentifierPrincipal;
import io.mapsmessaging.utilities.Agent;
import io.mapsmessaging.utilities.SystemProperties;
import io.mapsmessaging.utilities.configuration.ConfigurationManager;
import lombok.Getter;

import javax.security.auth.Subject;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.Principal;
import java.util.*;

import static io.mapsmessaging.engine.audit.AuditEvent.AUTHORISATION_FAILED;
import static io.mapsmessaging.logging.ServerLogMessages.*;

@SuppressWarnings("java:S6548") // yes it is a singleton
public class AuthManager implements Agent {


  private static final String ADMIN_USER = "admin";
  private static final String USER = "user";
  private static final String ANONYMOUS = "anonymous";

  private static final String ADMIN_GROUP = "admins";
  private static final String EVERYONE = "everyone";


  private static class Holder {
    static final AuthManager INSTANCE = new AuthManager();
  }
  public static AuthManager getInstance() {
    return Holder.INSTANCE;
  }


  private final Logger logger;

  @Getter
  private final AuthManagerConfig config;
  private AuthenticationStorage authenticationStorage;
  private final Map<String, Subject> subjectMap = new WeakHashMap<>();

  @Getter
  private final boolean authenticationEnabled;
  @Getter
  private final boolean authorisationEnabled;

  private String errMessage="";

  @Override
  public String getName() {
    return "AuthManager";
  }

  @Override
  public String getDescription() {
    return "Manages the authentication and authorisation of user connections to the server";
  }

  @Override
  public void start() {
    if (authenticationEnabled) {
      String jaasConfig =  SystemProperties.getInstance().getProperty("java.security.auth.login.config");
      boolean exists = false;
      String msg ="Authentication enabled but login config file does not exist Configured:"+ jaasConfig+", please configure -Djava.security.auth.login.config=<valid path to jaas.config>";
      if (jaasConfig != null) {
        File loginConfig = new File(jaasConfig);
        exists = loginConfig.exists();
      }
      else{
        msg = "Authentication enabled but login config not configured, please configure -Djava.security.auth.login.config=<valid path to jaas.config>";
      }
      if(!exists || jaasConfig.isEmpty() ) {
        throw new NoLoginContextConfigException(msg);
      }
      try {
        authenticationStorage = new AuthenticationStorage(new ConfigurationProperties(config.getAuthConfig()));
        if (authenticationStorage.isFirstBoot()) {
          createInitialUsers((String)config.getAuthConfig().get("configDirectory"));
        }
        IdentityLookupFactory.getInstance().registerSiteIdentityLookup("system", authenticationStorage.getIdentityAccessManager().getIdentityLookup());
      } catch (Exception e) {
        errMessage = e.getMessage();
        logger.log(AUTH_STARTUP_FAILED, e);
      }
    }
  }

  private void saveInitialUserDetails(String path, String[][] details) {
    try (BufferedWriter bw = new BufferedWriter(new FileWriter(path + File.separator + "admin_password", true))) {
      for (String[] detail : details) {
        bw.write(detail[0] + "=" + detail[1]);
        bw.newLine(); // Add a newline character after each line
      }
    } catch (IOException e) {
      logger.log(AUTH_SAVE_FAILED, e);
    }
  }

  @Override
  public void stop() {
    if (authenticationStorage != null) authenticationStorage.close();
  }

  public boolean addUser(String username, char[] password, SessionPrivileges quotas, String[] groups) {
    if (authenticationStorage != null) {
      logger.log(AUTH_ADDED_USER, username);
      return authenticationStorage.addUser(username, password, quotas, groups);
    }
    return false;
  }

  public boolean delUser(String username) {
    if (authenticationStorage != null) {
      logger.log(AUTH_DELETED_USER, username);
      return authenticationStorage.delUser(username);
    }
    return false;
  }

  public Subject update(Subject subject) {
    if (subject == null) subject = new Subject();
    if (authenticationStorage != null) {
      return authenticationStorage.update(subject);
    }
    return null;
  }

  public SessionPrivileges getQuota(UUID userId) {
    if (authenticationStorage != null) {
      return authenticationStorage.getQuota(userId);
    }
    return null;
  }


  public boolean canAccess(Identity identity, Permission permission, ProtectedResource resource) {
    if(!authorisationEnabled) return true;
    boolean result = authenticationStorage.canAccess(identity, permission, resource);
    if(!result){
      logger.log(AUTHORISATION_FAILED, identity.getUsername(), permission.getName(), resource.getResourceId());
    }
    return result;
  }

  public boolean hasAllAccess(List<AuthRequest> request) {
    if(!authorisationEnabled) return true;
    boolean result = authenticationStorage.hasAllAccess(request);

    if(!result){
      for(AuthRequest authRequest : request){
        logger.log(AUTHORISATION_FAILED, authRequest.getIdentity().getUsername(), authRequest.getPermission().getName(), authRequest.getProtectedResource().getResourceId());
      }
    }
    return result;
  }

  public AuthorizationProvider getAuthorizationProvider() {
    return authenticationStorage.getAuthorizationProvider();
  }



  public void grant(Identity identity, Permission permission, ProtectedResource resource) {
    authenticationStorage.grant(identity, permission, resource);
  }

  public void grant(Group group, Permission permission, ProtectedResource resource) {
    authenticationStorage.grant(group, permission, resource);
  }

  public void deny(Identity identity, Permission permission, ProtectedResource resource) {
    authenticationStorage.deny(identity, permission, resource);
  }

  public void deny(Group group, Permission permission, ProtectedResource resource) {
    authenticationStorage.deny(group, permission, resource);
  }


  public void revoke(Identity identity, Permission permission, ProtectedResource resource) {
    authenticationStorage.revoke(identity, permission, resource);
  }

  public void revoke(Group group, Permission permission, ProtectedResource resource) {
    authenticationStorage.revoke(group, permission, resource);
  }

  private AuthManager() {
    logger = LoggerFactory.getLogger(AuthManager.class);
    config = ConfigurationManager.getInstance().getConfiguration(AuthManagerConfig.class);
    if (config != null) {
      authenticationEnabled = config.isAuthenticationEnabled();
      authorisationEnabled = config.isAuthorisationEnabled() &&  authenticationEnabled;
    }
    else{
      authenticationEnabled = false;
      authorisationEnabled = false;
    }
  }

  private void createInitialUsers(String path) throws IOException {
    String password = PasswordGenerator.generateRandomPassword(12);
    if (!addUser(ADMIN_USER, password.toCharArray(), SessionPrivileges.create(ADMIN_USER), new String[]{ADMIN_GROUP, EVERYONE})) {
      logger.log(SECURITY_MANAGER_FAILED_TO_CREATE_USER, ADMIN_USER);
    }

    String userpassword = PasswordGenerator.generateRandomPassword(12);
    if (!addUser(USER, userpassword.toCharArray(), SessionPrivileges.create(USER), new String[]{EVERYONE})) {
      logger.log(SECURITY_MANAGER_FAILED_TO_CREATE_USER, USER);
    }

    if (!addUser(ANONYMOUS, new char[0], SessionPrivileges.create(ANONYMOUS), new String[]{EVERYONE})) {
      logger.log(SECURITY_MANAGER_FAILED_TO_CREATE_USER, USER);
    }

    AuthContext context = new AuthContext("localhost", "setup", "setup");
    saveInitialUserDetails(path, new String[][]{{ADMIN_USER, password}, {USER, userpassword}});
    if (!authenticationStorage.validateUser(ADMIN_USER, password.toCharArray(), context)) {
      logger.log(SECURITY_MANAGER_FAILED_TO_INITIALISE_USER, USER);
    }
    if (!authenticationStorage.validateUser(USER, userpassword.toCharArray(), context)) {
      logger.log(SECURITY_MANAGER_FAILED_TO_INITIALISE_USER, USER);
    }
    if(authorisationEnabled) {
      setupDefaultAuthorisation();
    }
  }

  private void setupDefaultAuthorisation(){
    Group admin = authenticationStorage.findGroup(ADMIN_GROUP);
    ProtectedResource server  = new  ProtectedResource("Server", MessageDaemon.getInstance().getId(), null);
    List<ProtectedResource> destinations = new ArrayList<>();
    for(DestinationType type : DestinationType.values()){
      destinations.add( new  ProtectedResource(type.getName(), "/", null));
    }

    for(Permission permission:ServerPermissions.values()){
      int mask = Long.numberOfTrailingZeros(permission.getMask());
      if(mask < 30) { // Server based perms
        authenticationStorage.grant(admin, permission, server);
      }
      else{
        grantList(admin, permission, destinations);
      }
    }


    Group everyone = authenticationStorage.findGroup(EVERYONE);
    authenticationStorage.grant(everyone, ServerPermissions.CONNECT, server);
    authenticationStorage.grant(everyone, ServerPermissions.PERSISTENT_SESSION, server);
    authenticationStorage.grant(everyone, ServerPermissions.CREATE_DESTINATION, server);
    authenticationStorage.grant(everyone, ServerPermissions.PURGE_SERVER, server);
    authenticationStorage.grant(everyone, ServerPermissions.LIST_DESTINATIONS, server);
    authenticationStorage.grant(everyone, ServerPermissions.VIEW_STATS, server);
    authenticationStorage.grant(everyone, ServerPermissions.WILD_CARD_SUBSCRIBE, server);


    grantList(everyone, ServerPermissions.PUBLISH, destinations);
    grantList(everyone, ServerPermissions.SUBSCRIBE, destinations);
    grantList(everyone, ServerPermissions.RETAIN, destinations);
    grantList(everyone, ServerPermissions.VIEW, destinations);
    grantList(everyone, ServerPermissions.DURABLE, destinations);

    Identity anony = authenticationStorage.findUser(ANONYMOUS);
    if(anony != null) {
      grantUserList(anony, ServerPermissions.DELETE, destinations);
      grantUserList(anony, ServerPermissions.PUBLISH, destinations);
      grantUserList(anony, ServerPermissions.SUBSCRIBE, destinations);
      grantUserList(anony, ServerPermissions.RETAIN, destinations);
      grantUserList(anony, ServerPermissions.VIEW, destinations);
      grantUserList(anony, ServerPermissions.DURABLE, destinations);
    }


    ProtectedResource denyAll = new ProtectedResource("Topic", "test/nosubscribe", null);
    authenticationStorage.deny(everyone, ServerPermissions.SUBSCRIBE, denyAll);
    authenticationStorage.deny(admin, ServerPermissions.SUBSCRIBE, denyAll);
  }

  private void grantList(Group group, Permission permission, List<ProtectedResource> destinations ){
    for(ProtectedResource resource:destinations){
      authenticationStorage.grant(group, permission, resource);
    }
  }

  private void grantUserList(Identity user, Permission permission, List<ProtectedResource> destinations ){
    for(ProtectedResource resource:destinations){
      authenticationStorage.grant(user, permission, resource);
    }
  }

  public boolean validate(String username, char[] password, AuthContext context) throws IOException {
    return authenticationStorage.validateUser(username, password, context);
  }

  public Identity getUserIdentity(String username) {
    if(authenticationStorage == null) return null;
    return authenticationStorage.findUser(username);
  }

  public Identity getUserIdentity(UUID uuid) {
    return authenticationStorage.findUser(uuid);
  }

  public Group getGroupIdentity(String groupName){
    return authenticationStorage.findGroup(groupName);
  }

  public Group getGroupIdentity(UUID uuid){
    return authenticationStorage.findGroup(uuid);
  }

  public Subject getUserSubject(String username) {
    return subjectMap.computeIfAbsent(username, key -> {
      Subject newSubject = new Subject();
      Identity map = authenticationStorage.findUser(key);
      if (map != null) {
        Set<Principal> principalList = newSubject.getPrincipals();
        principalList.add(new UniqueIdentifierPrincipal(map.getId()));
        principalList.add(new UserPrincipal(key));
        authenticationStorage.update(newSubject);
      }
      return newSubject;
    });
  }


  public List<UserDetails> getUsers() {
    if (authenticationStorage != null) {
      return authenticationStorage.getUsers();
    }
    return new ArrayList<>();
  }

  public List<GroupDetails> getGroups() {
    if (authenticationStorage != null) {
      return authenticationStorage.getGroups();
    }
    return new ArrayList<>();
  }

  public void delGroup(String groupName) throws IOException {
    logger.log(AUTH_DELETED_GROUP, groupName);
    authenticationStorage.delGroup(groupName);
  }

  public GroupIdMap addGroup(String groupName) throws IOException {
    logger.log(AUTH_ADDED_GROUP, groupName);
    return authenticationStorage.addGroup(groupName);
  }

  public void addUserToGroup(String user, String group) throws IOException {
    logger.log(AUTH_MODIFIED_GROUP, user, "added to",  group);
    authenticationStorage.addUserToGroup(user, group);
  }

  public void removeUserFromGroup(String username, String groupName) throws IOException {
    logger.log(AUTH_MODIFIED_GROUP, username, "removed from",  groupName);
    authenticationStorage.removeUserFromGroup(username, groupName);
  }

  @Override
  public SubSystemStatusDTO getStatus() {
    SubSystemStatusDTO status = new SubSystemStatusDTO();
    status.setName(getName());
    status.setComment("");
    if (authenticationEnabled) {
      status.setStatus(Status.OK);
      if(!errMessage.isEmpty()){
        status.setComment(errMessage);
        status.setStatus(Status.ERROR);
      }
    }
    else{
      status.setStatus(Status.DISABLED);
    }
    return status;
  }

  private static class NoLoginContextConfigException extends RuntimeException {

    public NoLoginContextConfigException(String s) {
      super(s);
    }
  }
}
