package io.mapsmessaging.dto.rest.config.destination;

import io.mapsmessaging.dto.rest.config.BaseConfigDTO;
import io.mapsmessaging.dto.rest.config.S3Config;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema( description = "Configuration for deferred partition handling, including archive strategies, migration settings, and checksum algorithms.")
public class DeferredConfigDTO extends BaseConfigDTO {

  @Schema(
      description = "Archive strategy for rotated partitions: - **None**: No deferral; data remains in place. - **S3**: Move partitions to S3-compatible storage. - **Compress**: Compress data locally on disk. - **Migrate**: Move data to another (typically slower or network-based) store.",
      defaultValue = "None"
  )
  private String deferredName = "None";

  @Schema(
      description = "Time in milliseconds after which data should be archived",
      defaultValue = "-1"
  )
  private long idleTime = -1L;

  @Schema(
      description = "Destination directory or location for data migration"
  )
  private String migrationDestination;

  @Schema(
      description = "Digest algorithm name for checksums (e.g., SHA-256)"
  )
  private String digestName = "";

  @Schema(
      description = "Optional S3 configuration parameters to use to push stale partitions to"
  )
  private S3Config s3Config;

}
