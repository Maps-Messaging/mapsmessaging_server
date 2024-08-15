/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.mapsmessaging.ml;

import io.mapsmessaging.selector.operators.functions.ml.ModelStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.mlflow.api.proto.Service.*;
import org.mlflow.tracking.MlflowClient;

public class MLFlowModelStore implements ModelStore {

  private final MlflowClient mlflowClient;
  private final String experimentId;

  public MLFlowModelStore(MlflowClient mlflowClient, String experimentId) {
    this.mlflowClient = mlflowClient;
    this.experimentId = experimentId;
  }

  @Override
  public void saveModel(String s, byte[] bytes) throws Exception {
    String runId = createRun(experimentId, s);
    Path tempFile = Files.createTempFile(s, ".model");
    Files.write(tempFile, bytes);
    mlflowClient.logArtifact(runId, tempFile.toFile());
    Files.delete(tempFile);
    mlflowClient.setTag(runId, "modelName", s);
  }

  @Override
  public byte[] loadModel(String s) throws Exception {
    String runId = getRunIdByModelName(s);
    if (runId == null) {
      throw new Exception("Model not found");
    }
    Path tempDir = Files.createTempDirectory("mlflow-model");
    mlflowClient.downloadArtifacts(runId, "/"+ tempDir.toString());
    Path modelPath = tempDir.resolve(s + ".model");
    byte[] modelData = Files.readAllBytes(modelPath);
    Files.delete(modelPath);
    Files.delete(tempDir);
    return modelData;
  }

  @Override
  public boolean modelExists(String s) throws Exception {
    return getRunIdByModelName(s) != null;
  }

  private String createRun(String experimentId, String modelName) {
    return  mlflowClient.createRun(experimentId).getRunUuid();
  }

  private String getRunIdByModelName(String modelName) {
    for (RunInfo runInfo : mlflowClient.listRunInfos(experimentId)) {
      Run run = mlflowClient.getRun(runInfo.getRunUuid());
      List<RunTag> tags = run.getData().getTagsList();
      for (RunTag tag : tags) {
        if ("modelName".equals(tag.getKey()) && modelName.equals(tag.getValue())) {
          return runInfo.getRunUuid();
        }
      }
    }
    return null;
  }
}

