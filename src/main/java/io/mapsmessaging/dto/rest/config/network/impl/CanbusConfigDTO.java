package io.mapsmessaging.dto.rest.config.network.impl;

import io.mapsmessaging.canbus.device.QueueFullPolicy;
import io.mapsmessaging.dto.rest.config.network.EndPointConfigDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Schema(
    name = "CanbusConfig",
    description = "Configuration for a CAN bus endpoint. Supports native SocketCAN interfaces and serial CAN adapters."
)
public class CanbusConfigDTO extends EndPointConfigDTO {

  public static final boolean DEFAULT_QUEUED_WRITES_ENABLED = true;
  public static final int DEFAULT_QUEUE_DEPTH = 128;
  public static final int DEFAULT_BITRATE_BITS_PER_SECOND = 250_000;
  public static final double DEFAULT_MAX_BUS_USAGE_PERCENT = 20.0;
  public static final QueueFullPolicy DEFAULT_QUEUE_FULL_POLICY = QueueFullPolicy.DROP_OLDEST;
  public static final long DEFAULT_WRITE_FAILURE_BACKOFF_MILLISECONDS = 100L;

  public CanbusConfigDTO() {
    super("canbus");
  }

  @Schema(
      description = "CAN bus device name. For SocketCAN this is usually the Linux interface name, such as can0. For serial adapters this is a logical device name.",
      example = "can0",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected String deviceName;

  @Schema(
      description = "Serial configuration used when the CAN bus device is accessed through a serial adapter instead of a native SocketCAN interface.",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED,
      nullable = true
  )
  protected SerialConfigDTO serialConfig;

  @Schema(
      description = "Enables asynchronous queued writes for the CAN bus device. When enabled, writes are queued as logical messages so multi-frame CAN/N2K messages are accepted, dropped, or rejected as a unit.",
      example = "true",
      defaultValue = "true",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected boolean queuedWritesEnabled = DEFAULT_QUEUED_WRITES_ENABLED;

  @Schema(
      description = "Maximum number of logical CAN messages held in the outbound queue. A logical message may contain one CAN frame or multiple frames, such as an N2K fast-packet message.",
      example = "128",
      defaultValue = "128",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected int queueDepth = DEFAULT_QUEUE_DEPTH;

  @Schema(
      description = "CAN bus bitrate in bits per second. Used by the queued writer to estimate outbound bandwidth usage.",
      example = "250000",
      defaultValue = "250000",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected int bitrateBitsPerSecond = DEFAULT_BITRATE_BITS_PER_SECOND;

  @Schema(
      description = "Maximum percentage of the CAN bus bandwidth that queued writes should consume during normal operation.",
      example = "20.0",
      defaultValue = "20.0",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected double maxBusUsagePercent = DEFAULT_MAX_BUS_USAGE_PERCENT;

  @Schema(
      description = "Policy used when the outbound CAN queue is full. DROP_OLDEST removes the oldest queued logical message. REJECT_NEW rejects the new logical message.",
      example = "DROP_OLDEST",
      defaultValue = "DROP_OLDEST",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected QueueFullPolicy queueFullPolicy = DEFAULT_QUEUE_FULL_POLICY;

  @Schema(
      description = "Backoff delay in milliseconds after a failed CAN write before the writer thread attempts the next queued logical message.",
      example = "100",
      defaultValue = "100",
      requiredMode = Schema.RequiredMode.NOT_REQUIRED
  )
  protected long writeFailureBackoffMilliseconds = DEFAULT_WRITE_FAILURE_BACKOFF_MILLISECONDS;
}