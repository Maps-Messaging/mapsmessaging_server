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

package io.mapsmessaging.dto.rest.config.device.triggers;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = CronTriggerConfigDTO.class, name = "cron"),
    @JsonSubTypes.Type(value = InterruptTriggerConfigDTO.class, name = "interrupt"),
    @JsonSubTypes.Type(value = PeriodicTriggerConfigDTO.class, name = "periodic"),
})
@Schema(
    description = "Abstract base class for all schema configurations",
    discriminatorProperty = "type",
    discriminatorMapping = {
        @DiscriminatorMapping(value = "cron", schema = CronTriggerConfigDTO.class),
        @DiscriminatorMapping(value = "interrupt", schema = InterruptTriggerConfigDTO.class),
        @DiscriminatorMapping(value = "periodic", schema = PeriodicTriggerConfigDTO.class),
    })

@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class BaseTriggerConfigDTO extends BaseConfigDTO{

  @Schema(description = "Type of the trigger",
      example = "cron",
      allowableValues = {"cron", "interrupt", "periodic"}
  )
  protected String type;


  @Schema(description = "Name of the trigger", example = "dailyTrigger")
  protected String name;

}
