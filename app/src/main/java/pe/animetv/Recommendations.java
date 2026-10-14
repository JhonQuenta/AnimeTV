package pe.animetv;

import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import static pe.animetv.Models.*;

/** Finds entries available in the current source; title matching is not canonical relation metadata. */
final class Recommendations {
    private final Source source;
    private final TimedCache<List<Anime>> cache=new TimedCache<>(12,600000);
    Recommendations(Source source) { this.source=source; }
    private static final ExecutorService FILLERS=Executors.newFixedThreadPool(2,r->{Thread t=new Thread(r,"recommendation-fill");t.setDaemon(true);return t;});
    List<Anime> load(Detail detail)throws Exception {return load(detail,ignored->{});}
    List<Anime> load(Detail detail,Consumer<List<Anime>> progress)throws Exception {
        String genre=detail.genres().isEmpty()?"":detail.genres().get(0).slug();
        return cache.get(detail.anime().url()+"|"+genre,()->fetch(detail,genre,progress));
    }
    private List<Anime> fetch(Detail detail,String genre,Consumer<List<Anime>> progress)throws Exception {
        Anime current=detail.anime();String root=family(current.title());
        LinkedHashMap<String,Anime> out=new LinkedHashMap<>();Exception failure=null;
        Future<List<Anime>> fill=FILLERS.submit(()->source.catalog("",1,genre.isBlank()?Map.of():Map.of("genre",genre)));
        try{
            try{for(Anime anime:source.related(detail))add(out,anime,current.url());}catch(Exception unavailable){if(Thread.currentThread().isInterrupted())throw new InterruptedException();}
            publish(out,progress);
            if(root.length()>=3&&out.size()<10){
                Set<String> seen=new HashSet<>();
                try{
                    for(int page=1;page<=3&&out.size()<10;page++){
                        if(Thread.currentThread().isInterrupted())throw new InterruptedException();
                        List<Anime> matches=source.catalog(root,page,Map.of());boolean fresh=false;
                        for(Anime anime:matches){fresh|=seen.add(anime.url());if(sameFamily(current.title(),anime.title()))add(out,anime,current.url());}
                        publish(out,progress);if(!fresh)break;
                    }
                }catch(InterruptedException cancelled){throw cancelled;}catch(Exception e){failure=e;}
            }
            if(out.size()<10)try{
                List<Anime> general=fill.get(15,TimeUnit.SECONDS);
                for(Anime anime:general)if(sameFamily(current.title(),anime.title()))add(out,anime,current.url());
                for(Anime anime:general)add(out,anime,current.url());
            }catch(InterruptedException cancelled){throw cancelled;}catch(Exception e){if(out.isEmpty())throw failure==null?e:failure;}
            return limited(out);
        }finally{fill.cancel(true);}
    }
    private static List<Anime> limited(Map<String,Anime> out){return List.copyOf(new ArrayList<>(out.values()).subList(0,Math.min(10,out.size())));}
    private static void publish(Map<String,Anime> out,Consumer<List<Anime>> progress){if(!out.isEmpty())progress.accept(limited(out));}
    private static void add(Map<String,Anime> out,Anime anime,String current) {
        if(!anime.url().equals(current))out.putIfAbsent(anime.url(),anime);
    }
    static String normalized(String title){return SourceText.normalized(title);}
    static String family(String title) {
        String name=normalized(title).replaceAll("(?i)\\s*(?:\\([^)]*(?:latino|castellano|subtitulado)[^)]*\\)|(?:espanol\\s+)?(?:latino|castellano|subtitulado))$","");
        name=name.split("[:：]",2)[0].replaceAll("[★☆]"," ").replaceAll("\\s+"," ").trim();
        // Preserve meaningful words; remove only common suffixes for series installments.
        String previous;
        do {
            previous=name;
            name=name.replaceAll("\\s+(?:brotherhood|extra edition|final season|s\\d+|alternative|progressive|shippuuden|shippuden|[ivx]+|\\d+|z|gt|super|(?:\\d+(?:st|nd|rd|th)\\s+)?(?:season|temporada)(?:\\s+\\d+)?|(?:movie|movies|pelicula|ova|ona|special|especial|part|parte)(?:\\s+\\d+)?)$","").trim();
        } while(!name.equals(previous));
        return name;
    }
    static boolean sameFamily(String current,String candidate) {
        String root=family(current),other=family(candidate);
        return !root.isBlank() && (root.equals(other)||root.length()>=7&&root.split(" ").length>=2&&(other.startsWith(root+" ")||root.startsWith(other+" ")&&other.length()>=7&&other.split(" ").length>=2));
    }
}
