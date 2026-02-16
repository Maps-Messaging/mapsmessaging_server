package io.mapsmessaging.dto.rest.config.transformer;

import com.google.gson.annotations.SerializedName;
import lombok.Getter;

@Getter
public enum TransformationType {

  @SerializedName("jsontoxml")
  JSON_TO_XML("jsontoxml"),

  @SerializedName("xmltojson")
  XML_TO_JSON("xmltojson"),

  @SerializedName("jsontovalue")
  JSON_TO_VALUE("jsontovalue"),

  @SerializedName("jsonquery")
  JSON_QUERY("jsonquery"),

  @SerializedName("geohash")
  GEOHASH("geohash"),

  @SerializedName("schematojson")
  SCHEMA_TO_JSON("schematojson"),

  @SerializedName("jsonmutate")
  JSON_MUTATE("jsonmutate");

  private final String wireName;

  TransformationType(String wireName) {
    this.wireName = wireName;
  }


  public static TransformationType fromWireName(String value) {
    if (value == null) {
      throw new IllegalArgumentException("Transformation type is null");
    }
    for (TransformationType type : values()) {
      if (type.wireName.equalsIgnoreCase(value)) {
        return type;
      }
    }

    throw new IllegalArgumentException("Unknown transformation type: " + value);
  }
}
