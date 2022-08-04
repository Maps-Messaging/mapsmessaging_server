package io.mapsmessaging.network.protocol.impl.coap.packet.options;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Getter;
import lombok.ToString;


/*
  | No. | C | U | N | R | Name           | Format | Length | Default  |
   +-----+---+---+---+---+----------------+--------+--------+----------+
   |   1 | x |   |   | x | If-Match       | opaque | 0-8    | (none)   |
   |   3 | x | x | - |   | Uri-Host       | string | 1-255  | (see     |
   |     |   |   |   |   |                |        |        | below)   |
   |   4 |   |   |   | x | ETag           | opaque | 1-8    | (none)   |
   |   5 | x |   |   |   | If-None-Match  | empty  | 0      | (none)   |
   |   7 | x | x | - |   | Uri-Port       | uint   | 0-2    | (see     |
   |     |   |   |   |   |                |        |        | below)   |
   |   8 |   |   |   | x | Location-Path  | string | 0-255  | (none)   |
   |  11 | x | x | - | x | Uri-Path       | string | 0-255  | (none)   |
   |  12 |   |   |   |   | Content-Format | uint   | 0-2    | (none)   |
   |  14 |   | x | - |   | Max-Age        | uint   | 0-4    | 60       |
   |  15 | x | x | - | x | Uri-Query      | string | 0-255  | (none)   |
   |  17 | x |   |   |   | Accept         | uint   | 0-2    | (none)   |
   |  20 |   |   |   | x | Location-Query | string | 0-255  | (none)   |
   |  35 | x | x | - |   | Proxy-Uri      | string | 1-1034 | (none)   |
   |  39 | x | x | - |   | Proxy-Scheme   | string | 1-255  | (none)   |
   |  60 |   |   | x |   | Size1          | uint   | 0-4    | (none)   |
   +-----+---+---+---+---+----------------+--------+--------+----------+
 */
@ToString
public class OptionSet {

  @Getter
  Map<Integer, Option> optionList;

  public OptionSet(){
    optionList = new LinkedHashMap<>();
  }

  public void add(Option option) {
    optionList.put(option.getId(), option);
  }

  public boolean hasOption(int id){
    return optionList.containsKey(id);
  }

  public Option getOption(int id){
    return optionList.computeIfAbsent(id, this::createOption);
  }

  public void putOption(Option option){
    optionList.put(option.getId(), option);
  }

  private Option createOption(int id) {
    switch (id) {
      case Constants.IF_MATCH:
        return new IfMatch();

      case Constants.IF_NONE_MATCH:
        return new IfNoneMatch();

      case Constants.ACCEPT:
        return new Accept();

      case Constants.CONTENT_FORMAT:
        return new ContentFormat();

      case Constants.ETAG:
        return new ETag();

      case Constants.LOCATION_PATH:
        return new LocationPath();

      case Constants.LOCATION_QUERY:
        return new LocationQuery();

      case Constants.MAX_AGE:
        return new MaxAge();

      case Constants.PROXY_URI:
        return new ProxyUri();

      case Constants.PROXY_SCHEME:
        return new ProxyScheme();

      case Constants.OBSERVE:
        return new Observe(1);

      case Constants.SIZE1:
        return new Size1();

      case Constants.URI_HOST:
        return new UriHost();

      case Constants.URI_PATH:
        return new UriPath();

      case Constants.URI_PORT:
        return new UriPort();

      case Constants.URI_QUERY:
        return new UriQuery();

      case Constants.BLOCK1:
        return new Block(id);

      case Constants.BLOCK2:
        return new Block(id);

      default:
        return new GenericOption(id);
    }
  }
}
