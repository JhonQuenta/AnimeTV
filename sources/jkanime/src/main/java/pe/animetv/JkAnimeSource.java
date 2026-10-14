package pe.animetv;

import java.io.IOException;
import java.util.*;
import java.util.regex.*;
import okhttp3.*;
import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import static pe.animetv.Models.*;

/** Public catalogue and anonymous episode lookup; no remote JavaScript is executed. */
public final class JkAnimeSource implements Source {
    interface Pages {String get(String url)throws Exception;String episodes(String url,String token,String referer)throws Exception;}
    private static final class HttpPages implements Pages {
        private final OkHttpClient client=SourceNetwork.HTTP.newBuilder().cookieJar(new CookieJar(){
            private final List<Cookie> cookies=new ArrayList<>();
            public synchronized void saveFromResponse(HttpUrl url,List<Cookie> incoming){for(Cookie c:incoming){cookies.removeIf(old->old.name().equals(c.name())&&old.domain().equals(c.domain())&&old.path().equals(c.path()));cookies.add(c);}cookies.removeIf(c->c.expiresAt()<System.currentTimeMillis());while(cookies.size()>32)cookies.remove(0);}
            public synchronized List<Cookie> loadForRequest(HttpUrl url){return cookies.stream().filter(c->c.expiresAt()>System.currentTimeMillis()&&c.matches(url)).toList();}
        }).build();
        private String request(Request r)throws Exception{return SourceHttp.get(client,r,"JKAnime:");}
        public String get(String url)throws Exception{return request(new Request.Builder().url(url).header("User-Agent",NativeStreams.UA).header("Referer","https://jkanime.net/").build());}
        public String episodes(String url,String token,String referer)throws Exception{return request(new Request.Builder().url(url).header("User-Agent",NativeStreams.UA).header("Referer",referer).header("X-Requested-With","XMLHttpRequest").header("Accept","application/json").post(new FormBody.Builder().add("_token",token).build()).build());}
    }
    private final String endpoint;private final Pages pages;
    private final TimedCache<Detail> details=new TimedCache<>(40,300000),metadata=new TimedCache<>(64,300000);
    private final TimedCache<List<Anime>> relations=new TimedCache<>(40,300000),searches=new TimedCache<>(8,300000);
    private final TimedCache<List<Server>> serverCache=new TimedCache<>(24,30000);
    private final TimedCache<Stream> streams=new TimedCache<>(12,90000);
    private final TimedCache<List<Filter>> filterCache=new TimedCache<>(1,3600000);
    public JkAnimeSource(){this("https://jkanime.net",new HttpPages());}
    JkAnimeSource(String endpoint,Pages pages){this.endpoint=endpoint;this.pages=pages;}
    public List<String> defaultServers(){return List.of("Desu","Magi","Desuka","Voe","Streamwish","Mixdrop","MP4Upload","Filemoon","Doodstream","Mega");}
    public String id(){return "jkanime";}public String name(){return "JKAnime";}public String baseUrl(){return endpoint;}
    private Document document(String url)throws Exception{Document d=Jsoup.parse(pages.get(url),url);if(d.title().toLowerCase(Locale.ROOT).contains("just a moment"))throw new IOException("JKAnime requiere verificación web");return d;}
    private static String text(Element e,String css){Element found=e.selectFirst(css);return found==null?"":found.text();}
    private static String scripts(Document d){return d.select("script").stream().map(Element::data).collect(java.util.stream.Collectors.joining("\n"));}
    static String status(String raw){String s=SourceText.normalized(raw);return s.contains("concl")||s.contains("final")||s.equals("finished")?"Finalizado":s.contains("emisi")||s.equals("currently")?"En emisión":s.contains("estren")||s.contains("proxim")?"Próximamente":"";}
    private String canonical(String url){HttpUrl u=HttpUrl.parse(url);return u!=null&&u.host().equals(HttpUrl.get(endpoint).host())&&u.encodedPath().matches("/[^/]+/?")?u.newBuilder().query(null).fragment(null).build().toString():"";}
    static List<Anime> searchItems(Document d){List<Anime> out=new ArrayList<>();for(Element title:d.select(".anime__item__text h5 a[href]")){Element box=title.parent().parent().parent();Element pic=box.selectFirst("[data-setbg]");List<Element> labels=box.select(".anime__item__text li");out.add(new Anime(title.text(),title.absUrl("href"),pic==null?"":pic.absUrl("data-setbg"),labels.size()>1?labels.get(1).text():"",labels.isEmpty()?"":status(labels.get(0).text())));}return List.copyOf(out);}
    private final TimedCache<List<Anime>> listingCache=new TimedCache<>(16,120000);
    public List<Anime> catalog(String query,int page,Map<String,String> filters)throws Exception {
        Map<String,String> selected=Map.copyOf(filters);String key=query.trim()+"|"+page+"|"+new TreeMap<>(selected);
        return listingCache.get(key,()->catalogUncached(query,page,selected));
    }
    private List<Anime> catalogUncached(String query,int page,Map<String,String> filters)throws Exception{
        if(page<1)return List.of();
        if(!query.isBlank()){
            if(filters.values().stream().anyMatch(v->!v.isBlank()))return SearchFilters.page(searches.get(query+new TreeMap<>(filters),()->SearchFilters.collect(this,n->catalog(query,n,Map.of()),filters)),page);
            // The site's search returns its complete result set, without a pagination endpoint.
            return SearchFilters.page(searches.get("plain|"+query,()->searchItems(document(HttpUrl.get(endpoint+"/buscar").newBuilder().addQueryParameter("q",query).build().toString()))),page);
        }
        HttpUrl.Builder url=HttpUrl.get(endpoint+"/directorio").newBuilder().addQueryParameter("p",String.valueOf(page));
        Set<String> allowed=Set.of("genero","tipo","estado","fecha","letra","orden");for(var f:filters.entrySet()){String key=f.getKey().equals("genre")?"genero":f.getKey();if(allowed.contains(key)&&!f.getValue().isBlank())url.addQueryParameter(key,f.getValue());}
        JSONObject data=new JSONObject("{"+JsData.object(scripts(document(url.build().toString())),"animes")+"}");if(page>data.optInt("last_page",1))return List.of();JSONArray items=data.optJSONArray("data");if(items==null)throw new IOException("JKAnime: catálogo sin datos");List<Anime> out=new ArrayList<>();for(int n=0;n<items.length();n++){JSONObject a=items.getJSONObject(n);String link=canonical(a.optString("url",endpoint+"/"+a.optString("slug")+"/"));if(!link.isBlank())out.add(new Anime(a.optString("title"),link,a.optString("image"),a.optString("tipo"),status(a.optString("estado",a.optString("status")))));}return List.copyOf(out);
    }
    public List<Filter> filters()throws Exception{return filterCache.get("filters",()->{List<Filter> out=new ArrayList<>();for(Element select:document(endpoint+"/directorio").select("select[name]")){String key=select.attr("name");String label=switch(key){case "genero"->"Género";case "tipo"->"Tipo";case "estado"->"Estado";case "fecha"->"Año";case "letra"->"Letra";default->"";};if(label.isBlank())continue;List<String> values=new ArrayList<>(List.of("")),labels=new ArrayList<>(List.of("Todos"));for(Element option:select.select("option")){String value=option.hasAttr("value")?option.attr("value"):option.text();if(value.isBlank()||option.hasAttr("disabled")||option.hasAttr("selected")&&option.elementSiblingIndex()==0)continue;values.add(value);labels.add(option.text());}out.add(new Filter(key.equals("genero")?"genre":key,label,List.copyOf(values),List.copyOf(labels)));}return List.copyOf(out);});}
    private static String field(Document d,String label){for(Element li:d.select(".anime_data li")){Element span=li.selectFirst("span");if(span!=null&&SourceText.normalized(span.text()).startsWith(label)){Element copy=li.clone();copy.select("span").remove();return copy.text();}}return "";}
    private Detail parseMetadata(Anime anime,Document d)throws Exception{
        String title=text(d,".anime__details__content h3");if(title.isBlank())throw new IOException("JKAnime: ficha no disponible");Element cover=d.selectFirst(".anime_pic img");String type=field(d,"tipo"),state=status(field(d,"estado"));Matcher year=Pattern.compile("\\b(?:19|20)\\d{2}\\b").matcher(field(d,"emitido"));LinkedHashMap<String,Genre> genres=new LinkedHashMap<>();for(Element a:d.select(".anime_data a[href*=/genero/]")){HttpUrl u=HttpUrl.parse(a.absUrl("href"));if(u!=null&&u.pathSegments().size()>1){String slug=u.pathSegments().get(1);genres.putIfAbsent(slug,new Genre(a.text(),slug));}}
        List<Anime> related=new ArrayList<>();for(Element heading:d.select("h3"))if(heading.text().equalsIgnoreCase("Temporadas y relacionados")){for(Element a:heading.parent().select("a[href]")){String url=canonical(a.absUrl("href"));Element img=a.selectFirst("img");if(!url.isBlank()&&!url.equals(anime.url()))related.add(new Anime(a.text(),url,img==null?"":img.absUrl("src")));}}relations.get(anime.url(),()->List.copyOf(related));
        return new Detail(new Anime(title,anime.url(),cover==null?anime.image():cover.absUrl("src"),type,state),text(d,"p.scroll"),List.of(),List.copyOf(genres.values()),type,year.find()?year.group():"",state);
    }
    public Detail metadata(Anime anime)throws Exception{return metadata.get(anime.url(),()->parseMetadata(anime,document(anime.url())));}
    public boolean matchesFilters(Detail d,Map<String,String> f){Map<String,String> translated=new HashMap<>(f);String type=translated.remove("tipo"),state=translated.remove("estado");if(type!=null)translated.put("type[]",switch(type){case "animes"->"Serie";case "peliculas"->"Película";case "ovas"->"OVA";case "onas"->"ONA";case "especiales"->"Especial";default->type;});if(state!=null)translated.put("status",switch(state){case "emision"->"En emisión";case "finalizados"->"Finalizado";case "estrenos"->"Próximamente";default->state;});return SearchFilters.matches(d,translated);}
    private JSONObject episodePage(String id,int page,String token,String referer)throws Exception {
        JSONObject data=new JSONObject(pages.episodes(endpoint+"/ajax/episodes/"+id+"/"+page,token,referer));
        if(data.optInt("current_page")!=page)throw new IOException("JKAnime: paginación de episodios inconsistente");
        return data;
    }
    public Detail detail(Anime anime)throws Exception{return details.get(anime.url(),()->{
        Document d=document(anime.url());Detail info=parseMetadata(anime,d);metadata.get(anime.url(),()->info);
        Element id=d.selectFirst(".guardar_anime[data-anime]"),token=d.selectFirst("meta[name=csrf-token]");
        if(id==null||token==null)throw new IOException("JKAnime: faltan datos de episodios");
        String number=id.attr("data-anime");if(!number.matches("\\d+"))throw new IOException("JKAnime: identificador inválido");
        JSONObject first=episodePage(number,1,token.attr("content"),anime.url());int last=first.optInt("last_page",1),total=first.optInt("total",-1);
        if(last<1||last>200)throw new IOException("JKAnime: demasiadas páginas de episodios");
        List<JSONObject> all=new ArrayList<>(List.of(first));List<Integer> remaining=new ArrayList<>();for(int page=2;page<=last;page++)remaining.add(page);
        for(SourceWork.Result<JSONObject> result:SourceWork.map(remaining,page->episodePage(number,page,token.attr("content"),anime.url()))){if(result.failure()!=null)throw result.failure();all.add(result.value());}
        TreeMap<Double,Episode> episodes=new TreeMap<>();
        for(JSONObject data:all){
            if(data.optInt("last_page",1)!=last)throw new IOException("JKAnime: el listado cambió durante la carga; reintenta");
            JSONArray items=data.getJSONArray("data");for(int n=0;n<items.length();n++){
                JSONObject e=items.getJSONObject(n);String value=e.get("number").toString();
                if(!value.matches("\\d+(?:\\.\\d+)?"))throw new IOException("JKAnime: número de episodio inválido");
                String image=e.optString("image"),thumb=image.matches("[A-Za-z0-9_.-]+")?"https://cdn.jkdesa.com/assets/images/animes/video/image/"+image:"";
                episodes.put(Double.parseDouble(value),new Episode("Episodio "+value,anime.url().replaceAll("/+$","")+"/"+value+"/",thumb));
            }
        }
        if(total>=0&&episodes.size()!=total)throw new IOException("JKAnime: listado de episodios incompleto");
        return new Detail(info.anime(),info.description(),List.copyOf(episodes.values()),info.genres(),info.type(),info.year(),info.status());
    });}
    public List<Anime> related(Detail d)throws Exception{metadata(d.anime());List<Anime> out=new ArrayList<>();List<Anime> known=relations.get(d.anime().url(),List::of);known=known.subList(0,Math.min(12,known.size()));List<SourceWork.Result<Detail>> loaded=SourceWork.map(known,this::metadata);for(int n=0;n<known.size();n++)out.add(loaded.get(n).failure()==null?loaded.get(n).value().anime():known.get(n));return List.copyOf(out);}
    public List<RecentEpisode> recent()throws Exception{Document d=document(endpoint+"/");LinkedHashMap<String,RecentEpisode> out=new LinkedHashMap<>();for(Element card:d.select(".dir1 .card")){Element a=card.selectFirst("a[href]"),img=card.selectFirst("img");if(a==null||img==null)continue;HttpUrl u=HttpUrl.parse(a.absUrl("href"));if(u==null)continue;Matcher m=Pattern.compile("/([^/]+)/(\\d+(?:\\.\\d+)?)/?").matcher(u.encodedPath());if(!m.matches())continue;List<Element> badges=card.select(".badge");String title=text(card,"h5");out.put(u.toString(),new RecentEpisode(new Anime(title,endpoint+"/"+m.group(1)+"/",img.absUrl("data-animepic"),"Serie",""),new Episode("Episodio "+m.group(2),u.toString(),img.absUrl("src")),badges.size()>1?badges.get(1).text():""));}return List.copyOf(out.values());}
    public List<CalendarEntry> calendar()throws Exception{List<CalendarEntry> out=new ArrayList<>();int day=0;for(Element box:document(endpoint+"/horario").select(".box.semana")){if(++day>7)break;for(Element card:box.select(".cajas > .box.img")){Element link=card.selectFirst("a[href]"),img=card.selectFirst("img");if(link==null||img==null)continue;String url=canonical(link.absUrl("href"));if(!url.isBlank())out.add(new CalendarEntry(new Anime(card.attr("title"),url,img.absUrl("src"),"Serie",status(text(card,".finished_anime"))),day,"",1440,text(card,"time")));}}return List.copyOf(out);}
    static String language(String value){String v=SourceText.normalized(value);return v.contains("castell")?"CAST":v.contains("latino")?"LAT":"SUB";}
    static List<Server> parseServers(Document d)throws Exception{
        String script=scripts(d);Map<Integer,String> videos=new HashMap<>();Matcher video=Pattern.compile("video\\[(\\d+)]\\s*=\\s*'([^'\\r\\n]+)'").matcher(script);while(video.find()){Element iframe=Jsoup.parse(video.group(2)).selectFirst("iframe[src]");if(iframe!=null)videos.put(Integer.parseInt(video.group(1)),iframe.attr("src"));}
        Map<String,String> langs=new HashMap<>();for(Element option:d.select("#deflang option"))langs.put(option.attr("value"),language(option.text()));List<Server> out=new ArrayList<>();
        for(Element button:d.select(".bg-servers a[data-id]")){String name=button.text().trim();if(!Set.of("Desu","Magi","Desuka").contains(name))continue;String url=videos.get(Integer.parseInt(button.attr("data-id")));HttpUrl u=url==null?null:HttpUrl.parse(url);if(u!=null&&u.isHttps()&&u.host().equals("jkanime.net")&&u.encodedPath().startsWith("/jkplayer/")){String lang="SUB";for(String cls:button.classNames())if(cls.startsWith("lg_"))lang=langs.getOrDefault(cls.substring(3),"SUB");out.add(new Server(name,url,lang));}}
        JSONArray external=new JSONArray("["+JsData.array(script,"servers")+"]");for(int n=0;n<external.length();n++){JSONObject e=external.getJSONObject(n);String name=switch(e.optString("server").toLowerCase(Locale.ROOT)){case "voe"->"Voe";case "mp4upload"->"MP4Upload";case "mixdrop"->"Mixdrop";case "streamwish"->"Streamwish";case "mega"->"Mega";case "filemoon"->"Filemoon";case "doodstream"->"Doodstream";default->"";};if(name.isBlank())continue;String url;try{url=new String(Base64.getDecoder().decode(e.optString("remote")),java.nio.charset.StandardCharsets.UTF_8).trim();}catch(IllegalArgumentException bad){continue;}HttpUrl u=HttpUrl.parse(url);if(u!=null&&u.isHttps())out.add(new Server(name,url,langs.getOrDefault(e.optString("lang"),"SUB")));}return List.copyOf(out);
    }
    public List<Server> servers(Episode episode)throws Exception{return serverCache.get(episode.url(),()->parseServers(document(episode.url())));}
    public Stream resolve(Server s,String episode)throws Exception{return streams.get(s.url()+"|"+episode,()->StreamExtractors.resolve(s,episode));}
    public void invalidate(Server s,String episode){streams.remove(s.url()+"|"+episode);serverCache.remove(episode);}
}
