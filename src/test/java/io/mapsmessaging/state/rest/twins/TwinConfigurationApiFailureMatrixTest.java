/*
 *
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *
 */

package io.mapsmessaging.state.rest.twins;

import io.mapsmessaging.dto.rest.config.protocol.impl.TakProtocolDTO;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.MavlinkTwinConfigDTO;
import io.mapsmessaging.state.config.TwinPublishConfigDTO;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TwinConfigurationApiFailureMatrixTest {

  @ParameterizedTest(name = "{0} maps unauthorized access to 401")
  @EnumSource(Operation.class)
  void everyOperationMapsUnauthorizedAccess(Operation operation) {
    TestApi api = new TestApi(AccessFailure.UNAUTHORIZED, false);

    try (Response response = operation.invoke(api)) {
      assertEquals(401, response.getStatus(), operation.name());
      assertEquals("application/json", response.getMediaType().toString());
    }
  }

  @ParameterizedTest(name = "{0} maps forbidden access to 403")
  @EnumSource(Operation.class)
  void everyOperationMapsForbiddenAccess(Operation operation) {
    TestApi api = new TestApi(AccessFailure.FORBIDDEN, false);

    try (Response response = operation.invoke(api)) {
      assertEquals(403, response.getStatus(), operation.name());
      assertEquals("application/json", response.getMediaType().toString());
    }
  }

  @ParameterizedTest(name = "{0} preserves unexpected authorization status")
  @EnumSource(Operation.class)
  void unexpectedAuthorizationFailuresAreRethrown(Operation operation) {
    TestApi api = new TestApi(AccessFailure.OTHER, false);

    WebApplicationException failure =
        assertThrows(WebApplicationException.class, () -> operation.invoke(api), operation.name());

    assertNotNull(failure.getResponse());
    assertEquals(418, failure.getResponse().getStatus());
  }

  @ParameterizedTest(name = "{0} contains unavailable store as internal error")
  @EnumSource(Operation.class)
  void everyOperationContainsUnavailableConfigurationStore(Operation operation) {
    TestApi api = new TestApi(AccessFailure.NONE, true);

    try (Response response = operation.invoke(api)) {
      assertEquals(500, response.getStatus(), operation.name());
      assertInstanceOf(io.mapsmessaging.rest.responses.StatusResponse.class, response.getEntity());
    }
  }

  private enum Operation {
    GET_CONFIGURATION {
      @Override Response invoke(TwinConfigurationApi api) { return api.getTwinConfiguration(); }
    },
    GET_CORE {
      @Override Response invoke(TwinConfigurationApi api) { return api.getCoreConfig(); }
    },
    UPDATE_CORE {
      @Override Response invoke(TwinConfigurationApi api) {
        return api.updateCoreConfig(new TwinCoreConfigDTO());
      }
    },
    GET_TAK {
      @Override Response invoke(TwinConfigurationApi api) { return api.getTakConfig(); }
    },
    PUT_TAK {
      @Override Response invoke(TwinConfigurationApi api) {
        return api.putTakConfig(new TakProtocolDTO());
      }
    },
    DELETE_TAK {
      @Override Response invoke(TwinConfigurationApi api) { return api.deleteTakConfig(); }
    },
    GET_PUBLISH {
      @Override Response invoke(TwinConfigurationApi api) { return api.getPublishConfig(); }
    },
    PUT_PUBLISH {
      @Override Response invoke(TwinConfigurationApi api) {
        return api.putPublishConfig(new TwinPublishConfigDTO());
      }
    },
    DELETE_PUBLISH {
      @Override Response invoke(TwinConfigurationApi api) { return api.deletePublishConfig(); }
    },
    GET_N2K {
      @Override Response invoke(TwinConfigurationApi api) { return api.getN2kConfig(); }
    },
    PUT_N2K {
      @Override Response invoke(TwinConfigurationApi api) {
        return api.putN2kConfig(new N2KTwinConfig());
      }
    },
    DELETE_N2K {
      @Override Response invoke(TwinConfigurationApi api) { return api.deleteN2kConfig(); }
    },
    LIST_MAVLINK {
      @Override Response invoke(TwinConfigurationApi api) { return api.listMavlinkSources(); }
    },
    GET_MAVLINK {
      @Override Response invoke(TwinConfigurationApi api) { return api.getMavlinkSource("source"); }
    },
    CREATE_MAVLINK {
      @Override Response invoke(TwinConfigurationApi api) {
        return api.createMavlinkSource(new MavlinkTwinConfigDTO());
      }
    },
    UPDATE_MAVLINK {
      @Override Response invoke(TwinConfigurationApi api) {
        return api.updateMavlinkSource("source", new MavlinkTwinConfigDTO());
      }
    },
    DELETE_MAVLINK {
      @Override Response invoke(TwinConfigurationApi api) { return api.deleteMavlinkSource("source"); }
    },
    LIST_MODELS {
      @Override Response invoke(TwinConfigurationApi api) { return api.listDroneModels(); }
    },
    LIST_AREAS {
      @Override Response invoke(TwinConfigurationApi api) { return api.listGeospatialAreaNames(); }
    },
    LIST_DRONES {
      @Override Response invoke(TwinConfigurationApi api) { return api.listDrones(); }
    },
    GET_DRONE {
      @Override Response invoke(TwinConfigurationApi api) { return api.getDrone("drone"); }
    },
    CREATE_DRONE {
      @Override Response invoke(TwinConfigurationApi api) {
        return api.createDrone(new DroneInfoDTO());
      }
    },
    UPDATE_DRONE {
      @Override Response invoke(TwinConfigurationApi api) {
        return api.updateDrone("drone", new DroneInfoDTO());
      }
    },
    DELETE_DRONE {
      @Override Response invoke(TwinConfigurationApi api) { return api.deleteDrone("drone"); }
    },
    UPDATE_AUTHORITIES {
      @Override Response invoke(TwinConfigurationApi api) {
        return api.updateAuthorityBindings("authority", new AuthorityBindingsUpdateDTO());
      }
    },
    DELETE_AUTHORITY {
      @Override Response invoke(TwinConfigurationApi api) { return api.deleteAuthority("authority"); }
    },
    LIST_ADAPTERS {
      @Override Response invoke(TwinConfigurationApi api) { return api.listAdapterConfigs(); }
    },
    REPLACE_ADAPTERS {
      @Override Response invoke(TwinConfigurationApi api) {
        return api.replaceAdapterConfigs(Map.of("adapter", Map.of("enabled", true)));
      }
    },
    GET_ADAPTER {
      @Override Response invoke(TwinConfigurationApi api) { return api.getAdapterConfig("adapter"); }
    },
    CREATE_ADAPTER {
      @Override Response invoke(TwinConfigurationApi api) {
        return api.createAdapterConfig("adapter", new LinkedHashMap<>());
      }
    },
    UPDATE_ADAPTER {
      @Override Response invoke(TwinConfigurationApi api) {
        return api.updateAdapterConfig("adapter", new LinkedHashMap<>());
      }
    },
    DELETE_ADAPTER {
      @Override Response invoke(TwinConfigurationApi api) { return api.deleteAdapterConfig("adapter"); }
    };

    abstract Response invoke(TwinConfigurationApi api);
  }

  private enum AccessFailure {
    NONE,
    UNAUTHORIZED,
    FORBIDDEN,
    OTHER
  }

  private static final class TestApi extends TwinConfigurationApi {
    private final AccessFailure accessFailure;
    private final boolean storeUnavailable;

    private TestApi(AccessFailure accessFailure, boolean storeUnavailable) {
      this.accessFailure = accessFailure;
      this.storeUnavailable = storeUnavailable;
    }

    @Override
    protected void hasAccess(String resource) {
      switch (accessFailure) {
        case UNAUTHORIZED ->
            throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).build());
        case FORBIDDEN ->
            throw new WebApplicationException(Response.status(Response.Status.FORBIDDEN).build());
        case OTHER -> throw new WebApplicationException(Response.status(418).build());
        case NONE -> {
          // access granted
        }
      }
    }

    @Override
    TwinConfigurationStore store() {
      if (storeUnavailable) {
        throw new IllegalStateException("configuration unavailable");
      }
      throw new AssertionError("store must not be reached for access-control failures");
    }
  }
}
