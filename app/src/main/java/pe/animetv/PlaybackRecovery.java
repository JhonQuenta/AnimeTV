package pe.animetv;

final class PlaybackRecovery {
    private PlaybackRecovery() {}
    static boolean reuse(long resolvedAt,long now) { return now>=resolvedAt && now-resolvedAt<60000; }
    static boolean refreshFor(int code) { return code==1003 || (code>=2000 && code<3000); }
}
