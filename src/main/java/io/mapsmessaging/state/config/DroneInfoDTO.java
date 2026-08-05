/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 */

package io.mapsmessaging.state.config;

import io.mapsmessaging.state.config.capability.TaskCapabilities;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DroneInfoDTO {

  @Schema(description = "Unique drone id")
  private String name;

  @Schema(description = "UUID of drone")
  private UUID uuid;

  @Schema(description = "Configured UxV model name used to resolve the command model implementation.")
  private String modelName;

  @Schema(description = "Named geospatial operating area assigned to this drone.")
  private String geospatialArea;

  @Schema(description = "Message encoding used when communicating with this drone.")
  private MessageEncodingEnum messageEncoding = MessageEncodingEnum.JSON;

  @Schema(description = "Total battery capacity in amp-hours.")
  private double batteryCapacityAh = 0.0;

  @Schema(description = "Total battery capacity in hours.")
  private double batteryCapacityHours = 0.0;

  @Schema(
      description =
          "Survey coverage radius in metres measured from the vehicle centreline. "
              + "The effective survey width is twice this value.",
      example = "200.0",
      nullable = true)
  private Double surveyRadiusMeters;

  @Schema(description = "Drone description")
  private Map<String, Object> description;

  @Schema(description = "Optional external data products exposed by this drone.")
  private List<DataProductConfig> dataProducts = new ArrayList<>();

  @Schema(description = "Task capabilities supported by this known MAVLink source.")
  private TaskCapabilities capabilities = new TaskCapabilities();

  @Schema(description = "Action performed when the current task is cancelled.")
  private StopActionEnum cancelAction = StopActionEnum.STOP;

  @Schema(description = "Action performed when a mission completes normally.")
  private StopActionEnum missionEndAction = StopActionEnum.STOP;

  @Schema(description = "Action performed when a mission exceeds its configured duration.")
  private StopActionEnum missionTimeoutAction = StopActionEnum.STOP;

  public List<DataProductConfig> getDataProducts() {
    if (dataProducts == null || dataProducts.isEmpty()) {
      dataProducts = new ArrayList<>(DataProductConfigLoader.load(name, uuid));
    }
    return List.copyOf(dataProducts);
  }

  public void setDataProducts(List<DataProductConfig> dataProducts) {
    this.dataProducts =
        dataProducts == null ? new ArrayList<>() : new ArrayList<>(dataProducts);
  }

  @Deprecated
  public StopActionEnum getStopAction() {
    return cancelAction;
  }

  @Deprecated
  public void setStopAction(StopActionEnum stopAction) {
    cancelAction = stopAction;
  }
}
