package pe.animetv;

import java.util.*;
import static pe.animetv.Models.*;

/** Providers whose search endpoint ignores filters are intersected against real ficha metadata. */
final class SearchFilters {
    interface Page {List<Anime> load(int page)throws Exception;}
    static List<Anime> collect(Source source,Page fetch,Map<String,String> filters)throws Exception {
        Map<String,Anime> out=new LinkedHashMap<>();Set<String> seen=new HashSet<>();
        for(int page=1;page<=12;page++){
            List<Anime> items=fetch.load(page);boolean fresh=false;
            List<Anime> freshItems=new ArrayList<>();for(Anime anime:items)if(seen.add(anime.url()))freshItems.add(anime);
            fresh=!freshItems.isEmpty();Exception firstFailure=null;int loaded=0;
            for(SourceWork.Result<Detail> result:SourceWork.map(freshItems,source::metadata)){
                if(result.failure()!=null){if(firstFailure==null)firstFailure=result.failure();continue;}
                loaded++;Detail detail=result.value();if(source.matchesFilters(detail,filters))out.put(detail.anime().url(),detail.anime());
            }
            if(fresh&&loaded==0&&firstFailure!=null)throw firstFailure;
            if(!fresh)break;
            if(page==12)throw new java.io.IOException("La búsqueda es demasiado amplia para combinar filtros. Escribe un título más específico.");
        }
        return List.copyOf(out.values());
    }
    static List<Anime> page(List<Anime> items,int page){int from=Math.min(items.size(),Math.max(0,page-1)*24);return List.copyOf(items.subList(from,Math.min(items.size(),from+24)));}
    static boolean matches(Detail d,Map<String,String> filters){
        for(var entry:filters.entrySet()){
            String value=entry.getValue();if(value.isBlank()||value.equals("false"))continue;
            String key=entry.getKey();
            if(key.equals("sort"))continue;
            if((key.equals("year")||key.equals("fecha"))&&!d.year().equals(value))return false;
            if(key.equals("letra")){String name=SourceText.normalized(d.anime().title());if(value.equals("09")?!name.matches("^[0-9].*"):!name.startsWith(value.toLowerCase(Locale.ROOT)))return false;}
            if(key.equals("genre")&&!Set.of("latino","castellano").contains(value)&&d.genres().stream().noneMatch(g->g.slug().equalsIgnoreCase(value)))return false;
            if(key.equals("status")){String expected=switch(value){case "2"->"Finalizado";case "1"->"En emisión";case "3"->"Próximamente";default->value;};if(!d.status().equalsIgnoreCase(expected))return false;}
            if((key.equals("type[]")||key.equals("categoria"))&&!Set.of("latino","castellano").contains(value)){String expected=switch(value.toLowerCase(Locale.ROOT)){case "0","anime"->"Serie";case "1","película","pelicula"->"Película";case "2","ova"->"OVA";case "3","especial"->"Especial";default->value;};if(!d.type().equalsIgnoreCase(expected))return false;}
        }return true;
    }
}
