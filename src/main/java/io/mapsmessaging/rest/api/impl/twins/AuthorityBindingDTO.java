/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.rest.api.impl.twins;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
@Schema(description = "Requested full-drone bindings for a STANAG authority UUID.")
public class AuthorityBindingDTO {

  @Schema(description = "Drone names on which the authority must be present on every configured task capability.")
  private List<String> drones = new ArrayList<>();
}
