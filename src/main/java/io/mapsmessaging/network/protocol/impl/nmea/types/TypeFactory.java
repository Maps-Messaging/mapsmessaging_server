package io.mapsmessaging.network.protocol.impl.nmea.types;

import java.util.Iterator;

public class TypeFactory {

  public static Type create(String parameterName, String typeName, String param, Iterator<String> iterator){
    switch(typeName){
      case "Position":
        return new PositionType(iterator.next(), iterator.next());

      case "UTCTime":
        return new UTCTimeType(iterator.next());

      case "Height":
        return new HeightType(iterator.next(), iterator.next());

      case "String":
      case "char":
        return new StringType(iterator.next());

      case "long":
        return new LongType(iterator.next());

      case "double":
        return new DoubleType(iterator.next());

      case "boolean":
        return new BooleanType(iterator.next(), param);

      case "Date":
        return new DateType(iterator.next());

      case "Enum":
        return EnumTypeFactory.getInstance().getEnum(parameterName, iterator.next());

      default:
        return null;
    }
  }

  private TypeFactory(){}
}
