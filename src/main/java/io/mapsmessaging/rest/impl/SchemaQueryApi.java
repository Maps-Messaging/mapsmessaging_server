package io.mapsmessaging.rest.impl;

import static spark.Spark.get;

import io.mapsmessaging.engine.schema.SchemaManager;
import io.mapsmessaging.rest.RestApi;
import io.mapsmessaging.rest.data.SchemaData;
import io.mapsmessaging.rest.transformation.JsonTransformation;
import io.mapsmessaging.schemas.config.SchemaConfig;
import java.util.ArrayList;
import java.util.List;
import spark.Request;
import spark.Response;

public class SchemaQueryApi implements RestApi {

  public void initialise() {
    get("/schema/getAll", this::getAll, new JsonTransformation());
    get("/schema/get/:id", this::getId, new JsonTransformation());
  }

  protected SchemaData getId(Request req, Response res) {
    res.type("application/json");
    String id =  req.params(":id");
    for(SchemaConfig config:SchemaManager.getInstance().getAll()){
      if(config.getUniqueId().equals(id)){
        return new SchemaData(config);
      }
    }
    return null;
  }

  protected List<SchemaData> getAll(Request req, Response res) {
    res.type("application/json");
    List<SchemaData> list = new ArrayList<>();
    for(SchemaConfig config:SchemaManager.getInstance().getAll()){
      list.add(new SchemaData(config));
    }
    return list;
  }
}
