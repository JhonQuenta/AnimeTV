package pe.animetv;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaybackRecoveryTest {
    @Test public void shortAbsenceReusesLinkButLongAbsenceResolvesAgain() {
        assertTrue(PlaybackRecovery.reuse(1000,59999));
        assertFalse(PlaybackRecovery.reuse(1000,61000));
        assertFalse(PlaybackRecovery.reuse(1000,999));
    }
    @Test public void RefreshesNetworkAndTimeoutButNotCodecOrMalformedFile() {
        assertTrue(PlaybackRecovery.refreshFor(2004));
        assertTrue(PlaybackRecovery.refreshFor(2001));
        assertTrue(PlaybackRecovery.refreshFor(1003));
        assertFalse(PlaybackRecovery.refreshFor(4001));
        assertFalse(PlaybackRecovery.refreshFor(3001));
    }
}
