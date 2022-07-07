package io.mapsmessaging.engine.schema;

import io.mapsmessaging.schemas.config.SchemaConfig;
import lombok.Getter;

public class Schema {

  @Getter
  private String uniqueId;

  public Schema(SchemaConfig config) {
    uniqueId = config.getUniqueId();
  }

  public boolean update(Schema rhs) {
    uniqueId = rhs.uniqueId;
    return true;
  }
}
