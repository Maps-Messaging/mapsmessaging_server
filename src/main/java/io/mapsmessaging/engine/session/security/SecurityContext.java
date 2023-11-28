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

package io.mapsmessaging.engine.session.security;

import com.sun.security.auth.UserPrincipal;

import javax.security.auth.Subject;
import java.io.IOException;
import java.security.Principal;
import java.util.HashSet;
import java.util.Set;

public abstract class SecurityContext {

  protected final String username;
  protected Subject subject;
  protected boolean isLoggedIn;

  protected SecurityContext(String username){
    this.username = username;
  }

  public String getUsername(){
    return username;
  }

  public Subject getSubject(){
    return subject;
  }

  public boolean isLoggedIn(){
    return isLoggedIn;
  }

  public abstract void login() throws IOException;

  public abstract void logout();

  protected Subject buildSubject(String user, Principal endPointPrincipal){
    Set<Principal> principalSet = new HashSet<>();
    Set<String> credentials = new HashSet<>();
    Set<String> privileges = new HashSet<>();
    principalSet.add(new UserPrincipal(user));
    if(endPointPrincipal != null) principalSet.add(endPointPrincipal);
    return new Subject(true, principalSet, credentials, privileges);
  }

  @Override
  public String toString(){
    return "Username:"+username+"/tSubject:"+subject;
  }
}
