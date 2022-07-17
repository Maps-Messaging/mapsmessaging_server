package io.mapsmessaging.engine.schema;

import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.operators.ParserExecutor;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class LinkFormatManager {

  private static final LinkFormatManager instance;

  public static LinkFormatManager getInstance() {
    return instance;
  }

  static {
    instance = new LinkFormatManager();
  }

  public String buildLinkFormatString(String filter, List<LinkFormat> linkFormatList) {
    ParserExecutor parser = build(filter);
    StringBuilder sb = new StringBuilder();
    AtomicBoolean first = new AtomicBoolean(true);
    linkFormatList.stream().filter(linkFormat -> select(linkFormat, parser)).forEach(linkFormat -> {
      if (!first.get()) {
        sb.append(",");
      }
      first.set(false);
      sb.append(linkFormat.pack());
    });
    return sb.toString();
  }

  private boolean select(LinkFormat linkFormat, ParserExecutor parser) {
    if (parser != null && !parser.evaluate(linkFormat)) {
      return false;
    }
    return !linkFormat.getPath().toLowerCase().startsWith("$sys") &&
        !linkFormat.getPath().equalsIgnoreCase(".well-known/core") &&
        linkFormat.getInterfaceDescription() != null;
  }

  private ParserExecutor build(String filter) {
    if (filter != null && filter.length() > 0) {
      try {
        return io.mapsmessaging.selector.SelectorParser.compile(filter);
      } catch (ParseException e) {
        // Ignore this
      }
    }
    return null;
  }

}
