package io.mapsmessaging.rest.transformation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalDateTime;
import spark.ResponseTransformer;

public class JsonTransformation implements ResponseTransformer {

    private final Gson gson;

    public JsonTransformation(){
      gson = new GsonBuilder()
          .setPrettyPrinting()
          .registerTypeAdapter(LocalDateTime.class, new DateTimeTransformation())
          .create();
    }

    @Override
    public String render(Object model) {
      return gson.toJson(model);
    }
}
