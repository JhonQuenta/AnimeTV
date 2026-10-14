package pe.animetv;

import java.util.*;
import java.util.regex.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import okhttp3.HttpUrl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import static pe.animetv.Models.*;

/** Independent HTML adapter; players and advertising scripts are never executed. */
public final class LatanimeSource implements Source {
    private final String endpoint;
    private final TimedCache<Detail> metadataCache=new TimedCache<>(48,300000);
    private final TimedCache<Detail> details=new TimedCache<>(48,300000);
    private final TimedCache<List<Anime>> searched=new TimedCache<>(8,300000);
    private final TimedCache<List<Filter>> filterCache=new TimedCache<>(1,3600000);
    private final TimedCache<List<Server>> serversCache=new TimedCache<>(24,30000);
    private final TimedCache<Stream> streams=new TimedCache<>(8,60000);
    private final TimedCache<List<Detail>> audioDetails=new TimedCache<>(48,300000);
    private final Map<String,LinkedHashMap<String,Anime>> audioSeeds=new LinkedHashMap<>(96,.75f,true);
    public LatanimeSource(){this("https://latanime.org");}
    LatanimeSource(String endpoint){this.endpoint=endpoint;}
    public List<String> defaultServers(){return List.of("Dsvplay","Filemoon","Vide0","Lulu","Voe","Byse","MP4Upload","Mixdrop","Do7go","Mega");}
    public boolean supportsPreload(){return true;}
    public String id(){return "latanime";}
    public String name(){return "Latanime";}
    public String baseUrl(){return endpoint;}
    private Document document(String url)throws Exception {
        Document d=Jsoup.parse(NativeStreams.get(url,endpoint+"/"),url);
        if(d.title().toLowerCase(Locale.ROOT).contains("just a moment"))throw new IOException("Latanime requiere una verificación web");return d;
    }
    private static String text(Element e,String selector){Element match=e.selectFirst(selector);return match==null?"":match.text();}
    private static String image(Element e){Element img=e==null?null:e.selectFirst("img");return img==null?"":img.absUrl(img.hasAttr("data-src")?"data-src":"src");}
    static String type(String category){String c=category.toLowerCase(Locale.ROOT);return c.contains("pel")?"Película":c.contains("ova")?"OVA":c.contains("ona")?"ONA":c.contains("especial")?"Especial":"Serie";}
    private List<Anime> cards(Document d){Map<String,Anime> out=new LinkedHashMap<>();for(Element card:d.select(".series")){
        Element a=card.closest("a[href]");if(a==null||!HttpUrl.get(a.absUrl("href")).encodedPath().startsWith("/anime/"))continue;
        String title=text(card,"h3");if(title.isBlank())continue;Anime grouped=groupAnime(new Anime(title,a.absUrl("href"),image(card),type(text(card,".seriedetails .opacity-75")),""));out.putIfAbsent(grouped.url(),grouped);
    }return List.copyOf(out.values());}
    private final TimedCache<List<Anime>> listingCache=new TimedCache<>(16,120000);
    public List<Anime> catalog(String query,int page,Map<String,String> filters)throws Exception {
        Map<String,String> selected=Map.copyOf(filters);String key=query.trim()+"|"+page+"|"+new TreeMap<>(selected);
        return listingCache.get(key,()->catalogUncached(query,page,selected));
    }
    private List<Anime> catalogUncached(String query,int page,Map<String,String> filters)throws Exception {
        if(!query.isBlank()&&filters.values().stream().anyMatch(v->!v.isBlank()&&!v.equals("false")))return SearchFilters.page(searched.get(query+new TreeMap<>(filters),()->SearchFilters.collect(this,p->catalog(query,p,Map.of()),filters)),page);
        HttpUrl.Builder url=HttpUrl.get(endpoint+(query.isBlank()?"/animes":"/buscar")).newBuilder().addQueryParameter("p",String.valueOf(page));
        if(!query.isBlank())url.addQueryParameter("q",query);
        for(var entry:filters.entrySet())if(!entry.getValue().isBlank()){String key=entry.getKey().equals("genre")?"genero":entry.getKey();if(Set.of("fecha","genero","letra","categoria").contains(key))url.addQueryParameter(key,entry.getValue());}
        return cards(document(url.build().toString()));
    }
    public List<Filter> filters()throws Exception{return filterCache.get("filters",()->{
        List<Filter> out=new ArrayList<>();for(Element select:document(endpoint+"/animes").select("select[name]")){
            String key=select.attr("name");if(!Set.of("fecha","genero","letra","categoria").contains(key))continue;
            List<String> values=new ArrayList<>(List.of("")),labels=new ArrayList<>(List.of("Todos"));for(Element option:select.select("option"))if(!option.attr("value").equals("false")){values.add(option.attr("value"));labels.add(option.text());}
            out.add(new Filter(key.equals("genero")?"genre":key,switch(key){case "fecha"->"Año";case "genero"->"Género / audio";case "letra"->"Letra";default->"Tipo / audio";},List.copyOf(values),List.copyOf(labels)));
        }return List.copyOf(out);
    });}
    static String seriesKey(String slug){return slug.replaceFirst("-(?:latino|castellano)$","");}
    public boolean matchesFilters(Detail detail,Map<String,String> filters)throws Exception {
        if(!SearchFilters.matches(detail,filters))return false;
        String audio=filters.getOrDefault("genre","");if(!Set.of("latino","castellano").contains(audio))audio=filters.getOrDefault("categoria","");
        if(Set.of("latino","castellano").contains(audio)){String wanted=audio.equals("latino")?"LAT":"CAST";return versions(detail.anime().url()).stream().anyMatch(d->SourceAudio.fromTitle(d.anime().title()).equals(wanted));}return true;
    }
    private static String cleanTitle(String title){return title.replaceFirst("(?i)\\s+(?:latino|castellano)$","");}
    private String groupUrl(String key){return endpoint+"/anime/"+key+"?animetvAudio=all";}
    private String slug(String url){List<String> parts=HttpUrl.get(url).pathSegments();return parts.get(parts.size()-1);}
    private boolean grouped(String url){return "all".equals(HttpUrl.get(url).queryParameter("animetvAudio"));}
    private Anime groupAnime(Anime anime){
        String key=seriesKey(slug(anime.url())),url=groupUrl(key);
        synchronized(audioSeeds){audioSeeds.computeIfAbsent(url,k->new LinkedHashMap<>()).put(anime.url(),anime);while(audioSeeds.size()>96)audioSeeds.remove(audioSeeds.keySet().iterator().next());}
        return new Anime(cleanTitle(anime.title()),url,anime.image(),anime.type(),anime.status());
    }
    private List<Detail> versions(String url)throws Exception{return audioDetails.get(url,()->{
        String key=seriesKey(slug(url));LinkedHashMap<String,Anime> candidates=new LinkedHashMap<>();synchronized(audioSeeds){if(audioSeeds.containsKey(url))candidates.putAll(audioSeeds.get(url));}
        List<Detail> result=new ArrayList<>();Set<String> loaded=new HashSet<>();Exception last=null;
        // Known real catalog links first. Candidate pages are never exposed unless their real ficha is valid.
        List<String> paths=new ArrayList<>(candidates.keySet());paths.add(endpoint+"/anime/"+key+"-latino");paths.add(endpoint+"/anime/"+key+"-castellano");paths.add(endpoint+"/anime/"+key);
        for(String candidate:paths){if(!loaded.add(candidate))continue;
            String expected=candidate.endsWith("-latino")?"LAT":candidate.endsWith("-castellano")?"CAST":"";
            if(!expected.isBlank()&&result.stream().anyMatch(d->SourceAudio.fromTitle(d.anime().title()).equals(expected)))continue;
            if(expected.isBlank()&&result.stream().anyMatch(d->SourceAudio.fromTitle(d.anime().title()).equals("LAT")))continue;
            try{
                Detail detail=rawDetail(candidates.getOrDefault(candidate,new Anime("",candidate,"")));
                if(!seriesKey(slug(detail.anime().url())).equals(key)||detail.episodes().isEmpty())continue;
                if(!result.isEmpty()&&!result.get(0).year().isBlank()&&!detail.year().isBlank()&&!result.get(0).year().equals(detail.year()))continue;
                String language=SourceAudio.fromTitle(detail.anime().title());if(result.stream().noneMatch(d->SourceAudio.fromTitle(d.anime().title()).equals(language)))result.add(detail);
            }catch(IOException unavailable){last=unavailable;}
        }
        if(result.isEmpty())throw new IOException("Latanime: no se encontraron episodios de esta serie",last);
        result.sort(Comparator.comparingInt(d->switch(SourceAudio.fromTitle(d.anime().title())){case "LAT"->0;case "CAST"->1;default->2;}));return List.copyOf(result);
    });}
    public Detail metadata(Anime anime)throws Exception {
        if(!grouped(anime.url()))return rawDetail(anime);
        return metadataCache.get(anime.url(),()->{
            List<Anime> seeds; synchronized(audioSeeds){seeds=audioSeeds.containsKey(anime.url())?List.copyOf(audioSeeds.get(anime.url()).values()):List.of();}
            if(seeds.isEmpty())return detail(anime);
            Exception last=null;
            for(Anime seed:seeds)try{
                Detail raw=rawDetail(seed);
                // Card metadata only needs the real catalogue ficha. Do not wait for
                // other dubs or reject an upcoming title because it has no episodes.
                Anime grouped=new Anime(cleanTitle(raw.anime().title()),anime.url(),raw.anime().image(),raw.type(),raw.status());
                return new Detail(grouped,raw.description(),List.of(),raw.genres(),raw.type(),raw.year(),raw.status(),raw.backdrop());
            }catch(Exception unavailable){last=unavailable;}
            throw new IOException("Latanime: no se pudo consultar el estado de esta ficha",last);
        });
    }
    public Detail detail(Anime anime)throws Exception {
        if(!grouped(anime.url()))return rawDetail(anime);
        return details.get(anime.url(),()->{
            List<Detail> versions=versions(anime.url());Detail primary=versions.get(0);TreeMap<Integer,Episode> episodes=new TreeMap<>();String key=seriesKey(slug(anime.url()));
            for(Detail version:versions)for(Episode e:version.episodes()){
                int number=episodeNumber(e.url());episodes.putIfAbsent(number,new Episode("Episodio "+number,endpoint+"/ver/"+key+"-episodio-"+number+"?animetvAudio=all",e.image()));
            }
            return new Detail(new Anime(cleanTitle(primary.anime().title()),anime.url(),primary.anime().image(),primary.type(),primary.status()),primary.description(),List.copyOf(episodes.values()),primary.genres(),primary.type(),primary.year(),primary.status(),primary.backdrop());
        });
    }
    private static int episodeNumber(String url)throws IOException {Matcher m=Pattern.compile("-episodio-(\\d+)$").matcher(HttpUrl.get(url).encodedPath());if(!m.find())throw new IOException("Latanime: número de episodio inválido");return Integer.parseInt(m.group(1));}
    private String episodeGroup(String url)throws IOException {String key=slug(url).replaceFirst("-episodio-\\d+$","");episodeNumber(url);return groupUrl(seriesKey(key));}
    private Map<String,Episode> episodeVersions(String url)throws Exception {
        Map<String,Episode> out=new LinkedHashMap<>();int number=episodeNumber(url);for(Detail detail:versions(episodeGroup(url)))for(Episode episode:detail.episodes())if(episodeNumber(episode.url())==number)out.put(SourceAudio.fromTitle(detail.anime().title()),episode);return out;
    }
    private Detail rawDetail(Anime anime)throws Exception{return details.get(anime.url(),()->{
        Document d=document(anime.url());Element heading=d.selectFirst(".col-lg-9.col-md-8 > h2");if(heading==null)throw new IOException("Latanime: ficha no disponible");Element info=heading.parent();
        String title=heading.text(),status=text(d,".btn-estado.finalizado");if(status.isBlank())status=text(d,".btn-estado.emision");if(status.isBlank())status=text(d,".serieimgficha .btn-estado:not(#follow_d)");
        status=statusLabel(d);
        String cover=image(d.selectFirst(".serieimgficha")),kind=anime.type().isBlank()?type(title):anime.type(),year="";Matcher y=Pattern.compile("\\b(?:19|20)\\d{2}\\b").matcher(text(info,".span-tiempo"));if(y.find())year=y.group();
        List<Genre> genres=new ArrayList<>();for(Element a:info.select("a[href*=/genero/]")){HttpUrl u=HttpUrl.get(a.absUrl("href"));genres.add(new Genre(a.text(),u.pathSegments().get(u.pathSegments().size()-1)));}
        TreeMap<Integer,Episode> episodes=new TreeMap<>();for(Element a:info.select("a[href*=/ver/]:has(.cap-layout)")){
            String url=a.absUrl("href");Matcher n=Pattern.compile("-episodio-(\\d+)$").matcher(HttpUrl.get(url).encodedPath());if(n.find())episodes.put(Integer.parseInt(n.group(1)),new Episode("Episodio "+n.group(1),url,image(a)));
        }
        List<Episode> ordered=List.copyOf(episodes.values());
        return new Detail(new Anime(title,anime.url(),cover,kind,status),text(info,"p.my-2.opacity-75"),ordered,List.copyOf(genres),kind,year,status,sharedArtwork(ordered));
    });}
    static String sharedArtwork(List<Episode> episodes){
        if(episodes.size()<2)return "";String image=episodes.get(0).image();HttpUrl url=HttpUrl.parse(image);
        if(url==null||!url.isHttps()||!url.encodedPath().contains("/thumbs/portada/"))return "";
        return episodes.stream().allMatch(e->image.equals(e.image()))?image:"";
    }
    static String statusLabel(Document d){
        for(Element badge:d.select(".btn-estado:not(#follow_d),[data-status],[data-estado]")){
            String value=SourceText.normalized(badge.text()+" "+badge.className()+" "+badge.attr("data-status")+" "+badge.attr("data-estado"));
            if(value.matches(".*\\b(finalizado|finalizada|completed|finished)\\b.*"))return "Finalizado";
            if(value.matches(".*\\b(emision|emitiendo|ongoing|airing)\\b.*"))return "En emisión";
        }
        return d.select(".btn-estado.estreno").isEmpty()?"":"Estreno";
    }
    public List<RecentEpisode> recent()throws Exception {
        List<RecentEpisode> out=new ArrayList<>();for(Element a:document(endpoint+"/").select("a[href*=/ver/]:has(.imgrec)")){
            String url=a.absUrl("href");Matcher n=Pattern.compile("/ver/(.+)-episodio-(\\d+)$").matcher(HttpUrl.get(url).encodedPath());if(!n.find())continue;
            String title=text(a,".info h2").replaceFirst("^Episodio\\s+\\d+\\s*-\\s*","");String thumb=image(a);
            out.add(new RecentEpisode(new Anime(title,endpoint+"/anime/"+n.group(1),thumb,"Serie",""),new Episode("Episodio "+n.group(2),url,thumb),text(a,".span-tiempo")));
        }Map<String,RecentEpisode> combined=new LinkedHashMap<>();for(RecentEpisode r:out){Anime group=groupAnime(r.anime());int number=episodeNumber(r.episode().url());String key=seriesKey(slug(group.url()));String url=endpoint+"/ver/"+key+"-episodio-"+number+"?animetvAudio=all";combined.putIfAbsent(url,new RecentEpisode(group,new Episode(r.episode().name(),url,r.episode().image()),r.published()));}return List.copyOf(combined.values());
    }
    public List<CalendarEntry> calendar()throws Exception {
        Document d=document(endpoint+"/calendario");List<CalendarEntry> out=new ArrayList<>();String[] days={"lunes","martes","miercoles","jueves","viernes","sabado","domingo"};
        for(int day=0;day<7;day++)for(Element a:d.select("#"+days[day]+"-tap-pane a[href*=/anime/]"))out.add(new CalendarEntry(new Anime(text(a,"h3"),a.absUrl("href"),image(a)),day+1,text(a,".badge"),1440));
        Map<String,CalendarEntry> combined=new LinkedHashMap<>();for(CalendarEntry entry:out){Anime anime=groupAnime(entry.anime());combined.putIfAbsent(entry.day()+"|"+anime.url(),new CalendarEntry(anime,entry.day(),entry.label(),entry.sortMinutes()));}return List.copyOf(combined.values());
    }
    static String serverName(String url){HttpUrl u=HttpUrl.parse(url);if(u==null||!u.isHttps())return "";String h=u.host();return switch(h){
        case "playmogo.com","dsvplay.com"->"Dsvplay";
        case "vide0.net"->"Vide0";
        case "do7go.com"->"Do7go";
        case "filemoon.sx","filemoon.to","filemoon.in"->"Filemoon";
        case "luluvdoo.com","luluvdo.com","lulustream.com"->"Lulu";
        case "bysekoze.com","byse.sx","byse.in"->"Byse";
        case "voe.sx"->"Voe";
        case "mp4upload.com","www.mp4upload.com"->"MP4Upload";
        case "mega.nz"->"Mega";
        case "mixdrop.top","mxdrop.top","mxdrop.to","mixdrop.co","mixdrop.to"->"Mixdrop";
        default->"";
    };}
    public List<Server> servers(Episode episode)throws Exception{return serversCache.get(episode.url(),()->{
        if(grouped(episode.url())){List<Server> out=new ArrayList<>();IOException last=null;for(var entry:episodeVersions(episode.url()).entrySet())try{for(Server server:servers(entry.getValue()))out.add(new Server(server.name(),server.url(),entry.getKey()));}catch(IOException unavailable){last=unavailable;}if(out.isEmpty()&&last!=null)throw last;return List.copyOf(out);}
        Document d=document(episode.url());LinkedHashMap<String,Server> out=new LinkedHashMap<>();String label=(text(d,"h2.mojon4")+" "+episode.url()).toLowerCase(Locale.ROOT);String lang=label.contains("latino")||label.contains("castellano")||label.contains("catalan")||label.contains("catalán")?"DUB":"SUB";
        for(Element a:d.select("a.play-video[data-player]"))try{
            String url=new String(Base64.getDecoder().decode(a.attr("data-player")),StandardCharsets.UTF_8),name=serverName(url);if(!name.isBlank())out.putIfAbsent(url,new Server(name,url,lang));
        }catch(IllegalArgumentException invalid){/* Ignore malformed player records, never execute them. */}
        if(d.select("a.play-video[data-player]").isEmpty())throw new IOException("Latanime: faltan enlaces de reproducción");return List.copyOf(out.values());
    });}
    String playbackEpisode(Server server,String episode)throws Exception{if(!grouped(episode))return episode;Episode version=episodeVersions(episode).get(server.language());if(version==null)throw new IOException("Latanime: este episodio no está disponible en "+SourceAudio.label(server.language()));return version.url();}
    public Stream resolve(Server server,String episode)throws Exception{
        final String referer=playbackEpisode(server,episode);return streams.get(server.url()+"|"+episode+"|"+server.language(),()->StreamExtractors.resolve(server,referer));}
    public void invalidate(Server server,String episode){streams.remove(server.url()+"|"+episode+"|"+server.language());}
}
