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

package io.mapsmessaging.engine.resources;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
public class ResourceProperties {

  @Getter @Setter private Date date;
  @Getter @Setter private String resourceName;
  @Getter @Setter private String type;
  @Getter @Setter private String uuid;
  @Getter @Setter private String buildDate;
  @Getter @Setter private String buildVersion;

  public ResourceProperties(){}

  public ResourceProperties(Date date, String resourceName, String type, String uuid, String buildDate, String buildVersion){
    this.date = date;
    this.resourceName = resourceName;
    this.type = type;
    this.uuid = uuid;
    this.buildDate = buildDate;
    this.buildVersion = buildVersion;
  }

}
