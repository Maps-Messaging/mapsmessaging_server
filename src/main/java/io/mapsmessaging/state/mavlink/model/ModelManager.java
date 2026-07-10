/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.state.mavlink.model;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ModelManager {

    private static final ModelManager INSTANCE = new ModelManager();

    private final Map<String, UxvModel> models;

    private ModelManager() {
        models = new ConcurrentHashMap<>();
        loadModels();
    }

    public static ModelManager getInstance() {
        return INSTANCE;
    }

    public Optional<UxvModel> getModel(String modelName) {
        Objects.requireNonNull(modelName, "modelName must not be null");
        return Optional.ofNullable(models.get(modelName));
    }

    public UxvModel getRequiredModel(String modelName) {
        return getModel(modelName)
            .orElse(null);
    }

    public Collection<UxvModel> getModels() {
        return Collections.unmodifiableCollection(models.values());
    }

    public Collection<String> getModelNames() {
        return Collections.unmodifiableSet(models.keySet());
    }

    private void loadModels() {
        ServiceLoader<UxvModel> modelService = ServiceLoader.load(UxvModel.class);

        for (UxvModel model : modelService) {
            registerModel(model);
        }
    }

    private void registerModel(UxvModel model) {
        Objects.requireNonNull(model, "model must not be null");

        String modelName = model.getModelName();
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalStateException("UxV model " + model.getClass().getName() + " returned a blank model name");
        }

        UxvModel existing = models.putIfAbsent(modelName, model);
        if (existing != null) {
            throw new IllegalStateException("Duplicate UxV model name '" + modelName + "' used by " + existing.getClass().getName() + " and " + model.getClass().getName());
        }
    }
}