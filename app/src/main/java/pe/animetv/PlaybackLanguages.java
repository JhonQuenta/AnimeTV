package pe.animetv;
/** Playback-facing wrapper; identifiers are shared with sources, preferences stay in Store. */
final class PlaybackLanguages {
    private PlaybackLanguages(){}
    static boolean matches(String audio,String preference){return SourceAudio.matches(audio,preference);}
    static String label(String audio){return SourceAudio.label(audio);}
    static String fromTitle(String title){return SourceAudio.fromTitle(title);}
}
