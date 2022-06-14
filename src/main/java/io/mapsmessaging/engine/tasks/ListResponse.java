/*
 *    Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *
 */

package io.mapsmessaging.engine.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ListResponse implements Response {

  private final List<Future<Response>> responses;

  public ListResponse(){
    responses = new ArrayList<>();
  }

  public void addResponse(Future<Response> response){
    responses.add(response);
  }

  public boolean isDone(){
    for(Future<Response> future:responses){
      if(!future.isDone()){
        return false;
      }
    }
    return true;
  }

  public  List<Response> getResponse() throws ExecutionException, InterruptedException {
    List<Response> returnList = new ArrayList<>();
    for(Future<Response> future:responses){
      returnList.add(future.get());
    }
    return returnList;
  }

}
