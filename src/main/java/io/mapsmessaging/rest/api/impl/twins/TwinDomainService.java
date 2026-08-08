/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 */
package io.mapsmessaging.rest.api.impl.twins;

import io.mapsmessaging.dto.rest.config.protocol.impl.MavlinkKnownSourceDTO;
import io.mapsmessaging.state.config.DroneInfoDTO;
import io.mapsmessaging.state.config.MavlinkTwinConfigDTO;
import io.mapsmessaging.state.config.TwinManagerConfig;
import io.mapsmessaging.state.config.VehicleClass;
import io.mapsmessaging.state.config.capability.Authorities;
import io.mapsmessaging.state.config.capability.TaskCapability;
import io.mapsmessaging.state.config.geospatial.GeoSpatialAreaConfigDTO;
import io.mapsmessaging.state.config.n2k.N2KTwinConfig;
import io.mapsmessaging.state.mavlink.model.ModelManager;
import io.mapsmessaging.state.mavlink.model.UxvModel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

final class TwinDomainService {

  private static final Object CONFIG_LOCK = new Object();
  private final TwinManagerConfig config;

  TwinDomainService(TwinManagerConfig config) {
    this.config = Objects.requireNonNull(config, "config must not be null");
  }

  List<DroneAdminDTO> listDrones() {
    return config.getDroneInfo().stream().map(this::toAdminDrone).toList();
  }

  Optional<DroneAdminDTO> getDrone(String name) {
    return findDrone(name).map(this::toAdminDrone);
  }

  DroneAdminDTO createDrone(DroneAdminDTO request) throws IOException {
    validateRequest(request);
    synchronized (CONFIG_LOCK) {
      DroneInfoDTO drone = request.getDrone();
      if (findDrone(drone.getName()).isPresent()) {
        throw new TwinConfigurationStore.TwinConfigurationException("Drone configuration already exists: " + drone.getName(), 409);
      }
      if (config.getDroneInfo().stream().anyMatch(existing -> drone.getUuid().equals(existing.getUuid()))) {
        throw new TwinConfigurationStore.TwinConfigurationException("Drone UUID is already configured: " + drone.getUuid(), 409);
      }
      validateTransport(request.getTransport(), drone.getName(), null);
      config.getDroneInfo().add(drone);
      applyTransport(drone.getName(), request.getTransport());
      config.save();
      return toAdminDrone(drone);
    }
  }

  DroneAdminDTO updateDrone(String name, DroneAdminDTO request) throws IOException {
    validateName(name);
    validateRequest(request);
    if (!name.equals(request.getDrone().getName())) {
      throw new TwinConfigurationStore.TwinConfigurationException("Drone configuration name cannot be changed", 400);
    }
    synchronized (CONFIG_LOCK) {
      DroneInfoDTO existing = findDrone(name).orElseThrow(() -> new TwinConfigurationStore.TwinConfigurationException("Unknown drone configuration: " + name, 404));
      if (config.getDroneInfo().stream().anyMatch(candidate -> candidate != existing && request.getDrone().getUuid().equals(candidate.getUuid()))) {
        throw new TwinConfigurationStore.TwinConfigurationException("Drone UUID is already configured: " + request.getDrone().getUuid(), 409);
      }
      validateTransport(request.getTransport(), name, name);
      int index = config.getDroneInfo().indexOf(existing);
      config.getDroneInfo().set(index, request.getDrone());
      removeTransport(name);
      applyTransport(name, request.getTransport());
      config.save();
      return toAdminDrone(request.getDrone());
    }
  }

  void deleteDrone(String name) throws IOException {
    validateName(name);
    synchronized (CONFIG_LOCK) {
      if (config.getDroneInfo().removeIf(drone -> name.equals(drone.getName()))) {
        removeTransport(name);
        config.save();
        return;
      }
      throw new TwinConfigurationStore.TwinConfigurationException("Unknown drone configuration: " + name, 404);
    }
  }

