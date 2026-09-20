package io.mapsmessaging.config;

import io.mapsmessaging.security.access.monitor.AuthenticationMonitorConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthManagerConfigBranchCoverageTest {

  @Test
  void monitorConfigReflectsAuthenticationLockSettings() {
    AuthManagerConfig config = new AuthManagerConfig();
    config.setMaxFailuresBeforeLock(7);
    config.setInitialLockSeconds(11);
    config.setMaxLockSeconds(101);
    config.setFailureDecaySeconds(202);
    config.setEnableSoftDelay(false);
    config.setSoftDelayMillisPerFailure(303);
    config.setMaxSoftDelayMillis(404);

    AuthenticationMonitorConfig monitor = config.buildMonitorConfig();

    assertEquals(7, monitor.getMaxFailuresBeforeLock());
    assertEquals(11, monitor.getInitialLockSeconds());
    assertEquals(101, monitor.getMaxLockSeconds());
    assertEquals(202, monitor.getFailureDecaySeconds());
    assertFalse(monitor.isEnableSoftDelay());
    assertEquals(303, monitor.getSoftDelayMillisPerFailure());
    assertEquals(404, monitor.getMaxSoftDelayMillis());
  }

  @Test
  void updateAppliesPasswordAndAuthenticationPolicyChanges() {
    AuthManagerConfig current = new AuthManagerConfig();
    AuthManagerConfig changed = new AuthManagerConfig();
    changed.setAuthenticationEnabled(!current.isAuthenticationEnabled());
    changed.setAuthorisationEnabled(!current.isAuthorisationEnabled());
    changed.setAllowAnonymous(!current.isAllowAnonymous());
    changed.setMinimumPasswordLength(current.getMinimumPasswordLength() + 1);
    changed.setMaximumPasswordLength(current.getMaximumPasswordLength() + 1);
    changed.setMinimumLowercase(current.getMinimumLowercase() + 1);
    changed.setMinimumUppercase(current.getMinimumUppercase() + 1);
    changed.setMinimumDigits(current.getMinimumDigits() + 1);
    changed.setMinimumSpecial(current.getMinimumSpecial() + 1);
    changed.setAllowedSpecialCharacters("!@");
    changed.setRejectWhitespace(!current.isRejectWhitespace());
    changed.setRejectContainsUsername(!current.isRejectContainsUsername());
    changed.setMaximumConsecutiveIdenticalCharacters(
        current.getMaximumConsecutiveIdenticalCharacters() + 1);
    changed.setPasswordRegex(".*");
    changed.setPasswordHistoryCount(current.getPasswordHistoryCount() + 1);
    changed.setPasswordMaxAgeDays(current.getPasswordMaxAgeDays() + 1);
    changed.setMaxFailuresBeforeLock(current.getMaxFailuresBeforeLock() + 1);
    changed.setInitialLockSeconds(current.getInitialLockSeconds() + 1);
    changed.setMaxLockSeconds(current.getMaxLockSeconds() + 1);
    changed.setFailureDecaySeconds(current.getFailureDecaySeconds() + 1);
    changed.setEnableSoftDelay(!current.isEnableSoftDelay());
    changed.setSoftDelayMillisPerFailure(current.getSoftDelayMillisPerFailure() + 1);
    changed.setMaxSoftDelayMillis(current.getMaxSoftDelayMillis() + 1);

    assertTrue(current.update(changed));
    assertEquals(changed.getMinimumPasswordLength(), current.getMinimumPasswordLength());
    assertEquals(changed.getMaxFailuresBeforeLock(), current.getMaxFailuresBeforeLock());
    assertFalse(current.update(changed));
  }
}