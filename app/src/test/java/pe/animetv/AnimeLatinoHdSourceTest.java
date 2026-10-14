package pe.animetv;
import java.util.*;
import org.json.*;
import org.jsoup.Jsoup;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static pe.animetv.Models.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class AnimeLatinoHdSourceTest {
    @Test public void recentUsesRealPosterAndKeepsHorizontalArtForEpisode()throws Exception{
        JSONObject e=new JSONObject().put("anime_slug","tempal-item-no-chikara").put("anime_title","Tempal").put("number",1).put("anime_image","/horizontal.jpg");
        var s=source(new JSONObject().put("episodes",new JSONArray().put(e)).put("animes",new JSONArray().put(anime())));RecentEpisode recent=s.recent().get(0);assertTrue(recent.anime().image().contains("w500/fmAQ"));assertEquals("https://image.tmdb.org/t/p/w780/horizontal.jpg",recent.episode().image());assertNotEquals(recent.anime().image(),recent.episode().image());
        s=source(new JSONObject().put("episodes",new JSONArray().put(e)).put("animes",new JSONArray()));assertEquals("",s.recent().get(0).anime().image());
    }
    @Test public void bannerIsIndependentFromPosterAndEpisodes()throws Exception{
        var s=source(new JSONObject().put("anime",anime()).put("episodes",new JSONArray("[{\"number\":1}]")));Detail detail=s.detail(new Anime("Tempal","https://www.animelatinohd.com/anime/tempal-item-no-chikara",""));assertEquals("https://image.tmdb.org/t/p/w1280/745xqHbiUWwO51TM60MXRCR4Onm.jpg",detail.backdrop());assertNotEquals(detail.anime().image(),detail.backdrop());
    }
    private String requested;
    static String page(JSONObject data)throws Exception{return "<main></main><script>self.__next_f.push("+new JSONArray().put(1).put("5:"+new JSONArray().put("$").put("$L17").put(JSONObject.NULL).put(new JSONObject().put("data",data))+"\n")+");</script>";}
    private AnimeLatinoHdSource source(JSONObject data)throws Exception{String html=page(data);return new AnimeLatinoHdSource("https://www.animelatinohd.com",url->{requested=url;return html;});}
    private JSONObject anime()throws Exception{return new JSONObject("{\"name\":\"Tempal: Item no Chikara\",\"slug\":\"tempal-item-no-chikara\",\"poster\":\"/fmAQ2lOtLAoEU2p6wQ12wXbIgLM.jpg\",\"banner\":\"/745xqHbiUWwO51TM60MXRCR4Onm.jpg\",\"type\":\"TV\",\"status\":1,\"aired\":\"2026-10-02\",\"genres\":\"accion,fantasia\",\"overview\":\"Youngwoo juega a Satisfy\"}");}
    @Test public void flightDataIsDecodedAcrossSeparateScriptChunks()throws Exception {
        String value="5:[\"$\",\"$L17\",null,{\"data\":{\"anime\":{\"name\":\"A\"},\"episodes\":[]}}]\n";String first=new JSONArray().put(1).put(value.substring(0,35)).toString(),second=new JSONArray().put(1).put(value.substring(35)).toString();var d=Jsoup.parse("<script>self.__next_f.push("+first+");</script><script>self.__next_f.push("+second+");</script>");assertEquals("A",NextPage.find(d,"anime","episodes").getJSONObject("anime").getString("name"));
    }
    @Test public void malformedOrChangedDataFailsInsteadOfExecutingJavascript()throws Exception {try{NextPage.find(Jsoup.parse("<script>self.__next_f.push(fetch('https://other.test'))</script>"),"players");fail();}catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("faltan"));}}
    @Test public void catalogUsesVerifiedPaginationAndQueryParameters()throws Exception {
        var s=source(new JSONObject().put("current_page",2).put("last_page",85).put("data",new JSONArray().put(anime())));List<Anime> items=s.catalog("Tempal",2,Map.of("year","2026","genre","fantasia"));var url=okhttp3.HttpUrl.get(requested);assertEquals("2",url.queryParameter("page"));assertEquals("Tempal",url.queryParameter("search"));assertEquals("fantasia",url.queryParameter("genre"));assertEquals("2026",url.queryParameter("year"));assertEquals("Serie",items.get(0).type());assertTrue(items.get(0).image().contains("image.tmdb.org/t/p/w500/"));
    }
    @Test public void realEpisodeNumbersAreAscendingAndNotGeneratedFromCount()throws Exception {
        var s=source(new JSONObject().put("anime",anime()).put("episodes",new JSONArray("[{\"number\":3},{\"number\":1}]")));Detail d=s.detail(new Anime("Tempal","https://www.animelatinohd.com/anime/tempal-item-no-chikara",""));assertEquals(List.of("Episodio 1","Episodio 3"),d.episodes().stream().map(Episode::name).toList());assertEquals("2026",d.year());assertEquals("En emisión",d.status());assertEquals(2,d.genres().size());assertEquals("Youngwoo juega a Satisfy",d.description());assertTrue(d.episodes().get(1).url().endsWith("/3"));
    }
    @Test public void allThreeAudiosKeepSeparateDeltaAndEpsilonEntries()throws Exception {
        JSONArray players=new JSONArray();for(String audio:List.of("SUB","LAT","ESP"))for(String name:List.of("Delta","Epsilon"))players.put(new JSONObject().put("language",audio).put("server_name",name).put("show_desktop",true).put("bridge_url","https://website.animelatinohd.com/v/test/"+audio+name));players.put(new JSONObject().put("language","SUB").put("server_name","Gamma").put("bridge_url","https://website.animelatinohd.com/v/test/gamma"));
        List<Server> servers=source(new JSONObject().put("anime",anime()).put("players",players)).servers(new Episode("3","https://www.animelatinohd.com/ver/tempal/3"));assertEquals(6,servers.size());assertEquals(List.of("SUB","LAT","CAST"),servers.stream().map(Server::language).distinct().toList());assertNotEquals(servers.get(0).url(),servers.get(2).url());
    }
    @Test public void subtitleOnlyEpisodeDoesNotInventDubServers()throws Exception {JSONArray players=new JSONArray("[{\"language\":\"SUB\",\"server_name\":\"Delta\",\"bridge_url\":\"https://website.animelatinohd.com/v/old/token\"}]");assertEquals(List.of("SUB"),source(new JSONObject().put("anime",anime()).put("players",players)).servers(new Episode("26","https://www.animelatinohd.com/ver/azumanga/26")).stream().map(Server::language).toList());}
    @Test public void recentRetainsOriginalAnimeAndSpecificEpisode()throws Exception {JSONObject e=new JSONObject("{\"anime_title\":\"Tempal\",\"anime_slug\":\"tempal-item-no-chikara\",\"anime_image\":\"/banner.jpg\",\"number\":3,\"created_at\":\"2026-10-11T15:15:44Z\"}");RecentEpisode item=source(new JSONObject().put("episodes",new JSONArray().put(e)).put("animes",new JSONArray())).recent().get(0);assertTrue(item.anime().url().endsWith("/anime/tempal-item-no-chikara"));assertTrue(item.episode().url().endsWith("/ver/tempal-item-no-chikara/3"));assertFalse(item.published().isBlank());}
    @Test public void calendarUsesIsoBroadcastAndSkipsUnknownDays()throws Exception {JSONObject a=anime().put("broadcast","7").put("last_episode_number",3).put("last_episode_at","2026-10-11T15:15:44Z");List<CalendarEntry> rows=source(new JSONObject().put("initialData",new JSONArray().put(a).put(anime().put("broadcast",JSONObject.NULL)))).calendar();assertEquals(1,rows.size());assertEquals(7,rows.get(0).day());assertTrue(rows.get(0).label().contains("3"));}
    @Test public void bridgeAndEmbeddedHostsCannotBeSpoofed()throws Exception {assertFalse(AnimeLatinoHdSource.bridge("https://website.animelatinohd.com.attacker.test/v/x"));assertFalse(AnimeLatinoHdSource.bridge("http://website.animelatinohd.com/v/x"));assertEquals("https://byseraguci.com/e/3nl7k9n0yt7m",AnimeLatinoHdSource.embedded("<iframe src='https://byseraguci.com/e/3nl7k9n0yt7m'></iframe>","https://website.animelatinohd.com/v/x"));try{AnimeLatinoHdSource.embedded("<iframe src='https://advert.example/e/x'></iframe>","https://website.animelatinohd.com/v/x");fail();}catch(java.io.IOException expected){}}
    @Test public void sourceRoutesByOriginalUrlAndHasTwoDefaultOptions(){assertEquals("animelatinohd",Sources.forUrl("https://www.animelatinohd.com/anime/tempal",Sources.create("latanime")).id());assertEquals(List.of("Delta","Epsilon"),Sources.servers("animelatinohd"));}
    @Test public void cloudflareDetectionDoesNotTreatEveryForbiddenResponseAsChallenge(){assertTrue(ProtectedPages.challenge(403,"cf-chl challenge-platform"));assertFalse(ProtectedPages.challenge(403,"Acceso denegado"));assertFalse(ProtectedPages.allowed("https://other.test/"));assertTrue(ProtectedPages.allowed("https://www.animelatinohd.com/"));assertFalse(ProtectedPages.ready("<main>Just a moment</main>"));}
    @Test public void remotePointerClampsToTheBrowserSurface(){ProtectedPages.Pointer p=new ProtectedPages.Pointer(50,50);p.move(-100,300,120,80);assertEquals(0,p.x,0);assertEquals(79,p.y,0);p.move(500,-100,120,80);assertEquals(119,p.x,0);assertEquals(0,p.y,0);}
    @Test public void movieAfterRawFlightTextLoadsStatusAndEpisode()throws Exception {
        JSONObject movie=anime().put("name","Sword Art Online Movie: Ordinal Scale").put("slug","sword-art-online-movie-ordinal-scale").put("type","Movie").put("status",0).put("aired","2017-02-18");
        String raw="Descripción en español: película, año 2017.\nContiene dos líneas y un texto 5: que no es un registro.";
        String data=new JSONArray().put("$").put("$L17").put(JSONObject.NULL).put(new JSONObject().put("data",new JSONObject().put("anime",movie).put("episodes",new JSONArray("[{number:1}]")))).toString();
        String flight="16:T"+Integer.toHexString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length)+","+raw+"5:"+data+"\n";
        String html="<main></main><script>self.__next_f.push("+new JSONArray().put(1).put(flight)+");</script>";
        Source source=new AnimeLatinoHdSource("https://www.animelatinohd.com",url->html);Detail d=source.detail(new Anime("","https://www.animelatinohd.com/anime/sword-art-online-movie-ordinal-scale",""));
        assertEquals("Finalizado",source.metadata(d.anime()).status());assertEquals("Finalizado",d.status());assertEquals("Película",d.type());assertEquals("2017",d.year());assertEquals(1,d.episodes().size());assertTrue(d.episodes().get(0).url().endsWith("ordinal-scale/1"));
    }
    @Test public void rawFlightTextCanSpanScriptsAndContainFakeJson()throws Exception{
        String raw="Árbol\n7:{\"anime\":{},\"episodes\":[]}\n终";String flight="16:T"+Integer.toHexString(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8).length)+","+raw+"5:{\"anime\":{\"name\":\"correct\"},\"episodes\":[]}\n";
        int split=flight.indexOf("7:")+8;String html="<script>self.__next_f.push("+new JSONArray().put(1).put(flight.substring(0,split))+");</script><script>self.__next_f.push("+new JSONArray().put(1).put(flight.substring(split))+");</script>";
        assertEquals("correct",NextPage.find(Jsoup.parse(html),"anime","episodes").getJSONObject("anime").getString("name"));
    }
    @Test public void truncatedRawFlightTextFailsClearly()throws Exception{String html="<script>self.__next_f.push("+new JSONArray().put(1).put("16:Tfff,short")+");</script>";try{NextPage.records(Jsoup.parse(html));fail();}catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("incompleto"));}}
}
