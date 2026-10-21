package io.mapsmessaging.network.protocol.impl.satellite.protocol;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SatelliteMessageRebuilderBranchCoverageTest {

  @Test
  void nullZeroTotalNegativeAndOutOfRangeFragmentsAreRejected() {
    SatelliteMessageRebuilder rebuilder = new SatelliteMessageRebuilder();

    assertNull(rebuilder.rebuild(null));
    assertNull(rebuilder.rebuild(new SatelliteMessage(1, new byte[]{1}, 0, 0, false, (byte) 0)));
    assertNull(rebuilder.rebuild(new SatelliteMessage(1, new byte[]{1}, -1, 2, false, (byte) 0)));
    assertNull(rebuilder.rebuild(new SatelliteMessage(1, new byte[]{1}, 2, 2, false, (byte) 0)));
  }

  @Test
  void singleFragmentIsReturnedImmediately() {
    SatelliteMessageRebuilder rebuilder = new SatelliteMessageRebuilder();
    SatelliteMessage fragment =
        new SatelliteMessage(1, new byte[]{1, 2}, 0, 1, false, (byte) 0);

    assertSame(fragment, rebuilder.rebuild(fragment));
  }

  @Test
  void mismatchedFragmentCountDropsCorruptStreamAndAllowsRestart() {
    SatelliteMessageRebuilder rebuilder = new SatelliteMessageRebuilder();

    assertNull(rebuilder.rebuild(new SatelliteMessage(7, new byte[]{1}, 0, 2, false, (byte) 0)));
    assertNull(rebuilder.rebuild(new SatelliteMessage(7, new byte[]{2}, 1, 3, false, (byte) 0)));

    assertNull(rebuilder.rebuild(new SatelliteMessage(7, new byte[]{1}, 0, 2, false, (byte) 0)));
    SatelliteMessage complete =
        rebuilder.rebuild(new SatelliteMessage(7, new byte[]{2}, 1, 2, false, (byte) 0));

    assertNotNull(complete);
    assertArrayEquals(new byte[]{1, 2}, complete.getMessage());
  }

  @Test
  void duplicateFragmentsDoNotPrematurelyCompleteStream() {
    SatelliteMessageRebuilder rebuilder = new SatelliteMessageRebuilder();
    SatelliteMessage first =
        new SatelliteMessage(3, new byte[]{1}, 0, 2, false, (byte) 0);

    assertNull(rebuilder.rebuild(first));
    assertNull(rebuilder.rebuild(first));
    assertNotNull(rebuilder.rebuild(new SatelliteMessage(3, new byte[]{2}, 1, 2, false, (byte) 0)));
  }
}