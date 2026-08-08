/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.rest.api.impl.twins;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Tag(name = "Server Twin Administration", description = "Administrative REST facade over the existing TwinManager configuration model.")
@Path(URI_PATH + "/server/twin/admin")
@Produces(MediaType.APPLICATION_JSON)
public class TwinAdministrationApi extends TwinDomainApi {}
