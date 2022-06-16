package io.mapsmessaging.api.message.format.walker;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class StructureWalker {


  public static Object locateObject(Resolver resolver, List<String> searchPath){
    Object context = null;
    while(!searchPath.isEmpty()){
      var path = searchPath.remove(0);
      context = resolver.get(path);
      if(context instanceof  Resolver){
        resolver= (Resolver) context;
      }
    }
    return parse(context);
  }

  private static Object parse(Object lookup){
    if (lookup instanceof String ||
        lookup instanceof Float ||
        lookup instanceof Double ||
        lookup instanceof Byte ||
        lookup instanceof Character ||
        lookup instanceof Short ||
        lookup instanceof Integer ||
        lookup instanceof Long) {
      return lookup;
    }
    else if(lookup instanceof BigDecimal){
      return ((BigDecimal)lookup).doubleValue();
    }
    return lookup.toString();
  }
}
