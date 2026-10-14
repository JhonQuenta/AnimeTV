package pe.animetv;
import java.text.Normalizer;import java.util.Locale;
final class SourceText {
    private SourceText(){}
    static String normalized(String value){return Normalizer.normalize(value,Normalizer.Form.NFKD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT);}
}
