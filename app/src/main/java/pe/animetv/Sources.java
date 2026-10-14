package pe.animetv;
import java.util.*;import okhttp3.HttpUrl;import static pe.animetv.Models.*;

/** App composition root. Internal modules are registered here; no downloaded code is loaded. */
final class Sources {
    private Sources(){}
    private static final Map<String,Source> REGISTRY=register(new AnimeAv1Source(),new TioAnimeSource(),new LatanimeSource(),new AnimeLatinoHdSource(),new JkAnimeSource());
    private static Map<String,Source> register(Source... modules){LinkedHashMap<String,Source> out=new LinkedHashMap<>();for(Source source:modules){if(out.putIfAbsent(source.id(),source)!=null)throw new IllegalArgumentException("Fuente duplicada: "+source.id());}return Collections.unmodifiableMap(out);}
    static List<String> ids(){return List.copyOf(REGISTRY.keySet());}
    static Source create(String id){return REGISTRY.getOrDefault(id,REGISTRY.get("animeav1"));}
    static String nameForUrl(String url,Source fallback){return forUrl(url,fallback).name();}
    static List<String> servers(String id){return create(id).defaultServers();}
    static Source forUrl(String url,Source fallback){HttpUrl parsed=url==null?null:HttpUrl.parse(url);if(parsed==null)return fallback;for(Source source:REGISTRY.values())if(source.domains().contains(parsed.host()))return fallback!=null&&source.id().equals(fallback.id())?fallback:source;return fallback;}
}
