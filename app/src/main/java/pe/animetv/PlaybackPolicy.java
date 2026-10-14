package pe.animetv;

public final class PlaybackPolicy {
    private PlaybackPolicy() {}
    public static int serverRank(String name) {
        return SourceServers.rank(name);
    }
    public static long seek(long position, long delta, long duration) {
        long result = Math.max(0, position + delta);
        return duration > 0 ? Math.min(result, duration) : result;
    }
    public static boolean reachedEnd(long position, long duration) {
        return duration > 0 && position >= duration;
    }
}
