package pe.animetv;

import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.*;
import static pe.animetv.Models.*;

/** Independent adapter. No Aniyomi APK loading or execution of website JavaScript. */
public final class AnimeAv1Source implements Source {
    private final String endpoint;
    private final TimedCache<Detail> details=new TimedCache<>(48,300000);
    private final TimedCache<List<Filter>> filterCache=new TimedCache<>(1,3600000);
    public AnimeAv1Source() { this("https://animeav1.com"); }
    AnimeAv1Source(String endpoint) { this.endpoint=endpoint; }

    public List<String> defaultServers(){return List.of("Voe","Byse","MP4Upload","HLS");}
    public boolean supportsPreload(){return true;}
    public String id() { return "animeav1"; }
    public String name() { return "AnimeAV1"; }
    public String baseUrl() { return endpoint; }
    private Document document(String url) throws Exception {
        return document(url,baseUrl()+"/");
    }
    private Document document(String url,String referer) throws Exception {
        Request r=new Request.Builder().url(url).header("User-Agent","Mozilla/5.0").header("Referer",referer).build();
        String html=SourceHttp.get(SourceNetwork.HTTP,r,"La fuente respondió");return Jsoup.parse(html,url);
    }
    private final TimedCache<List<Anime>> listingCache=new TimedCache<>(16,120000);
    public List<Anime> catalog(String query,int page,Map<String,String> filters)throws Exception {
        Map<String,String> selected=Map.copyOf(filters);String key=query.trim()+"|"+page+"|"+new TreeMap<>(selected);
        return listingCache.get(key,()->catalogUncached(query,page,selected));
    }
    private List<Anime> catalogUncached(String query,int page,Map<String,String> filters) throws Exception {
        HttpUrl.Builder u=HttpUrl.get(baseUrl()+"/catalogo").newBuilder().addQueryParameter("page",String.valueOf(page));
        if(!query.isBlank()) u.addQueryParameter("search",query);
        filters.forEach((k,v)-> { if(!v.isBlank()) { if(k.equals("year")) { u.addQueryParameter("minYear",v); u.addQueryParameter("maxYear",v); } else u.addQueryParameter(k,v); } });
        Document d=document(u.build().toString()); List<Anime> out=new ArrayList<>();
        Map<String,String> types=typeMap(d),statuses=statusMap(d);
        for(Element article:d.select("article")) {
            Element title=article.selectFirst("h3"), link=article.selectFirst("a[href]"), image=article.selectFirst("img");
            if(title!=null && link!=null) out.add(new Anime(title.text(),link.absUrl("href"),image==null?"":image.absUrl("src"),types.getOrDefault(link.absUrl("href"),""),statuses.getOrDefault(link.absUrl("href"),"")));
        }
        if(out.isEmpty() && d.title().toLowerCase(Locale.ROOT).contains("just a moment")) throw new java.io.IOException("La fuente requiere una verificación web");
        return out;
    }
    public List<Filter> filters() throws Exception {return filterCache.get("filters",()->List.copyOf(loadFilters()));}
    private List<Filter> loadFilters() throws Exception {
        List<Filter> out=new ArrayList<>(); Document d=document(baseUrl()+"/catalogo");
        for(Element select:d.select("select[name]")) {
            List<String> values=new ArrayList<>(),labels=new ArrayList<>();
            for(Element option:select.select("option")) { values.add(option.attr("value")); labels.add(option.text()); }
            String key=select.attr("name");
            if(!values.isEmpty()) out.add(new Filter(key,select.attr("aria-label").isBlank()?key:select.attr("aria-label"),values,labels));
        }
        if(out.isEmpty()) {
            String scripts=d.select("script").stream().map(Element::data).collect(java.util.stream.Collectors.joining("\n"));
            int start=scripts.indexOf("genresIdsMap:");
            if(start>=0) {
                List<String> values=new ArrayList<>(List.of("")), labels=new ArrayList<>(List.of("Todos"));
                Matcher genre=Pattern.compile("id:\\d+,name:\"([^\"]+)\",type:\\d+,slug:\"([^\"]+)\"").matcher(scripts.substring(start));
                while(genre.find()) { if(!values.contains(genre.group(2))) { labels.add(genre.group(1)); values.add(genre.group(2)); } }
                if(values.size()>1) out.add(new Filter("genre","Género",values,labels));
            }
            out.add(new Filter("category","Tipo",List.of("","tv-anime","pelicula","ova","especial"),List.of("Todos","Serie","Película","OVA","Especial")));
            out.add(new Filter("status","Estado",List.of("","emision","finalizado","proximamente"),List.of("Todos","En emisión","Finalizado","Próximamente")));
            Matcher years=Pattern.compile("years:\\[(\\d{4}),(\\d{4})]").matcher(scripts);
            if(years.find()) { List<String> values=new ArrayList<>(List.of("")),labels=new ArrayList<>(List.of("Todos"));
                int lower=Integer.parseInt(years.group(1)),upper=Integer.parseInt(years.group(2));
                if(upper-lower<150) for(int year=upper;year>=lower;year--) { values.add(String.valueOf(year)); labels.add(String.valueOf(year)); }
                if(values.size()>1)out.add(new Filter("year","Año",values,labels));
            }
        }
        return out;
    }
    public Detail detail(Anime anime) throws Exception {return details.get(anime.url(),()->loadDetail(anime));}
    private Detail loadDetail(Anime anime) throws Exception {
        Document d=document(anime.url()); List<Episode> episodes=new ArrayList<>();
        Pattern list=Pattern.compile("episodes\\s*:\\s*\\[([^]]*)]",Pattern.DOTALL);
        Pattern number=Pattern.compile("number\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");
        for(Element script:d.select("script")) {
            Matcher m=list.matcher(script.data()); if(!m.find()) continue;
            Matcher n=number.matcher(m.group(1)); while(n.find()) episodes.add(new Episode("Episodio "+n.group(1),anime.url().replaceAll("/$","")+"/"+n.group(1)));
        }
        Map<String,String> thumbnails=new HashMap<>();
        Map<String,String> types=typeMap(d),statuses=statusMap(d);
        for(Element article:d.select("article")) { Element link=article.selectFirst("a[href]"), image=article.selectFirst("img"); if(link!=null && image!=null)thumbnails.put(link.absUrl("href"),image.absUrl("src")); }
        episodes=episodes.stream().distinct().map(e->new Episode(e.name(),e.url(),thumbnails.getOrDefault(e.url(),""))).collect(java.util.stream.Collectors.toList());
        episodes.sort(Comparator.comparingDouble(e->Double.parseDouble(e.name().substring(9))));
        Element description=d.selectFirst(".entry p");
        List<Genre> genres=new ArrayList<>();
        for(Element link:d.select("main header a[href*=genre=]")) { String slug=HttpUrl.get(link.absUrl("href")).queryParameter("genre"); if(slug!=null)genres.add(new Genre(link.text(),slug)); }
        String type="",year="",status=""; Element header=d.selectFirst("main header");
        if(header!=null)for(Element span:header.select("span")) { String t=span.text(); if(t.matches("\\d{4}"))year=t; if(t.equals("TV Anime")||t.equals("Película")||t.equals("OVA")||t.equals("ONA")||t.equals("Especial"))type=t; if(t.equals("En emisión")||t.equals("Finalizado")||t.equals("Próximamente"))status=t; }
        Element poster=d.selectFirst("main img[src*='/covers/']");
        Anime complete=new Anime(anime.title(),anime.url(),poster==null?anime.image():poster.absUrl("src"),displayType(type),status);
        Element background=null;for(Element candidate:d.select("main img[src*='/backdrops/']")){
            HttpUrl coverUrl=HttpUrl.parse(complete.image()),backgroundUrl=HttpUrl.parse(candidate.absUrl("src"));
            boolean sameId=coverUrl!=null&&backgroundUrl!=null&&coverUrl.pathSegments().get(coverUrl.pathSegments().size()-1).equals(backgroundUrl.pathSegments().get(backgroundUrl.pathSegments().size()-1));
            if(sameId||candidate.attr("alt").equalsIgnoreCase(anime.title()+" Backdrop")){background=candidate;break;}
        }
        return new Detail(complete,description==null?"":description.text(),episodes,genres,type,year,status,background==null?"":background.absUrl("src"));
    }
    public List<RecentEpisode> recent() throws Exception { return parseRecent(document(baseUrl()+"/")); }
    static List<RecentEpisode> parseRecent(Document d) {
        List<RecentEpisode> out=new ArrayList<>(); Set<String> seen=new HashSet<>();
        Map<String,String> types=typeMap(d),statuses=statusMap(d);
        for(Element article:d.select("article")) {
            Element link=article.selectFirst("a[href]"),image=article.selectFirst("img"),header=article.selectFirst("header");
            if(link==null || header==null)continue; String url=link.absUrl("href");
            if(!url.matches(".*/media/[^/]+/[0-9]+(?:\\.[0-9]+)?") || !seen.add(url))continue;
            String parent=url.substring(0,url.lastIndexOf('/')),number=url.substring(url.lastIndexOf('/')+1),thumb=image==null?"":image.absUrl("src");
            String poster=thumb.replace("/thumbnails/","/covers/"); String published="";
            for(Element span:article.select("span")) if(span.text().matches("(?i).*(hace|ayer|hoy).*")) { published=span.text(); break; }
            out.add(new RecentEpisode(new Anime(header.text(),parent,poster,types.getOrDefault(parent,""),statuses.getOrDefault(parent,"")),new Episode("Episodio "+number,url,thumb),published));
        } return out;
    }
    public List<CalendarEntry> calendar() throws Exception {
        Document d=document(baseUrl()+"/horario"); String scripts=d.select("script").stream().map(Element::data).collect(java.util.stream.Collectors.joining("\n"));
        return parseCalendar(scripts,baseUrl(),java.time.ZoneId.systemDefault());
    }
    static List<CalendarEntry> parseCalendar(String scripts,String base,java.time.ZoneId zone) {
        List<CalendarEntry> out=new ArrayList<>();
        for(String object:JsData.objects(JsData.array(scripts,"media"))) {
            String latest=JsData.object(object,"latestEpisode"),date=JsData.value(latest,"createdAt"); if(date.isEmpty())continue;
            try {
                java.time.ZonedDateTime time=java.time.OffsetDateTime.parse(date).atZoneSameInstant(zone);
                String id=JsData.number(object,"id"),slug=JsData.value(object,"slug"),title=JsData.value(object,"title"); if(slug.isEmpty()||title.isEmpty())continue;
                Anime anime=new Anime(title,base+"/media/"+slug,"https://cdn.animeav1.com/covers/"+id+".jpg",displayType(JsData.value(JsData.object(object,"category"),"name")),displayStatus(JsData.value(object,"status")));
                String label=time.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"));
                out.add(new CalendarEntry(anime,time.getDayOfWeek().getValue(),label,time.getHour()*60+time.getMinute(),time.format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy · HH:mm"))));
            } catch(java.time.DateTimeException ignored) {}
        }
        out.sort(Comparator.comparingInt(CalendarEntry::day).thenComparingInt(CalendarEntry::sortMinutes)); return out;
    }
    private static final TimedCache<List<Server>> SERVERS=new TimedCache<>(24,30000);
    private static final TimedCache<Stream> STREAMS=new TimedCache<>(8,90000);
    public List<Server> servers(Episode episode) throws Exception { return SERVERS.get(episode.url(),()->List.copyOf(parseServers(document(episode.url())))); }
    static List<Server> parseServers(Document document) {
        List<Server> out=new ArrayList<>();
        for(Element script:document.select("script")) {
            String embeds=JsData.object(script.data(),"embeds");
            for(String language:List.of("SUB","DUB"))for(String item:JsData.objects(JsData.array(embeds,language))) {
                String name=JsData.value(item,"server"),url=JsData.value(item,"url");
                if((name.equals("Voe")||name.equals("Byse")||name.equals("MP4Upload")||name.equals("HLS")&&ZillaStreams.supports(url)) && url.startsWith("https://"))out.add(new Server(name,url,language));
            }
        }
        return out.stream().distinct().sorted(Comparator.comparingInt(s->SourceServers.rank(s.name()))).collect(java.util.stream.Collectors.toList());
    }
    public Stream resolve(Server server,String episodeUrl) throws Exception {
        if(!server.name().equals("Voe")&&!server.name().equals("Byse")&&!server.name().equals("MP4Upload")&&!server.name().equals("HLS"))throw new java.io.IOException("Servidor no disponible: "+server.name());
        return STREAMS.get(server.url()+"|"+episodeUrl,()->StreamExtractors.resolve(new Server(Set.of("Voe","MP4Upload","HLS").contains(server.name())?server.name():"Byse",server.url(),server.language()),episodeUrl));
    }
    public void invalidate(Server server,String episodeUrl){invalidateStream(server,episodeUrl);}
    static void invalidateStream(Server server,String episodeUrl){STREAMS.remove(server.url()+"|"+episodeUrl);}
    static Map<String,String> typeMap(Document d) {
        Map<String,String> types=new HashMap<>();String scripts=d.select("script").stream().map(Element::data).collect(java.util.stream.Collectors.joining("\n"));
        Matcher m=Pattern.compile("categoryId:(\\d+),slug:\"([^\"]+)\"").matcher(scripts);
        Map<String,String> labels=Map.of("1","Serie","2","Película","3","OVA","4","Especial","5","ONA");
        while(m.find())types.put(HttpUrl.get(d.baseUri()).resolve("/media/"+m.group(2)).toString(),labels.getOrDefault(m.group(1),""));
        for(String key:List.of("latestMedia","featured","media"))for(String item:JsData.objects(JsData.array(scripts,key))) {
            String type=displayType(JsData.value(JsData.object(item,"category"),"name")),slug=JsData.value(item,"slug");
            if(!type.isBlank()&&!slug.isBlank())types.put(HttpUrl.get(d.baseUri()).resolve("/media/"+slug).toString(),type);
        }
        return types;
    }
    static Map<String,String> statusMap(Document d){
        Map<String,String> result=new HashMap<>();String scripts=d.select("script").stream().map(Element::data).collect(java.util.stream.Collectors.joining("\n"));
        for(String key:List.of("latestMedia","featured","media","latestEpisodes"))for(String object:JsData.objects(JsData.array(scripts,key))){
            String nested=JsData.object(object,"media"),item=nested.isBlank()?object:nested;String slug=JsData.value(item,"slug"),status=displayStatus(JsData.value(item,"status"));
            if(!slug.isBlank()&&!status.isBlank())result.put(HttpUrl.get(d.baseUri()).resolve("/media/"+slug).toString(),status);
        }return result;
    }
    static String displayStatus(String value){return switch(value){case "emision", "En emisión"->"En emisión";case "finalizado", "Finalizado"->"Finalizado";case "proximamente", "Próximamente"->"Próximamente";default->"";};}
    static String displayType(String type) { return type.equals("TV Anime")?"Serie":type; }
}


