package io.mapsmessaging.rest.data;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.time.LocalDateTime;
import lombok.Getter;

public class SchemaData {

  @Getter
  private final String format;
  @Getter
  private final String uniqueId;
  @Getter
  private final LocalDateTime creation;
  @Getter
  private final LocalDateTime expiresAfter;
  @Getter
  private final LocalDateTime notBefore;
  @Getter
  private final String comments;
  @Getter
  private final String version;
  @Getter
  private final String source;
  @Getter
  private final String mimeType;
  @Getter
  private final String resourceType;
  @Getter
  private final String interfaceDescription;

  public SchemaData(SchemaConfig schema){
    format  = schema.getFormat();
    uniqueId = schema.getUniqueId();
    creation = schema.getCreation();
    expiresAfter = schema.getExpiresAfter();
    notBefore = schema.getNotBefore();
    comments = schema.getComments();
    version = schema.getVersion();
    source = schema.getSource();
    mimeType = schema.getMimeType();
    resourceType = schema.getResourceType();
    interfaceDescription = schema.getInterfaceDescription();
  }

}
