/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package org.maps.utilities.threads.tasks;


import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

/**
 * This class, is used by the Task Queue to validate the domain that the thread is running is is authorised to
 * be running the tasks. It is a useful mechanism to ensure only the correct task queue is running the correct task
 *
 *  @since 1.0
 *  @author Matthew Buckton
 *  @version 1.0
 */
@ToString
public class ThreadLocalContext {

  private static final boolean DEBUG_DOMAIN;
  static{
    String value = System.getProperty("debug_domain", "false");
    boolean check = false;
    try {
      check = Boolean.parseBoolean(value);
    } catch (Exception exception) {
      // ignore we simply disable it
    }
    DEBUG_DOMAIN = check;
  }

  private static final ThreadLocalContext instance = new ThreadLocalContext();

  protected final ThreadLocal<ThreadStateContext> context;

  public static @Nullable ThreadStateContext get(){
    return instance.context.get();
  }

  public static void set(@NonNull @NotNull ThreadStateContext entry){
    instance.context.set(entry);
  }

  public static void remove(){
    instance.context.remove();
  }

  public static boolean checkDomain(@NonNull @NotNull String domain){
    boolean response = false;
    ThreadStateContext context = ThreadLocalContext.get();
    String check = "";
    if(context != null){
      check = (String) context.get("domain");
      response = domain.equals(check);
    }
    if(DEBUG_DOMAIN && !response){
      throw new RuntimeException("Incorrect thread domain detected! > "+check+" Expected "+domain);
    }
    return response;
  }

  private ThreadLocalContext(){
    context = new ThreadLocal<>();
  }

}
