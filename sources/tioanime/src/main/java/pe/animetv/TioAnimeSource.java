package pe.animetv;

import java.util.*;
import java.util.regex.*;
import java.io.IOException;
import okhttp3.*;
import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import static pe.animetv.Models.*;

/** Reads the site's HTML and data literals without executing advertisements or scripts. */
public final class TioAnimeSource implements Source {
    private final String endpoint;
    private final TimedCache<Detail> details=new TimedCache<>(48,300000);
    private final TimedCache<List<Anime>> searched=new TimedCache<>(8,300000);
    private final TimedCache<List<Filter>> filterCache=new TimedCache<>(1,3600000);
    private static final TimedCache<List<Server>> SERVERS=new TimedCache<>(24,30000);
    private static final TimedCache<Stream> STREAMS=new TimedCache<>(8,90000);
    public TioAnimeSource(){this("https://tioanime.com");}
    TioAnimeSource(String endpoint){this.endpoint=endpoint;}
    public List<String> defaultServers(){return List.of("Voe","YourUpload","Mega");}
    public boolean supportsPreload(){return true;}
    public String id(){return "tioanime";}
    public String name(){return "Tioanime";}
    public String baseUrl(){return endpoint;}
    private Document document(String url)throws Exception{
        Document d=Jsoup.parse(NativeStreams.get(url,endpoint+"/"),url);
        if(d.title().toLowerCase(Locale.ROOT).contains("just a moment"))throw new IOException("Tioanime requiere una verificación web");
        return d;
    }
    private String text(Element e,String selector){Element match=e.selectFirst(selector);return match==null?"":match.text();}
    private String image(Element e){Element image=e.selectFirst("img");return image==null?"":image.absUrl("src");}
    private String scripts(Document d){return d.select("script").stream().map(Element::data).collect(java.util.stream.Collectors.joining("\n"));}
    static JSONArray array(String script,String key)throws Exception{
        String literal=JsData.array(script,key);
        if(literal.isEmpty()&&!Pattern.compile("\\b"+Pattern.quote(key)+"\\s*=\\s*\\[\\s*]").matcher(script).find())throw new IOException("Tioanime: faltan datos de "+key);
        return new JSONArray("["+literal+"]");
    }
    private final TimedCache<List<Anime>> listingCache=new TimedCache<>(16,120000);
    public List<Anime> catalog(String query,int page,Map<String,String> filters)throws Exception {
        Map<String,String> selected=Map.copyOf(filters);String key=query.trim()+"|"+page+"|"+new TreeMap<>(selected);
        return listingCache.get(key,()->catalogUncached(query,page,selected));
    }
    private List<Anime> catalogUncached(String query,int page,Map<String,String> filters)throws Exception{
        if(!query.isBlank()&&filters.entrySet().stream().anyMatch(e->!e.getValue().isBlank()&&!e.getKey().equals("sort")))return SearchFilters.page(searched.get(query+new TreeMap<>(filters),()->SearchFilters.collect(this,p->catalog(query,p,Map.of()),filters)),page);
        HttpUrl.Builder url=HttpUrl.get(endpoint+"/directorio").newBuilder().addQueryParameter("page",String.valueOf(page));
        if(!query.isBlank())url.addQueryParameter("q",query);
        for(var entry:filters.entrySet())if(!entry.getValue().isBlank()){
            String key=entry.getKey().equals("genre")?"genero[]":entry.getKey();
            if(Set.of("genero[]","type[]","status","sort").contains(key))url.addQueryParameter(key,entry.getValue());
        }
        Document d=document(url.build().toString());List<Anime> result=new ArrayList<>();
        for(Element article:d.select("article.anime")){
            Element link=article.selectFirst("a[href^=/anime/]");if(link==null)continue;
            result.add(new Anime(text(article,"h3.title"),link.absUrl("href"),image(article)));
        }
        return List.copyOf(result);
    }
    public List<Filter> filters()throws Exception{return filterCache.get("filters",()->{
        List<Filter> out=new ArrayList<>();Document d=document(endpoint+"/directorio");
        for(Element select:d.select("select[name]")){
            String key=select.attr("name");if(!Set.of("type[]","genero[]","status","sort").contains(key))continue;
            List<String> values=new ArrayList<>(List.of("")),labels=new ArrayList<>(List.of("Todos"));
            for(Element option:select.select("option")){values.add(option.attr("value"));labels.add(option.text());}
            String label=switch(key){case "type[]"->"Tipo";case "genero[]"->"Género";case "status"->"Estado";default->"Orden";};
            out.add(new Filter(key.equals("genero[]")?"genre":key,label,List.copyOf(values),List.copyOf(labels)));
        }
        return List.copyOf(out);
    });}
    public Detail detail(Anime anime)throws Exception{return details.get(anime.url(),()->{
        HttpUrl animeUrl=HttpUrl.get(anime.url());
        if(animeUrl.encodedPath().equals("/directorio")&&animeUrl.queryParameter("calendarCover")!=null){
            String cover=animeUrl.queryParameter("calendarCover");
            Anime canonical=catalog(anime.title(),1,Map.of()).stream().filter(a->a.image().endsWith("/"+cover+".jpg")).findFirst().orElseThrow(()->new IOException("Tioanime: no se encontró la ficha del anime programado"));
            return detail(canonical);
        }
        Document d=document(anime.url());Element hero=d.selectFirst("article.anime-single");
        if(hero==null)throw new IOException("Tioanime: ficha no disponible");
        JSONArray info=array(scripts(d),"anime_info"),numbers=array(scripts(d),"episodes");
        String slug=info.getString(1),type=switch(text(hero,".meta [class^=anime-type]")){case "TV"->"Serie";case "Pelicula","Película"->"Película";case "Ova","OVA"->"OVA";case "Especial"->"Especial";default->text(hero,".meta [class^=anime-type]");};
        String rawStatus=text(hero,".status").toLowerCase(Locale.ROOT),status=rawStatus.contains("emisi")?"En emisión":rawStatus.contains("final")?"Finalizado":rawStatus.contains("proxi")||rawStatus.contains("próxi")?"Próximamente":"";
        Anime resolved=new Anime(text(hero,"h1.title"),anime.url(),image(hero),type,status);
        List<Episode> episodes=new ArrayList<>();TreeSet<Integer> sorted=new TreeSet<>();for(int n=0;n<numbers.length();n++)if(numbers.getInt(n)>0)sorted.add(numbers.getInt(n));
        for(int number:sorted)episodes.add(new Episode("Episodio "+number,endpoint+"/ver/"+slug+"-"+number,endpoint+"/uploads/thumbs/"+info.getString(0)+".jpg"));
        List<Genre> genres=new ArrayList<>();for(Element a:hero.select(".genres a[href]")){String genre=HttpUrl.get(a.absUrl("href")).queryParameter("genero");if(genre!=null)genres.add(new Genre(a.text(),genre));}
        Element background=hero.selectFirst("figure.backdrop img[src]");
        return new Detail(resolved,text(hero,".sinopsis"),List.copyOf(episodes),List.copyOf(genres),type,text(hero,".year"),status,background==null?"":background.absUrl("src"));
    });}
    private RecentEpisode recentItem(Element article,boolean titleIncludesEpisode)throws Exception{
        Element link=article.selectFirst("a[href]");if(link==null)return null;
        HttpUrl url=HttpUrl.get(endpoint+"/").resolve(link.attr("href").trim());if(url==null||!url.encodedPath().startsWith("/ver/"))return null;
        Matcher match=Pattern.compile("/ver/(.+)-(\\d+)$").matcher(url.encodedPath());if(!match.find())return null;
        String title=text(article,"h3.title"),number=match.group(2),thumb=image(article);
        if(titleIncludesEpisode)title=title.replaceFirst("\\s+"+Pattern.quote(number)+"$","");
        Anime anime=new Anime(title,endpoint+"/anime/"+match.group(1),thumb.replace("/thumbs/","/portadas/"));
        return new RecentEpisode(anime,new Episode("Episodio "+number,url.toString(),thumb),"");
    }
    public List<RecentEpisode> recent()throws Exception{
        List<RecentEpisode> out=new ArrayList<>();for(Element article:document(endpoint+"/").select("article.episode")){RecentEpisode item=recentItem(article,true);if(item!=null)out.add(item);}return List.copyOf(out);
    }
    public List<CalendarEntry> calendar()throws Exception{
        Document d=document(endpoint+"/programacion");List<CalendarEntry> out=new ArrayList<>();String[] days={"Monday","Tuesday","Wednesday","Thursday","Friday","Saturday","Sunday"};
        for(int day=0;day<days.length;day++)for(Element article:d.select("#"+days[day]+" article.episode")){
            RecentEpisode item=recentItem(article,false);Anime anime;
            if(item!=null)anime=item.anime();else{
                String title=text(article,"h3.title"),thumb=image(article);Matcher cover=Pattern.compile("/(\\d+)\\.jpg$").matcher(thumb);if(title.isBlank()||!cover.find())continue;
                // Upcoming slots expose only a cover id, not an anime URL. Resolve by that id on demand.
                String url=HttpUrl.get(endpoint+"/directorio").newBuilder().addQueryParameter("q",title).addQueryParameter("calendarCover",cover.group(1)).build().toString();
                anime=new Anime(title,url,thumb.replace("/thumbs/","/portadas/"));
            }
            String label=text(article,".time");int minutes=1440;Matcher clock=Pattern.compile("(\\d{1,2}):(\\d{2})").matcher(label);if(clock.find())minutes=Integer.parseInt(clock.group(1))*60+Integer.parseInt(clock.group(2));
            out.add(new CalendarEntry(anime,day+1,label,minutes));
        }
        return List.copyOf(out);
    }
    public List<Server> servers(Episode episode)throws Exception{return SERVERS.get(episode.url(),()->{
        JSONArray videos=array(scripts(document(episode.url())),"videos");List<Server> out=new ArrayList<>();
        for(int n=0;n<videos.length();n++){
            JSONArray video=videos.getJSONArray(n);String name=switch(video.getString(0).toLowerCase(Locale.ROOT)){case "voe"->"Voe";case "yourupload"->"YourUpload";case "mega"->"Mega";default->"";};
            String url=video.getString(1);HttpUrl parsed=HttpUrl.parse(url);
            if(parsed==null||!parsed.isHttps())continue;String host=parsed.host();
            if(name.equals("Voe")&&host.equals("voe.sx")||name.equals("YourUpload")&&(host.equals("www.yourupload.com")||host.equals("yourupload.com"))||name.equals("Mega")&&host.equals("mega.nz"))out.add(new Server(name,url,"SUB"));
        }
        out.sort(Comparator.comparingInt(s->SourceServers.rank(s.name())));return List.copyOf(out);
    });}
    public Stream resolve(Server server,String episode)throws Exception{
        return STREAMS.get(server.url()+"|"+episode,()->StreamExtractors.resolve(server,episode));
    }
    public void invalidate(Server server,String episode){STREAMS.remove(server.url()+"|"+episode);}
}
