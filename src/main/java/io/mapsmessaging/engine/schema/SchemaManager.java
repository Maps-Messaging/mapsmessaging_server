package io.mapsmessaging.engine.schema;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.repository.SchemaRepository;
import io.mapsmessaging.schemas.repository.impl.SimpleSchemaRepository;
import java.util.List;
import java.util.UUID;

public class SchemaManager implements SchemaRepository{

  public static final UUID DEFAULT_RAW_UUID = UUID.fromString("10000000-0000-1000-a000-100000000000");
  private static final SchemaManager instance;
  public static SchemaManager getInstance(){
    return instance;
  }
  static{
    instance = new SchemaManager();
  }

  private final SchemaRepository repository;

  @Override
  public synchronized SchemaConfig addSchema(String s, SchemaConfig schemaConfig) {
    return repository.addSchema(s, schemaConfig);
  }

  @Override
  public synchronized SchemaConfig getSchema(UUID uuid) {
    return repository.getSchema(uuid);
  }

  @Override
  public synchronized List<SchemaConfig> getSchema(String s) {
    return repository.getSchema(s);
  }

  @Override
  public synchronized List<SchemaConfig> getSchemas(String s) {
    return repository.getSchemas(s);
  }

  @Override
  public synchronized List<SchemaConfig> getAll() {
    return repository.getAll();
  }

  @Override
  public synchronized void removeSchema(UUID uuid) {
    repository.removeSchema(uuid);
  }

  @Override
  public synchronized void removeAllSchemas() {
    repository.removeAllSchemas();
  }

  private SchemaManager(){
    repository = new SimpleSchemaRepository();
  }

}
