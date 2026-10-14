package pe.animetv;
import java.time.*;import java.time.format.*;import java.util.*;import java.util.regex.*;
/** Format the stored UTC timestamp at display time, including history saved by older builds. */
final class PublicationTime {
    private static final Pattern ISO=Pattern.compile("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:\\.\\d{1,9})?(?:Z|[+-]\\d{2}:\\d{2})");
    static String display(String value){return display(value,ZoneId.systemDefault());}
    static String display(String value,ZoneId zone){
        Matcher match=ISO.matcher(value);StringBuffer out=new StringBuffer();while(match.find()){
            try{String date=OffsetDateTime.parse(match.group()).atZoneSameInstant(zone).format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm",Locale.forLanguageTag("es")));match.appendReplacement(out,Matcher.quoteReplacement(date));}
            catch(DateTimeException invalid){match.appendReplacement(out,Matcher.quoteReplacement(match.group()));}
        }match.appendTail(out);return out.toString();
    }
}
