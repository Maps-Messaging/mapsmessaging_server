package io.mapsmessaging.engine.schema;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This class is the in memory repo for the destination schemas.
 *
 * This provides a mechanism to query, update and delete schemas
 */
public class SchemaManager {

  private static final SchemaManager instance = new SchemaManager();
  public static SchemaManager getInstance(){
    return instance;
  }

  private final Map<String, Schema> schemaMap;

  public Schema get(String destinationName){
    return schemaMap.get(destinationName);
  }

  public void register(String destinationName, Schema schema){
    schemaMap.put(destinationName, schema);
    // Need to lookup the physical destination and rewrite the schema
  }

  public void delete(String destinationName){
    schemaMap.remove(destinationName);
    // Need to ensure the physical
  }

  public int size(){
    return schemaMap.size();
  }

  private SchemaManager(){
    schemaMap = new LinkedHashMap<>();
  }
}
