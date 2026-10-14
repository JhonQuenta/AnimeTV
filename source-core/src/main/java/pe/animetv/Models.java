package pe.animetv;

import java.util.*;

public final class Models {
    public record Anime(String title, String url, String image,String type,String status) { public Anime(String title,String url,String image,String type){this(title,url,image,type,"");} public Anime(String title,String url,String image) { this(title,url,image,"",""); } }
    public record Episode(String name, String url, String image) { public Episode(String name,String url) { this(name,url,""); } }
    public record Server(String name, String url, String language) {}
    public record Stream(String url, String referer,String mimeType,Map<String,String> headers,String megaKey) {
        public Stream { headers=Map.copyOf(headers); megaKey=megaKey==null?"":megaKey; }
        public Stream(String url,String referer,String mimeType,Map<String,String> headers){this(url,referer,mimeType,headers,"");}
        public Stream(String url,String referer,String mimeType){this(url,referer,mimeType,Map.of());}
        public Stream(String url,String referer){this(url,referer,url.matches("(?i).*\\.m3u8(?:\\?.*)?$")?"application/x-mpegURL":"video/mp4");}
    }
    public record Filter(String key, String label, List<String> values, List<String> labels) {}
    public record Genre(String name,String slug) {}
    public record Detail(Anime anime, String description, List<Episode> episodes, List<Genre> genres,String type,String year,String status,String backdrop) {
        public Detail {backdrop=backdrop==null?"":backdrop;}
        public Detail(Anime anime,String description,List<Episode> episodes,List<Genre> genres,String type,String year,String status){this(anime,description,episodes,genres,type,year,status,"");}
        public Detail(Anime anime,String description,List<Episode> episodes) { this(anime,description,episodes,List.of(),"","",""); }
    }
    public record RecentEpisode(Anime anime,Episode episode,String published) {}
    public record CalendarEntry(Anime anime,int day,String label,int sortMinutes,String publication) {public CalendarEntry(Anime anime,int day,String label,int sortMinutes){this(anime,day,label,sortMinutes,"");}}
    public interface Source {
        int API_VERSION=1;
        String id(); String name(); String baseUrl();
        default Set<String> domains(){String host=java.net.URI.create(baseUrl()).getHost();return host.startsWith("www.")?Set.of(host,host.substring(4)):Set.of(host,"www."+host);}
        default List<String> defaultServers(){return List.of();}
        default boolean supportsPreload(){return false;}
        List<Anime> catalog(String query, int page, Map<String,String> filters) throws Exception;
        List<Filter> filters() throws Exception;
        Detail detail(Anime anime) throws Exception;
        List<Server> servers(Episode episode) throws Exception;
        Stream resolve(Server server, String episodeUrl) throws Exception;
        List<RecentEpisode> recent() throws Exception;
        List<CalendarEntry> calendar() throws Exception;
        default void invalidate(Server server,String episodeUrl) {}
        default List<Anime> related(Detail detail)throws Exception{return List.of();}
        default Detail metadata(Anime anime)throws Exception{return detail(anime);}
        default boolean matchesFilters(Detail detail,Map<String,String> filters)throws Exception{return SearchFilters.matches(detail,filters);}
    }
}


