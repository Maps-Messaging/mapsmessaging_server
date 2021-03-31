package io.mapsmessaging.network.protocol.impl.nmea.sentences;

import io.mapsmessaging.network.protocol.impl.nmea.types.Type;
import java.util.List;
import java.util.Map;

public class Sentence {

  private final String name;
  private final Map<String, Type> values;
  private final List<String> order;
  private final String format;

  public Sentence(String name, List<String> order, Map<String, Type> values, String format){
    this.name = name;
    this.values = values;
    this.format = format;
    this.order = order;
  }


  public String toString(){
    if(format.equalsIgnoreCase("json")){
      return "not implemented yet";
    }
    else{
      StringBuilder sb = new StringBuilder();
      sb.append("$").append(name).append(",");
      for(int x=0;x<order.size();x++){
        sb.append(values.get(order.get(x)));
        if( x != order.size() -1){
          sb.append(",");
        }
      }
      return sb.toString();
    }
  }

}
