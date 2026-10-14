package pe.animetv;
import java.util.*;
import static pe.animetv.Models.*;

/** A non-empty page is not proof of another page. Require new, real result URLs. */
final class CatalogPagination {
    static boolean hasMore(List<Anime> current,List<Anime> next){
        if(current.isEmpty())return false;Set<String> urls=new HashSet<>();for(Anime anime:current)urls.add(anime.url());
        return next.stream().anyMatch(anime->!anime.url().isBlank()&&!urls.contains(anime.url()));
    }
}
