package pe.animetv;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaybackHealthTest {
    @Test public void audioReadyDoesNotProveVideoRendered() { assertTrue(PlaybackHealth.awaitingVideo(false,false,true)); }
    @Test public void pauseDoesNotTriggerServerReplacement() { assertFalse(PlaybackHealth.awaitingVideo(false,true,true)); }
    @Test public void successfulFrameEndsVideoWatchdog() { assertFalse(PlaybackHealth.awaitingVideo(true,false,true)); }
    @Test public void seekingDoesNotDependOnFirstFrame() { assertTrue(PlaybackHealth.canSeek(true,true)); }
    @Test public void unavailableTimelineDoesNotSeek() { assertFalse(PlaybackHealth.canSeek(false,true)); }
}
