package pe.animetv;
/** Stable default ordering shared by adapters; playback decisions remain in the app. */
public final class SourceServers {
    private SourceServers(){}
    public static int rank(String name){return switch(name.toLowerCase(java.util.Locale.ROOT)){case "voe"->0;case "byse","yourupload"->1;case "mp4upload"->2;case "mega"->4;default->3;};}
}
