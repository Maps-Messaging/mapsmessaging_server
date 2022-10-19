package io.mapsmessaging.rest.impl;

import static spark.Spark.get;

import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.rest.RestApi;
import io.mapsmessaging.rest.transformation.JsonTransformation;
import io.mapsmessaging.schemas.config.SchemaConfig;
import java.util.List;

public class SchemaQueryApi implements RestApi {

  public void initialise() {
    get("/schema/getAll", (req, res) -> getAll(), new JsonTransformation());
  }

  private List<SchemaConfig> getAll() {
    return SchemaManager.getInstance().getAll();
  }
}
