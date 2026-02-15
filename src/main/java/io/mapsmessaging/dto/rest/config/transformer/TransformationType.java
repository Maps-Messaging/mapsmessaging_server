package io.mapsmessaging.dto.rest.config.transformer;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public enum TransformationType {

  @SerializedName("json-to-xml")
  JSON_TO_XML("json-to-xml"),

  @SerializedName("xml-to-json")
  XML_TO_JSON("xml-to-json"),

  @SerializedName("json-to-value")
  JSON_TO_VALUE("json-to-value"),

  @SerializedName("json-query")
  JSON_QUERY("json-query"),

  @SerializedName("geohash")
  GEOHASH("geohash"),

  @SerializedName("schema-to-json")
  SCHEMA_TO_JSON("schema-to-json"),

  @SerializedName("json-mutate")
  JSON_MUTATE("json-mutate");

  private final String wireName;

  TransformationType(String wireName) {
    this.wireName = wireName;
  }


  public static TransformationType fromWireName(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Transformation type is null");
    }

    String normalized = value.trim().toLowerCase();

    for (TransformationType type : values()) {
      if (type.wireName.equals(normalized)) {
        return type;
      }
    }

    throw new IllegalArgumentException("Unknown transformation type: " + value);
  }
}
