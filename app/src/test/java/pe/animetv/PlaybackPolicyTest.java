package pe.animetv;
import org.junit.Test;
import static org.junit.Assert.*;
public class PlaybackPolicyTest {
    @Test public void voePrecedesByse(){assertEquals(0,PlaybackPolicy.serverRank("Voe"));assertEquals(1,PlaybackPolicy.serverRank("Byse"));assertEquals(2,PlaybackPolicy.serverRank("MP4Upload"));}
    @Test public void rewindNeverGoesNegative() { assertEquals(0,PlaybackPolicy.seek(2000,-5000,100000)); }
    @Test public void skipStopsAtEnd() { assertEquals(100000,PlaybackPolicy.seek(90000,85000,100000)); }
    @Test public void unknownDurationDoesNotSuggestNext() { assertFalse(PlaybackPolicy.reachedEnd(1000,-1)); }
    @Test public void postCreditsRemainPlayable() { assertEquals(1185000,PlaybackPolicy.seek(1100000,85000,1400000)); assertFalse(PlaybackPolicy.reachedEnd(1185000,1400000)); }
}

