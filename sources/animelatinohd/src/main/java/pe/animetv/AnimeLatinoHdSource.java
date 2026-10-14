package pe.animetv;

import java.util.*;
import java.io.IOException;
import okhttp3.HttpUrl;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.json.*;
import static pe.animetv.Models.*;

public final class AnimeLatinoHdSource implements Source {
    interface Pages {String get(String url)throws Exception;}
    private final String endpoint;private final Pages pages,catalogPages,metadataPages;
    private final TimedCache<List<Anime>> catalogs=new TimedCache<>(12,300000);
    private final TimedCache<List<Anime>> relations=new TimedCache<>(48,300000);
    private final TimedCache<Detail> details=new TimedCache<>(48,300000);
    private final TimedCache<List<Filter>> filters=new TimedCache<>(1,3600000);
    private final TimedCache<List<Server>> servers=new TimedCache<>(24,30000);
    private final TimedCache<Stream> streams=new TimedCache<>(8,60000);
    public AnimeLatinoHdSource(){this("https://www.animelatinohd.com",SourcePageAccess::get,SourcePageAccess::catalog,url->SourcePageAccess.get(url,false));}
    AnimeLatinoHdSource(String endpoint,Pages pages){this(endpoint,pages,pages);}
    AnimeLatinoHdSource(String endpoint,Pages pages,Pages catalogPages){this(endpoint,pages,catalogPages,pages);}
    private AnimeLatinoHdSource(String endpoint,Pages pages,Pages catalogPages,Pages metadataPages){this.endpoint=endpoint;this.pages=pages;this.catalogPages=catalogPages;this.metadataPages=metadataPages;}
    public List<String> defaultServers(){return List.of("Delta","Epsilon");}
    public String id(){return "animelatinohd";}
    public String name(){return "AnimeLatinoHD";}
    public String baseUrl(){return endpoint;}
    private Document document(String url)throws Exception{return Jsoup.parse(pages.get(url),url);}
    static String image(String path,String size){if(path.startsWith("https://"))return path;return path.startsWith("/")?"https://image.tmdb.org/t/p/"+size+path:"";}
    static String kind(String value){return switch(value){case "TV"->"Serie";case "Movie"->"Película";case "Special"->"Especial";default->value;};}
    static String status(JSONObject anime){if(!anime.has("status")||anime.isNull("status"))return "";return switch(anime.optInt("status",-1)){case 0->"Finalizado";case 1->"En emisión";case 2->"Pausado";case 3->"Próximamente";default->"";};}
    private Anime anime(JSONObject a)throws Exception{return new Anime(a.getString("name"),endpoint+"/anime/"+a.getString("slug"),image(a.optString("poster"),"w500"),kind(a.optString("type")),status(a));}
    public List<Anime> catalog(String query,int page,Map<String,String> values)throws Exception {
        HttpUrl.Builder url=HttpUrl.get(endpoint+"/directorio").newBuilder().addQueryParameter("page",String.valueOf(page));if(!query.isBlank())url.addQueryParameter("search",query);
        for(var entry:values.entrySet())if(Set.of("genre","year","status","type").contains(entry.getKey())&&!entry.getValue().isBlank())url.addQueryParameter(entry.getKey(),entry.getValue());
        String address=url.build().toString();return catalogs.get(address,()->{
        String html=catalogPages.get(address);Document d=Jsoup.parse(html,address);
        if(html.startsWith("<!-- animetv-rendered-catalog -->")){
            java.util.regex.Matcher pagination=java.util.regex.Pattern.compile("PÁGINA\\s+(\\d+)\\s*/\\s*(\\d+)").matcher(d.select("main").text());if(pagination.find()&&page>Integer.parseInt(pagination.group(2)))return List.of();
            List<Anime> out=new ArrayList<>();for(Element a:d.select("main a[href^=/anime/]")){Element name=a.selectFirst("h3"),img=a.selectFirst("img");if(name!=null&&img!=null)out.add(new Anime(name.text(),a.absUrl("href"),img.absUrl("src"),kind(values.getOrDefault("type","")),filteredStatus(values)));}return List.copyOf(out);
        }
        JSONObject pagination=NextPage.find(d,"current_page","data");if(page>pagination.optInt("last_page",Integer.MAX_VALUE))return List.of();
        List<Anime> out=new ArrayList<>();JSONArray array=pagination.getJSONArray("data");for(int n=0;n<array.length();n++)out.add(anime(array.getJSONObject(n)));return List.copyOf(out);});
    }
    private static String filteredStatus(Map<String,String> values)throws Exception {String value=values.get("status");return value==null||value.isBlank()?"":status(new JSONObject().put("status",value));}
    public Detail metadata(Anime item)throws Exception {return details.get(item.url(),()->parseDetail(item,Jsoup.parse(metadataPages.get(item.url()),item.url())));}
    public List<Filter> filters()throws Exception{return filters.get("filters",()->{
        Document d=document(endpoint+"/directorio");List<Filter> out=new ArrayList<>();String[] keys={"type","status","year","genre"},names={"Tipo","Estado","Año","Género"};List<Element> selects=d.select("main select");
        for(int i=0;i<Math.min(4,selects.size());i++){List<String> values=new ArrayList<>(),labels=new ArrayList<>();for(Element option:selects.get(i).select("option")){values.add(option.attr("value"));labels.add(option.text());}out.add(new Filter(keys[i],names[i],List.copyOf(values),List.copyOf(labels)));}return List.copyOf(out);
    });}
    public Detail detail(Anime item)throws Exception{return details.get(item.url(),()->parseDetail(item,document(item.url())));}
    private Detail parseDetail(Anime item,Document document)throws Exception {
        JSONObject data=NextPage.find(document,"anime","episodes"),a=data.getJSONObject("anime");Anime resolved=anime(a);String year=a.optString("aired");if(year.length()>=4)year=year.substring(0,4);else year="";
        JSONObject related=data.optJSONObject("relations");List<Anime> links=new ArrayList<>();if(related!=null)for(String key:List.of("prequel","sequel","related")){JSONArray list=related.optJSONArray(key);if(list!=null)for(int n=0;n<list.length();n++){JSONObject entry=list.optJSONObject(n);if(entry!=null&&entry.has("name")&&entry.has("slug"))links.add(anime(entry));}}relations.put(item.url(),List.copyOf(links));
        List<Genre> genres=new ArrayList<>();for(String slug:a.optString("genres").split(","))if(!slug.isBlank()){String label=slug.replace('-',' ');genres.add(new Genre(Character.toUpperCase(label.charAt(0))+label.substring(1),slug));}
        TreeMap<Double,Episode> ordered=new TreeMap<>();JSONArray eps=data.getJSONArray("episodes");for(int n=0;n<eps.length();n++){JSONObject e=eps.getJSONObject(n);String number=e.get("number").toString();ordered.put(Double.parseDouble(number),new Episode("Episodio "+number,endpoint+"/ver/"+a.getString("slug")+"/"+number,image(a.optString("banner"),"w780")));}
        return new Detail(resolved,a.optString("overview"),List.copyOf(ordered.values()),List.copyOf(genres),resolved.type(),year,resolved.status(),image(a.optString("banner"),"w1280"));
    }
    public List<Anime> related(Detail detail)throws Exception {return relations.get(detail.anime().url(),()->{detail(detail.anime());return List.of();});}
    public List<RecentEpisode> recent()throws Exception {
        JSONObject data=NextPage.find(document(endpoint+"/"),"episodes","animes");JSONArray array=data.getJSONArray("episodes");List<RecentEpisode> out=new ArrayList<>();Map<String,Anime> posters=new HashMap<>();JSONArray animes=data.optJSONArray("animes");if(animes!=null)for(int n=0;n<animes.length();n++){JSONObject a=animes.optJSONObject(n);if(a!=null&&a.has("name")&&a.has("slug")&&!a.optString("poster").isBlank())posters.put(a.getString("slug"),anime(a));}
        for(int n=0;n<array.length();n++){JSONObject e=array.getJSONObject(n);String slug=e.getString("anime_slug"),number=e.get("number").toString(),image=image(e.optString("anime_image"),"w780");Anime item=posters.getOrDefault(slug,new Anime(e.getString("anime_title"),endpoint+"/anime/"+slug,""));out.add(new RecentEpisode(item,new Episode("Episodio "+number,endpoint+"/ver/"+slug+"/"+number,image),e.optString("created_at")));}return List.copyOf(out);
    }
    public List<CalendarEntry> calendar()throws Exception {
        JSONArray array=NextPage.find(document(endpoint+"/calendario"),"initialData").getJSONArray("initialData");List<CalendarEntry> out=new ArrayList<>();
        for(int n=0;n<array.length();n++){JSONObject a=array.getJSONObject(n);int day=a.optInt("broadcast",0);if(day<1||day>7)continue;String number=a.optString("last_episode_number"),label=number.isBlank()?"":"Último · Episodio "+number;out.add(new CalendarEntry(anime(a),day,label,1440,a.optString("last_episode_at")));}return List.copyOf(out);
    }
    static String audio(String value){return switch(value){case "SUB"->"SUB";case "LAT"->"LAT";case "ESP"->"CAST";default->"";};}
    static boolean bridge(String url){HttpUrl u=HttpUrl.parse(url);return u!=null&&u.isHttps()&&u.host().equals("website.animelatinohd.com")&&u.encodedPath().startsWith("/v/");}
    public List<Server> servers(Episode episode)throws Exception{return servers.get(episode.url(),()->{
        JSONArray players=NextPage.find(document(episode.url()),"anime","players").getJSONArray("players");List<Server> out=new ArrayList<>();
        for(int n=0;n<players.length();n++){JSONObject p=players.getJSONObject(n);String name=p.optString("server_name"),url=p.optString("bridge_url"),language=audio(p.optString("language"));if(Set.of("Delta","Epsilon").contains(name)&&bridge(url)&&!language.isBlank()&&p.optBoolean("show_desktop",true))out.add(new Server(name,url,language));}return List.copyOf(out);
    });}
    static String embedded(String html,String bridge)throws Exception {
        for(Element iframe:Jsoup.parse(html,bridge).select("iframe[src]")){String url=iframe.absUrl("src");HttpUrl u=HttpUrl.parse(url);if(u==null||!u.isHttps())continue;String h=u.host();if(Set.of("byseraguci.com","bysekoze.com","luluvdoo.com","luluvdo.com","lulustream.com").contains(h))return url;}throw new IOException("AnimeLatinoHD: alojamiento del servidor no disponible");
    }
    public Stream resolve(Server server,String episode)throws Exception{return streams.get(server.url()+"|"+server.language(),()->{
        if(!bridge(server.url()))throw new IOException("AnimeLatinoHD: enlace de servidor inválido");String player=embedded(NativeStreams.get(server.url(),episode),server.url());String host=HttpUrl.get(player).host();
        return StreamExtractors.resolve(new Server(host.startsWith("byse")?"Byse":"Lulu",player,server.language()),server.url());
    });}
    public void invalidate(Server server,String episode){streams.remove(server.url()+"|"+server.language());}
}
