/*
 *
 *   Copyright [ 2020 - 2021 ] [Matthew Buckton]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package io.mapsmessaging.utilities.threads.tasks;

import java.util.concurrent.FutureTask;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;

/**
 * Simple interface for a task queue that offers a priority level for each task queued
 *
 * @param <V> - is what will be returned by the future on completion of the task
 *
 *  @since 1.0
 *  @author Matthew Buckton
 *  @version 1.0
 */
public interface PriorityTaskScheduler<V> extends TaskScheduler<V> {

  void addTask(@NonNull @NotNull  FutureTask<V> task, int priority);

}
