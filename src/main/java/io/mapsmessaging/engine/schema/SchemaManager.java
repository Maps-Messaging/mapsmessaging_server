package io.mapsmessaging.engine.schema;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig.TYPE;
import io.mapsmessaging.schemas.config.impl.RawSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import io.mapsmessaging.schemas.repository.SchemaRepository;
import io.mapsmessaging.schemas.repository.impl.SimpleSchemaRepository;
import java.util.List;
import java.util.UUID;

public class SchemaManager implements SchemaRepository{

  public static final UUID DEFAULT_RAW_UUID = UUID.fromString("10000000-0000-1000-a000-100000000000");
  public static final UUID DEFAULT_NUMERIC_STRING_SCHEMA = UUID.fromString("10000000-0000-1000-a000-100000000001");
  public static final UUID DEFAULT_STRING_SCHEMA = UUID.fromString("10000000-0000-1000-a000-100000000002");

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

  public void start(){
    SchemaConfig rawConfig = new RawSchemaConfig();
    rawConfig.setUniqueId(DEFAULT_RAW_UUID);
    addSchema("", rawConfig);

    NativeSchemaConfig nativeSchemaConfig = new NativeSchemaConfig();
    nativeSchemaConfig.setUniqueId(DEFAULT_NUMERIC_STRING_SCHEMA);
    nativeSchemaConfig.setType(TYPE.NUMERIC_STRING);
    addSchema("$SYS", nativeSchemaConfig);

    nativeSchemaConfig = new NativeSchemaConfig();
    nativeSchemaConfig.setUniqueId(DEFAULT_STRING_SCHEMA);
    nativeSchemaConfig.setType(TYPE.STRING);
    addSchema("$SYS", nativeSchemaConfig);


    MessageFormatterFactory factory = MessageFormatterFactory.getInstance();

  }

  private SchemaManager(){
    repository = new SimpleSchemaRepository();
  }

}
