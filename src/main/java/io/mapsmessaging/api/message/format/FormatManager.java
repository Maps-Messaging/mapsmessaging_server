package io.mapsmessaging.api.message.format;

import io.mapsmessaging.utilities.service.Service;
import io.mapsmessaging.utilities.service.ServiceManager;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

public class FormatManager implements ServiceManager {
  private static final FormatManager instance = new FormatManager();

  public static FormatManager getInstance(){
    return instance;
  }

  private final ServiceLoader<Format> formatServiceLoader;

  public Format getFormat(String name){
    for(Format format:formatServiceLoader){
      if(format.getName().equalsIgnoreCase(name)){
        return format;
      }
    }
    return new RawFormat();
  }

  @Override
  public Iterator<Service> getServices() {
    List<Service> service = new ArrayList<>();
    for(Format parser:formatServiceLoader){
      service.add(parser);
    }
    return service.listIterator();
  }

  private FormatManager(){
    formatServiceLoader = ServiceLoader.load(Format.class);
  }

}
