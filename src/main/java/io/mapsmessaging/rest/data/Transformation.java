package io.mapsmessaging.rest.data;

import com.google.gson.Gson;
import spark.ResponseTransformer;

public class Transformation implements ResponseTransformer {

    private Gson gson = new Gson();

    @Override
    public String render(Object model) {
      return gson.toJson(model);
    }
}