  List<AuthoritySummaryDTO> listAuthorities() {
    Map<UUID, List<AuthoritySummaryDTO.DroneBinding>> result = new LinkedHashMap<>();
    for (DroneInfoDTO drone : config.getDroneInfo()) {
      List<TaskCapability> tasks = tasks(drone);
      Map<UUID, Integer> assigned = new LinkedHashMap<>();
      for (TaskCapability task : tasks) {
        for (Authorities authority : authorities(task)) {
          if (authority != null && authority.getGuid() != null) {
            assigned.merge(authority.getGuid(), 1, Integer::sum);
          }
        }
      }
      for (Map.Entry<UUID, Integer> entry : assigned.entrySet()) {
        result.computeIfAbsent(entry.getKey(), ignored -> new ArrayList<>()).add(new AuthoritySummaryDTO.DroneBinding(drone.getName(), entry.getValue(), tasks.size()));
      }
    }
    return result.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> new AuthoritySummaryDTO(entry.getKey(), entry.getValue())).toList();
  }

  Optional<AuthoritySummaryDTO> getAuthority(UUID authority) {
    return listAuthorities().stream().filter(summary -> authority.equals(summary.getUuid())).findFirst();
  }

  AuthoritySummaryDTO updateAuthorityBindings(UUID authority, AuthorityBindingDTO request) throws IOException {
    if (authority == null) {
      throw new TwinConfigurationStore.TwinConfigurationException("authority UUID is required", 400);
    }
    if (request == null) {
      throw new TwinConfigurationStore.TwinConfigurationException("authority binding request is required", 400);
    }
    Set<String> additions = new LinkedHashSet<>(request.getAddDrones() == null ? List.of() : request.getAddDrones());
    Set<String> removals = new LinkedHashSet<>(request.getRemoveDrones() == null ? List.of() : request.getRemoveDrones());
    Set<String> overlap = new LinkedHashSet<>(additions);
    overlap.retainAll(removals);
    if (!overlap.isEmpty()) {
      throw new TwinConfigurationStore.TwinConfigurationException("A drone cannot be added and removed in the same authority update: " + overlap, 400);
    }
    for (String droneName : additions) {
      requireDrone(droneName);
    }
    for (String droneName : removals) {
      requireDrone(droneName);
    }
    synchronized (CONFIG_LOCK) {
      boolean changed = false;
      for (String droneName : additions) {
        changed |= setDroneAuthority(requireDrone(droneName), authority, true);
      }
      for (String droneName : removals) {
        changed |= setDroneAuthority(requireDrone(droneName), authority, false);
      }
      if (changed) {
        config.save();
      }
      return getAuthority(authority).orElse(new AuthoritySummaryDTO(authority, List.of()));
    }
  }

  void deleteAuthority(UUID authority) throws IOException {
    synchronized (CONFIG_LOCK) {
      boolean changed = false;
      for (DroneInfoDTO drone : config.getDroneInfo()) {
        changed |= setDroneAuthority(drone, authority, false);
      }
      if (!changed) {
        throw new TwinConfigurationStore.TwinConfigurationException("Unknown authority: " + authority, 404);
      }
      config.save();
    }
  }

  List<UUID> listDroneAuthorities(String name) {
    DroneInfoDTO drone = requireDrone(name);
    Set<UUID> values = new LinkedHashSet<>();
    for (TaskCapability task : tasks(drone)) {
      for (Authorities authority : authorities(task)) {
        if (authority != null && authority.getGuid() != null) {
          values.add(authority.getGuid());
        }
      }
    }
    return values.stream().sorted().toList();
  }

  void putDroneAuthority(String name, UUID authority) throws IOException {
    mutateDroneAuthority(name, authority, true);
  }

  void deleteDroneAuthority(String name, UUID authority) throws IOException {
    mutateDroneAuthority(name, authority, false);
  }

  List<UxvModelSummaryDTO> listModels() {
    return ModelManager.getInstance().getModels().stream().sorted(Comparator.comparing(UxvModel::getModelName)).map(model -> new UxvModelSummaryDTO(model.getModelName(), model.getVehicleType(), model.getSupportedOperations())).toList();
  }

  Optional<UxvModelSummaryDTO> getModel(String modelName) {
    return ModelManager.getInstance().getModel(modelName).map(model -> new UxvModelSummaryDTO(model.getModelName(), model.getVehicleType(), model.getSupportedOperations()));
  }

  List<GeoSpatialAreaConfigDTO> listGeospatialAreas() {
    if (config.getGeospatial() == null || config.getGeospatial().getAreas() == null) {
      return List.of();
    }
    return List.copyOf(config.getGeospatial().getAreas());
  }

  List<MavlinkTwinConfigDTO> listMavlinkSources() {
    return List.copyOf(config.getMavlink());
  }

  private void mutateDroneAuthority(String name, UUID authority, boolean add) throws IOException {
    validateName(name);
    if (authority == null) {
      throw new TwinConfigurationStore.TwinConfigurationException("authority UUID is required", 400);
    }
    synchronized (CONFIG_LOCK) {
      DroneInfoDTO drone = requireDrone(name);
      boolean changed = setDroneAuthority(drone, authority, add);
      if (!changed && !add) {
        throw new TwinConfigurationStore.TwinConfigurationException("Authority is not assigned to drone " + name + ": " + authority, 404);
      }
      if (changed) {
        config.save();
      }
    }
  }

  private boolean setDroneAuthority(DroneInfoDTO drone, UUID authority, boolean add) {
    boolean changed = false;
    for (TaskCapability task : tasks(drone)) {
      List<Authorities> current = new ArrayList<>(Arrays.asList(authorities(task)));
      boolean contains = current.stream().filter(Objects::nonNull).map(Authorities::getGuid).anyMatch(authority::equals);
      if (add && !contains) {
        current.add(new Authorities(authority));
        changed = true;
      } else if (!add && contains) {
        current.removeIf(value -> value != null && authority.equals(value.getGuid()));
        changed = true;
      }
      task.setAuthorities(current.toArray(new Authorities[0]));
    }
    return changed;
  }

  private void validateRequest(DroneAdminDTO request) {
    if (request == null || request.getDrone() == null) {
      throw new TwinConfigurationStore.TwinConfigurationException("Drone configuration is required", 400);
    }
    validateName(request.getDrone().getName());
    if (request.getDrone().getUuid() == null) {
      throw new TwinConfigurationStore.TwinConfigurationException("uuid is required", 400);
    }
    if (request.getTransport() == null) {
      request.setTransport(new DroneTransportDTO());
    }
  }

  private void validateTransport(DroneTransportDTO transport, String droneName, String currentDroneName) {
    if (transport == null || transport.getType() == DroneTransportDTO.Type.NONE) {
      return;
    }
    if (transport.getType() == DroneTransportDTO.Type.MAVLINK) {
      if (transport.getMavlinkSourceName() == null || transport.getMavlinkSourceName().isBlank()) {
        throw new TwinConfigurationStore.TwinConfigurationException("mavlinkSourceName is required for MAVLINK transport", 400);
      }
      if (transport.getSystemId() == null || transport.getSystemId() < 1 || transport.getSystemId() > 255) {
        throw new TwinConfigurationStore.TwinConfigurationException("systemId must be between 1 and 255", 400);
      }
      if (transport.getComponentId() == null || transport.getComponentId() < 0 || transport.getComponentId() > 255) {
        throw new TwinConfigurationStore.TwinConfigurationException("componentId must be between 0 and 255", 400);
      }
      MavlinkTwinConfigDTO source = findMavlinkSource(transport.getMavlinkSourceName()).orElseThrow(() -> new TwinConfigurationStore.TwinConfigurationException("Unknown MAVLink source: " + transport.getMavlinkSourceName(), 404));
      for (MavlinkKnownSourceDTO known : source.getKnownSources()) {
        if (known.getSystemId() == transport.getSystemId() && known.getComponentId() == transport.getComponentId() && !Objects.equals(known.getName(), currentDroneName)) {
          throw new TwinConfigurationStore.TwinConfigurationException("MAVLink " + transport.getSystemId() + "/" + transport.getComponentId() + " is already mapped to " + known.getName(), 409);
        }
      }
      return;
    }
    N2KTwinConfig n2k = config.getN2KTwinConfig();
    if (n2k != null && n2k.isEnable() && n2k.getName() != null && !n2k.getName().isBlank() && !Objects.equals(n2k.getName(), currentDroneName) && !n2k.getName().equals(droneName)) {
      throw new TwinConfigurationStore.TwinConfigurationException("Only one CAN/N2K drone is currently supported; existing device is " + n2k.getName(), 409);
    }
  }

  private void applyTransport(String droneName, DroneTransportDTO transport) {
    if (transport == null || transport.getType() == DroneTransportDTO.Type.NONE) {
      return;
    }
    if (transport.getType() == DroneTransportDTO.Type.MAVLINK) {
      MavlinkTwinConfigDTO source = findMavlinkSource(transport.getMavlinkSourceName()).orElseThrow();
      MavlinkKnownSourceDTO knownSource = new MavlinkKnownSourceDTO();
      knownSource.setName(droneName);
      knownSource.setDescription(droneName);
      knownSource.setSystemId(transport.getSystemId());
      knownSource.setComponentId(transport.getComponentId());
      knownSource.setVehicleClass(transport.getVehicleClass());
      source.getKnownSources().add(knownSource);
      return;
    }
    N2KTwinConfig n2k = config.getN2KTwinConfig();
    if (n2k == null) {
      n2k = new N2KTwinConfig();
      config.setN2KTwinConfig(n2k);
    }
    n2k.setEnable(true);
    n2k.setName(droneName);
    if (transport.getTopic() != null && !transport.getTopic().isBlank()) {
      n2k.setTopic(transport.getTopic());
    }
    if (transport.getVehicleClass() != null) {
      n2k.setVehicleClass(transport.getVehicleClass().name());
    }
  }

  private void removeTransport(String droneName) {
    for (MavlinkTwinConfigDTO source : config.getMavlink()) {
      source.getKnownSources().removeIf(known -> droneName.equals(known.getName()));
    }
    N2KTwinConfig n2k = config.getN2KTwinConfig();
    if (n2k != null && droneName.equals(n2k.getName())) {
      n2k.setEnable(false);
      n2k.setName(null);
    }
  }

  private DroneAdminDTO toAdminDrone(DroneInfoDTO drone) {
    DroneAdminDTO result = new DroneAdminDTO();
    result.setDrone(drone);
    result.setTransport(resolveTransport(drone.getName()));
    return result;
  }

  private DroneTransportDTO resolveTransport(String droneName) {
    for (MavlinkTwinConfigDTO source : config.getMavlink()) {
      for (MavlinkKnownSourceDTO known : source.getKnownSources()) {
        if (droneName.equals(known.getName())) {
          DroneTransportDTO transport = new DroneTransportDTO();
          transport.setType(DroneTransportDTO.Type.MAVLINK);
          transport.setMavlinkSourceName(source.getName());
          transport.setSystemId(known.getSystemId());
          transport.setComponentId(known.getComponentId());
          transport.setVehicleClass(known.getVehicleClass());
          return transport;
        }
      }
    }
    N2KTwinConfig n2k = config.getN2KTwinConfig();
    if (n2k != null && n2k.isEnable() && droneName.equals(n2k.getName())) {
      DroneTransportDTO transport = new DroneTransportDTO();
      transport.setType(DroneTransportDTO.Type.CANBUS);
      transport.setTopic(n2k.getTopic());
      if (n2k.getVehicleClass() != null) {
        try {
          transport.setVehicleClass(VehicleClass.valueOf(n2k.getVehicleClass()));
        } catch (IllegalArgumentException ignored) {
          transport.setVehicleClass(VehicleClass.UNKNOWN);
        }
      }
      return transport;
    }
    return new DroneTransportDTO();
  }

  private DroneInfoDTO requireDrone(String name) {
    validateName(name);
    return findDrone(name).orElseThrow(() -> new TwinConfigurationStore.TwinConfigurationException("Unknown drone configuration: " + name, 404));
  }

  private Optional<DroneInfoDTO> findDrone(String name) {
    if (name == null) {
      return Optional.empty();
    }
    return config.getDroneInfo().stream().filter(drone -> name.equals(drone.getName())).findFirst();
  }

  private Optional<MavlinkTwinConfigDTO> findMavlinkSource(String name) {
    return config.getMavlink().stream().filter(source -> name.equals(source.getName())).findFirst();
  }

  private List<TaskCapability> tasks(DroneInfoDTO drone) {
    if (drone.getCapabilities() == null || drone.getCapabilities().getTasks() == null) {
      return List.of();
    }
    return drone.getCapabilities().getTasks().stream().filter(Objects::nonNull).toList();
  }

  private Authorities[] authorities(TaskCapability task) {
    return task.getAuthorities() == null ? new Authorities[0] : task.getAuthorities();
  }

  private void validateName(String name) {
    if (name == null || name.isBlank()) {
      throw new TwinConfigurationStore.TwinConfigurationException("name is required", 400);
    }
  }
}
