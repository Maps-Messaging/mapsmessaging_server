package io.mapsmessaging.network.protocol.impl.nmea.types;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TypeFactoryTest {

  @Test
  void createsSupportedScalarTypes() {
    assertInstanceOf(StringType.class, TypeFactory.create("x", "String", "", List.of("abc").iterator()));
    assertInstanceOf(StringType.class, TypeFactory.create("x", "char", "", List.of("c").iterator()));
    assertInstanceOf(LongType.class, TypeFactory.create("x", "long", "", List.of("12").iterator()));
    assertInstanceOf(LongType.class, TypeFactory.create("x", "int", "", List.of("12").iterator()));
    assertInstanceOf(DoubleType.class, TypeFactory.create("x", "double", "", List.of("1.5").iterator()));
    assertInstanceOf(DoubleType.class, TypeFactory.create("x", "float", "", List.of("1.5").iterator()));
    assertInstanceOf(BooleanType.class, TypeFactory.create("x", "boolean", "Y", List.of("Y").iterator()));
    assertInstanceOf(DateType.class, TypeFactory.create("x", "Date", "", List.of("190926").iterator()));
    assertInstanceOf(UTCTimeType.class, TypeFactory.create("x", "UTCTime", "", List.of("235959").iterator()));
  }

  @Test
  void createsMultiTokenTypesInOrder() {
    PositionType position = assertInstanceOf(
        PositionType.class,
        TypeFactory.create("position", "Position", "", List.of("4916.45", "N").iterator())
    );
    HeightType height = assertInstanceOf(
        HeightType.class,
        TypeFactory.create("height", "Height", "", List.of("12.5", "m").iterator())
    );

    assertEquals(49.0 + 16.45 / 60.0, position.getPosition(), 0.000001);
    assertEquals(12.5, height.getHeight(), 0.0);
    assertEquals('M', height.getUnit());
  }

  @Test
  void enumUsesRegisteredParameterNameAndUnknownTypeReturnsNull() {
    String parameter = "factory-mode";
    EnumTypeFactory.getInstance().register(parameter, "[{\"1\":\"Active\"}]");

    EnumType value = assertInstanceOf(
        EnumType.class,
        TypeFactory.create(parameter, "Enum", "", List.of("1").iterator())
    );

    assertEquals("Active", value.getDescription());
    assertNull(TypeFactory.create("x", "not-a-type", "", List.of("unused").iterator()));
  }
}
