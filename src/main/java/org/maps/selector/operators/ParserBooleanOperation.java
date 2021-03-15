/*
 *    Copyright [ 2020 - 2021 ] [Matthew Buckton]
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

package org.maps.selector.operators;

public class ParserBooleanOperation implements ParserExecutor {

  private final Boolean result;

  public ParserBooleanOperation(Boolean result)  {
    this.result = result;
  }

  @Override
  public boolean evaluate(IdentifierResolver resolver) {
    return Boolean.TRUE.equals(result);
  }

  @Override
  public String toString(){
    return result.toString();
  }

  @Override
  public boolean equals(Object rhs){
    if(rhs instanceof ParserBooleanOperation){
      return result.equals(((ParserBooleanOperation) rhs).result);
    }
    return false;
  }

  @Override
  public int hashCode(){
    return result.hashCode();
  }

}
