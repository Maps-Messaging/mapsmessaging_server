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

package io.mapsmessaging.tools.config.lint;

import io.mapsmessaging.config.ConfigManager;
import io.mapsmessaging.dto.rest.config.BaseConfigDTO;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class ConfigLintRunner {

  private final Path jsonReportPath;
  private final Path textReportPath;

  public ConfigLintRunner(Path jsonReportPath, Path textReportPath) {
    this.jsonReportPath = jsonReportPath;
    this.textReportPath = textReportPath;
  }

  public ConfigLintReport run() throws Exception {
    boolean strict = Boolean.parseBoolean(
        System.getProperty("maps.configlint.strict", "false")
    );

    boolean showInfo = Boolean.parseBoolean(
        System.getProperty("maps.configlint.showInfo", "false")
    );

    ServiceLoader<ConfigManager> loader = ServiceLoader.load(ConfigManager.class);

    List<ConfigLintConfigResult> configs = new ArrayList<>();
    List<LintIssue> allIssues = new ArrayList<>();

    for (ConfigManager manager : loader) {
      String configName = resolveConfigName(manager);
      Class<?> managerClass = manager.getClass();
      Class<? extends BaseConfigDTO> rootDtoClass = RootDtoResolver.resolveRootDto(managerClass);

      if (rootDtoClass == null) {
        LintIssue issue = LintIssue.error(
            configName,
            null,
            "",
            "ROOT_DTO_NOT_FOUND",
            "Could not resolve root DTO by walking superclasses to BaseConfigDTO from " + managerClass.getName()
        );
        allIssues.add(issue);
        configs.add(new ConfigLintConfigResult(configName, null, List.of(issue)));
        continue;
      }

      LintEngine engine = new LintEngine(strict);
      DtoWalker walker = new DtoWalker(engine);

      List<LintIssue> issuesForConfig = walker.lint(configName, rootDtoClass);

      allIssues.addAll(issuesForConfig);
      configs.add(new ConfigLintConfigResult(configName, rootDtoClass.getName(), issuesForConfig));
    }

    List<ConfigLintConfigResult> filteredConfigs = new ArrayList<>();
    List<LintIssue> filteredAllIssues = new ArrayList<>();

    for (ConfigLintConfigResult config : configs) {
      List<LintIssue> filteredIssues =
          showInfo
              ? config.getIssues()
              : config.getIssues().stream()
              .filter(issue -> issue.getSeverity() != LintSeverity.INFO)
              .toList();

      if (!filteredIssues.isEmpty() || showInfo) {
        filteredConfigs.add(
            new ConfigLintConfigResult(
                config.getConfigName(),
                config.getRootDtoClass(),
                filteredIssues
            )
        );
      }

      filteredAllIssues.addAll(filteredIssues);
    }

    ConfigLintSummary summary = ConfigLintSummary.from(filteredAllIssues);

    ConfigLintReport report = new ConfigLintReport(
        OffsetDateTime.now().toString(),
        filteredConfigs,
        summary
    );

    Files.createDirectories(jsonReportPath.getParent());
    JsonReportWriter.write(jsonReportPath, report);
    TextReportWriter.write(textReportPath, report);

    return report;
  }

  private String resolveConfigName(ConfigManager manager) {
    try {
      Method method = manager.getClass().getMethod("getName");
      Object value = method.invoke(manager);
      if (value != null) {
        String text = value.toString().trim();
        if (!text.isEmpty()) {
          return text;
        }
      }
    }
    catch (Exception ignored) {
      // not available, fall back
    }
    return manager.getClass().getName();
  }
}
