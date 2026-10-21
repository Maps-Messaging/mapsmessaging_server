package io.mapsmessaging.api.transformers;

import io.mapsmessaging.api.message.Message;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InterServerPipelineTransformationTest {

  @Test
  void pipelinePassesUpdatedDestinationToNextStage() {
    InterServerTransformation first = mock(InterServerTransformation.class);
    InterServerTransformation second = mock(InterServerTransformation.class);
    Message message = mock(Message.class);

    ParsedMessage input = new ParsedMessage("/input", message);
    ParsedMessage afterFirst = new ParsedMessage("/stage-one", message);
    ParsedMessage afterSecond = new ParsedMessage("/stage-two", message);

    when(first.transform("source", input)).thenReturn(afterFirst);
    when(second.transform("/stage-one", afterFirst)).thenReturn(afterSecond);

    ParsedMessage result =
        new InterServerPipelineTransformation(List.of(first, second))
            .transform("source", input);

    assertSame(afterSecond, result);
    verify(first).transform("source", input);
    verify(second).transform("/stage-one", afterFirst);
  }

  @Test
  void nullFromStageShortCircuitsRemainingPipeline() {
    InterServerTransformation first = mock(InterServerTransformation.class);
    InterServerTransformation second = mock(InterServerTransformation.class);
    ParsedMessage input = new ParsedMessage("/input", mock(Message.class));

    when(first.transform("source", input)).thenReturn(null);

    ParsedMessage result =
        new InterServerPipelineTransformation(List.of(first, second))
            .transform("source", input);

    assertNull(result);
    verify(first).transform("source", input);
    verifyNoInteractions(second);
  }

  @Test
  void nullInputAndEmptyPipelineAreHandledWithoutInventingMessages() {
    InterServerTransformation stage = mock(InterServerTransformation.class);
    InterServerPipelineTransformation pipeline =
        new InterServerPipelineTransformation(List.of(stage));

    assertNull(pipeline.transform("source", null));
    verifyNoInteractions(stage);

    ParsedMessage input = new ParsedMessage("/input", mock(Message.class));
    assertSame(
        input,
        new InterServerPipelineTransformation(List.of()).transform("source", input)
    );
  }

  @Test
  void serviceMetadataIdentifiesInternalPipeline() {
    InterServerPipelineTransformation pipeline =
        new InterServerPipelineTransformation(List.of());

    assertEquals("Pipeline", pipeline.getName());
    assertEquals("Internal pipeline transformer", pipeline.getDescription());
    assertNull(pipeline.build(null));
  }
}
