package pe.animetv;
import java.util.Locale;
/** Audio identifiers shared by adapters; playback and user preferences stay in the app. */
public final class SourceAudio {
    private SourceAudio(){}
    public static boolean matches(String audio,String preference){return audio.equals(preference)||preference.equals("DUB")&&(audio.equals("LAT")||audio.equals("CAST"))||(preference.equals("LAT")||preference.equals("CAST"))&&audio.equals("DUB");}
    public static String label(String audio){return switch(audio){case "LAT"->"Latino";case "CAST"->"Castellano";case "DUB"->"Doblado";default->"Subtitulado";};}
    public static String fromTitle(String title){String value=title.toLowerCase(Locale.ROOT);return value.contains("castellano")?"CAST":value.contains("latino")?"LAT":"SUB";}
}
