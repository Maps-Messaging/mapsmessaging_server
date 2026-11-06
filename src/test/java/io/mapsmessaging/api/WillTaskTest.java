package io.mapsmessaging.api;

import io.mapsmessaging.api.features.QualityOfService;
import io.mapsmessaging.engine.session.will.WillTaskImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class WillTaskTest {

  @Test
  void publicApiDelegatesMutableWillProperties() {
    WillTaskImpl impl = mock(WillTaskImpl.class);
    WillTask task = new WillTask(impl);
    byte[] payload = new byte[]{1, 2, 3};

    task.updateMessage(payload);
    task.updateQoS(QualityOfService.EXACTLY_ONCE);
    task.updateRetainFlag(true);
    task.updateTopic("/last/will");
    task.cancel();

    verify(impl).updateMessage(payload);
    verify(impl).updateQoS(QualityOfService.EXACTLY_ONCE);
    verify(impl).updateRetain(true);
    verify(impl).updateTopic("/last/will");
    verify(impl).cancel();
  }

  @Test
  void nonNullApiContractsRejectNullBeforeDelegation() {
    WillTaskImpl impl = mock(WillTaskImpl.class);
    WillTask task = new WillTask(impl);

    assertThrows(NullPointerException.class, () -> task.updateMessage(null));
    assertThrows(NullPointerException.class, () -> task.updateQoS(null));
    assertThrows(NullPointerException.class, () -> task.updateTopic(null));

    verifyNoInteractions(impl);
  }
}
