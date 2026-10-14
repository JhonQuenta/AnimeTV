package pe.animetv;

/** A ready audio track does not prove that video has rendered. */
public final class PlaybackHealth {
    private PlaybackHealth() {}
    public static boolean awaitingVideo(boolean firstFrame,boolean paused,boolean videoExpected) {
        return !firstFrame && !paused && videoExpected;
    }
    public static boolean canSeek(boolean timelineAvailable,boolean seekCommandAvailable) {
        return timelineAvailable && seekCommandAvailable;
    }
}
