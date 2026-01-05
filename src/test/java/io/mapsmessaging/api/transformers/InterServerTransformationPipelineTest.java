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

package io.mapsmessaging.api.transformers;

import io.mapsmessaging.api.message.Message;
import io.mapsmessaging.network.protocol.Protocol;
import org.junit.jupiter.api.Test;

import java.util.List;

import static io.mapsmessaging.api.transformers.TransformationTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

public class InterServerTransformationPipelineTest {

  private static Protocol.ParsedMessage applyPipeline(
      String source,
      Protocol.ParsedMessage message,
      List<InterServerTransformation> transformers
  ) {
    Protocol.ParsedMessage current = message;
    for (InterServerTransformation transformer : transformers) {
      if (current == null) {
        return null;
      }
      current = transformer.transform(source, current);
    }
    return current;
  }

  @Test
  void pipeline_stopsWhenTransformerDrops() {
    InterServerTransformation pass = mock(InterServerTransformation.class);
    InterServerTransformation drop = mock(InterServerTransformation.class);
    InterServerTransformation neverCalled = mock(InterServerTransformation.class);

    Message message = mockMessage(utf8Bytes("{\"a\":1}"));
    Protocol.ParsedMessage parsedMessage = parsedMessage("/dst", message);

    when(pass.transform(anyString(), any())).thenAnswer(inv -> inv.getArgument(1));
    when(drop.transform(anyString(), any())).thenReturn(null);

    Protocol.ParsedMessage result = applyPipeline(
        "/src",
        parsedMessage,
        List.of(pass, drop, neverCalled)
    );

    assertNull(result);
    verify(pass, times(1)).transform(anyString(), any());
    verify(drop, times(1)).transform(anyString(), any());
    verifyNoInteractions(neverCalled);
  }
}
