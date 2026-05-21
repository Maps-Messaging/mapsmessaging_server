package io.mapsmessaging.network.protocol.impl.mavlink;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class GsonFactoryTest {

  private final Gson gson = GsonFactory.createStrictJsonWithSafeFloats();

  @Test
  void write_nonFiniteBoxedValues_areOmitted_notSerializedAsNaNOrInfinity() {
    BoxedNumbers data = new BoxedNumbers();
    data.doubleValue = Double.NaN;
    data.floatValue = Float.POSITIVE_INFINITY;

    String json = gson.toJson(data);

    assertEquals("{}", json);
    assertFalse(json.contains("NaN"), json);
    assertFalse(json.contains("Infinity"), json);
  }

  @Test
  void write_nonFinitePrimitiveValues_areOmitted_notSerializedAsNaNOrInfinity() {
    PrimitiveNumbers data = new PrimitiveNumbers();
    data.doubleValue = Double.NEGATIVE_INFINITY;
    data.floatValue = Float.NaN;

    String json = gson.toJson(data);

    assertEquals("{}", json);
    assertFalse(json.contains("NaN"), json);
    assertFalse(json.contains("Infinity"), json);
  }

  @Test
  void write_mixedFiniteAndNonFinite_onlyFiniteSurvives() {
    BoxedNumbers data = new BoxedNumbers();
    data.doubleValue = 12.5;
    data.floatValue = Float.NaN;

    String json = gson.toJson(data);
    JsonObject obj = JsonParser.parseString(json).getAsJsonObject();

    assertTrue(obj.has("doubleValue"), json);
    assertEquals(12.5, obj.get("doubleValue").getAsDouble(), 0.0);

    assertFalse(obj.has("floatValue"), json);
    assertFalse(json.contains("NaN"), json);
    assertFalse(json.contains("Infinity"), json);
  }

  @Test
  void write_finiteValues_arePreserved() {
    BoxedNumbers boxed = new BoxedNumbers();
    boxed.doubleValue = 12.5;
    boxed.floatValue = 3.25f;

    PrimitiveNumbers prim = new PrimitiveNumbers();
    prim.doubleValue = -99.125;
    prim.floatValue = 0.5f;

    String jsonBoxed = gson.toJson(boxed);
    String jsonPrim = gson.toJson(prim);

    JsonObject objBoxed = JsonParser.parseString(jsonBoxed).getAsJsonObject();
    assertEquals(12.5, objBoxed.get("doubleValue").getAsDouble(), 0.0);
    assertEquals(3.25f, objBoxed.get("floatValue").getAsFloat(), 0.0001f);

    JsonObject objPrim = JsonParser.parseString(jsonPrim).getAsJsonObject();
    assertEquals(-99.125, objPrim.get("doubleValue").getAsDouble(), 0.0);
    assertEquals(0.5f, objPrim.get("floatValue").getAsFloat(), 0.0001f);
  }

  @Test
  void write_nonFinite_directBoxedSerializesAsNull_provesAdapterIsActive() {
    assertEquals("null", gson.toJson(Double.NaN, Double.class));
    assertEquals("null", gson.toJson(Double.POSITIVE_INFINITY, Double.class));
    assertEquals("null", gson.toJson(Float.NaN, Float.class));
    assertEquals("null", gson.toJson(Float.NEGATIVE_INFINITY, Float.class));
  }

  @Test
  void read_stringNaNAndInfinity_mapToNull_forBoxed() {
    BoxedNumbers data = gson.fromJson("{\"doubleValue\":\"NaN\",\"floatValue\":\"Infinity\"}", BoxedNumbers.class);
    assertNull(data.doubleValue);
    assertNull(data.floatValue);

    BoxedNumbers data2 = gson.fromJson("{\"doubleValue\":\"-Infinity\",\"floatValue\":\"-Infinity\"}", BoxedNumbers.class);
    assertNull(data2.doubleValue);
    assertNull(data2.floatValue);
  }

  @Test
  void read_numericValues_parse_forBoxedAndPrimitive() {
    BoxedNumbers boxed = gson.fromJson("{\"doubleValue\":1.25,\"floatValue\":2.5}", BoxedNumbers.class);
    assertEquals(1.25, boxed.doubleValue, 0.0);
    assertEquals(2.5f, boxed.floatValue, 0.0001f);

    PrimitiveNumbers prim = gson.fromJson("{\"doubleValue\":-7.0,\"floatValue\":0.125}", PrimitiveNumbers.class);
    assertEquals(-7.0, prim.doubleValue, 0.0);
    assertEquals(0.125f, prim.floatValue, 0.0001f);
  }

  public static final class BoxedNumbers {
    public Double doubleValue;
    public Float floatValue;
  }

  public static final class PrimitiveNumbers {
    public double doubleValue;
    public float floatValue;
  }
}
