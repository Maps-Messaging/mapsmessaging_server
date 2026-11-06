package io.mapsmessaging.tools.config.yaml;

import com.google.gson.JsonNull;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JsonElementConverterTest {

  @Test
  void nullAndPrimitiveValuesConvertToJavaTypes() {
    assertNull(JsonElementConverter.toJava(null));
    assertNull(JsonElementConverter.toJava(JsonNull.INSTANCE));
    assertEquals(true, JsonElementConverter.toJava(JsonParser.parseString("true")));
    assertEquals("maps", JsonElementConverter.toJava(JsonParser.parseString("\"maps\"")));

    Number number = assertInstanceOf(
        Number.class,
        JsonElementConverter.toJava(JsonParser.parseString("12.5"))
    );
    assertEquals(12.5, number.doubleValue(), 0.0);
  }

  @Test
  void arraysConvertRecursivelyAndPreserveNulls() {
    Object converted = JsonElementConverter.toJava(
        JsonParser.parseString("[1,true,\"x\",null,{\"a\":2}]")
    );

    List<?> list = assertInstanceOf(List.class, converted);
    assertEquals(5, list.size());
    assertEquals(1, ((Number) list.get(0)).intValue());
    assertEquals(true, list.get(1));
    assertEquals("x", list.get(2));
    assertNull(list.get(3));

    Map<?, ?> nested = assertInstanceOf(Map.class, list.get(4));
    assertEquals(2, ((Number) nested.get("a")).intValue());
  }

  @Test
  void objectsConvertRecursivelyInInsertionOrder() {
    Object converted = JsonElementConverter.toJava(
        JsonParser.parseString("{\"first\":1,\"second\":[2,3],\"third\":{\"ok\":true}}")
    );

    Map<?, ?> map = assertInstanceOf(Map.class, converted);
    assertEquals(List.of("first", "second", "third"), List.copyOf(map.keySet()));

    List<?> second = assertInstanceOf(List.class, map.get("second"));
    assertEquals(2, ((Number) second.get(0)).intValue());

    Map<?, ?> third = assertInstanceOf(Map.class, map.get("third"));
    assertEquals(true, third.get("ok"));
  }
}
