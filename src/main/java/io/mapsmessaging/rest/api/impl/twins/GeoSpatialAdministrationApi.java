/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.rest.api.impl.twins;

import static io.mapsmessaging.rest.api.Constants.URI_PATH;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Path;

@Tag(name = "Server Twin Geospatial Administration", description = "Administration of the existing TwinManager geospatial area configuration and GeoJSON boundary files.")
@Path(URI_PATH + "/server/twin/admin/geospatial")
public class GeoSpatialAdministrationApi extends GeoSpatialAdminApi {}
