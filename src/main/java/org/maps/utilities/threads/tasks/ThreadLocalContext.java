/*
 *  Copyright [2020] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.maps.utilities.threads.tasks;


import javax.annotation.Nullable;
import org.jetbrains.annotations.NotNull;

/**
 * This class, is used by the Task Queue to validate the domain that the thread is running is is authorised to
 * be running the tasks. It is a useful mechanism to ensure only the correct task queue is running the correct task
 *
 *  @since 1.0
 *  @author Matthew Buckton
 *  @version 1.0
 */
public class ThreadLocalContext {

  private static final ThreadLocalContext instance = new ThreadLocalContext();

  protected final ThreadLocal<ThreadStateContext> context;


  public static @Nullable ThreadStateContext get(){
    return instance.context.get();
  }

  public static void set(@NotNull ThreadStateContext entry){
    instance.context.set(entry);
  }

  public static void remove(){
    instance.context.remove();
  }

  public static boolean checkDomain(@NotNull String domain){
    ThreadStateContext context = ThreadLocalContext.get();
    if(context == null){
      return false;
    }
    else {
      String check = (String) context.get("domain");
      return domain.equals(check);
    }
  }

  private ThreadLocalContext(){
    context = new ThreadLocal<>();
  }

}
