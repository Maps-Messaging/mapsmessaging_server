package io.mapsmessaging.rest.api.impl;


public class SchemaQueryApi {

  public void initialise() {
    //  get("/schema/getAll", this::getAll, new JsonTransformation());
    //  get("/schema/get/:id", this::getId, new JsonTransformation());
  }
/*
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

 */
}
