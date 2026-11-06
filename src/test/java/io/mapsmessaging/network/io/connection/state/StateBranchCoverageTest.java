package io.mapsmessaging.network.io.connection.state;

import io.mapsmessaging.network.io.connection.EndPointConnection;
import io.mapsmessaging.network.route.link.LinkState;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class StateBranchCoverageTest {

  @Test
  void constructSchemaAddsSchemaNamespaceWithOrWithoutLeadingSlash() {
    TestState state = new TestState(mock(EndPointConnection.class));

    assertEquals("$schema/topic", state.schema("topic"));
    assertEquals("$schema/topic", state.schema("/topic"));
  }

  @Test
  void runDelegatesToExecuteAndDefaultCancelIsNoOp() {
    TestState state = new TestState(mock(EndPointConnection.class));

    assertFalse(state.executed);
    state.run();
    assertTrue(state.executed);
    assertDoesNotThrow(state::cancel);
  }

  @Test
  void newlyCreatedStateHasNotTimedOut() {
    assertFalse(new TestState(mock(EndPointConnection.class)).hasTimedOut());
  }

  private static final class TestState extends State {
    boolean executed;

    TestState(EndPointConnection connection) {
      super(connection);
    }

    String schema(String namespace) {
      return constructSchema(namespace);
    }

    @Override
    public void execute() {
      executed = true;
    }

    @Override
    public String getName() {
      return "Test";
    }

    @Override
    public LinkState getLinkState() {
      return LinkState.CONNECTED;
    }
  }
}