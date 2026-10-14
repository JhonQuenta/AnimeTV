package pe.animetv;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import okhttp3.mockwebserver.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static pe.animetv.Models.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class LatanimeSourceTest {
    private MockWebServer server;private LatanimeSource source;
    @Before public void start()throws Exception{server=new MockWebServer();server.start();source=new LatanimeSource(server.url("/").toString().replaceAll("/$",""));}
    @After public void stop()throws Exception{server.shutdown();}
    private String html(String name)throws Exception{try(InputStream in=getClass().getResourceAsStream("/latanime/"+name+".html")){assertNotNull(in);return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
    private void fixture(String name)throws Exception{server.enqueue(new MockResponse().setBody(html(name).replace("https://latanime.org",server.url("/").toString().replaceAll("/$",""))));}
    @Test public void catalogUsesActualPaginationAndLazyArtwork()throws Exception {
        fixture("catalog");List<Anime> items=source.catalog("",2,Map.of("genre","castellano","fecha","2024"));assertTrue(items.size()>15);assertEquals("Dragon Ball Super: Beerus",items.get(0).title());assertTrue(items.get(0).image().contains("/thumbs/imagen/"));assertEquals("2",server.takeRequest().getRequestUrl().queryParameter("p"));
    }
    @Test public void searchKeepsBothDubsSeparate()throws Exception {
        fixture("search");List<Anime> items=source.catalog("frieren",1,Map.of());assertEquals(1,items.size());assertTrue(items.get(0).url().contains("animetvAudio=all"));assertEquals("frieren",server.takeRequest().getRequestUrl().queryParameter("q"));
    }
    @Test public void filtersOfferAudioGenresAndDoNotIncludeFalsePlaceholder()throws Exception {
        fixture("catalog");List<Filter> filters=source.filters();assertEquals(4,filters.size());Filter genres=filters.stream().filter(f->f.key().equals("genre")).findFirst().orElseThrow();assertTrue(genres.values().containsAll(List.of("latino","castellano","romance")));assertFalse(genres.values().contains("false"));assertSame(filters,source.filters());
    }
    @Test public void detailUsesActualAscendingEpisodesAndDistinctThumbnails()throws Exception {
        fixture("detail");Detail d=source.detail(new Anime("",server.url("/anime/azul").toString(),""));assertEquals("La Caja Azul Castellano",d.anime().title());assertEquals(27,d.episodes().size());assertEquals("2024",d.year());assertEquals("Finalizado",d.status());assertEquals(6,d.genres().size());assertTrue(d.description().startsWith("Taiki"));assertEquals("Episodio 1",d.episodes().get(0).name());assertEquals("Episodio 27",d.episodes().get(26).name());assertNotEquals(d.episodes().get(0).image(),d.episodes().get(1).image());assertSame(d,source.detail(d.anime()));
    }
    @Test public void recentPreservesRequestedEpisodeAndCanonicalAnimeLink()throws Exception {
        fixture("home");List<RecentEpisode> recent=source.recent();RecentEpisode r=recent.stream().filter(e->e.episode().url().contains("la-caja-azul-episodio-27")).findFirst().orElseThrow();assertEquals("La Caja Azul",r.anime().title());assertTrue(r.anime().url().endsWith("/anime/la-caja-azul?animetvAudio=all"));assertEquals("Episodio 27",r.episode().name());assertEquals(1,recent.stream().filter(e->e.episode().url().contains("la-caja-azul-episodio-27")).count());assertFalse(r.published().isBlank());
    }
    @Test public void calendarUsesSiteWeekdaysAndCanonicalLinks()throws Exception {
        fixture("calendar");List<CalendarEntry> entries=source.calendar();assertTrue(entries.size()>10);assertTrue(entries.stream().allMatch(e->e.day()>=1&&e.day()<=7&&e.anime().url().contains("/anime/")));assertTrue(entries.stream().anyMatch(e->e.anime().title().equals("La Caja Azul")));
    }
    @Test public void newEpisodesUseExistingAndNewNativeHosts()throws Exception {
        fixture("episode-new");List<Server> items=source.servers(new Episode("27",server.url("/ver/azul-castellano-episodio-27").toString()));assertEquals(List.of("Dsvplay","Byse","Mega","Mixdrop","Voe","MP4Upload"),items.stream().map(Server::name).toList());assertTrue(items.stream().allMatch(s->s.language().equals("DUB")));
    }
    @Test public void oldEpisodesRetainPriorityHostsAndExcludeParkedListeamed()throws Exception {
        fixture("episode-old");List<Server> items=source.servers(new Episode("1",server.url("/ver/frieren-latino-episodio-1").toString()));assertEquals(List.of("Filemoon","Mega","Vide0","Lulu","Mixdrop","Voe","MP4Upload"),items.stream().map(Server::name).toList());
    }
    @Test public void namesComeFromHostAndCannotBeSpoofed()throws Exception {
        assertEquals("Voe",LatanimeSource.serverName("https://voe.sx/e/x"));assertEquals("",LatanimeSource.serverName("https://voe.sx.attacker.test/e/x"));assertEquals("",LatanimeSource.serverName("http://voe.sx/e/x"));
        String encoded=Base64.getEncoder().encodeToString("https://example.com/ad".getBytes(StandardCharsets.UTF_8));server.enqueue(new MockResponse().setBody("<a class='play-video' data-player='"+encoded+"'>voe</a>"));assertTrue(source.servers(new Episode("",server.url("/ver/spoof").toString())).isEmpty());
    }
    @Test public void malformedBase64DoesNotExecuteOrPreventOtherServers()throws Exception {
        server.enqueue(new MockResponse().setBody("<a class='play-video' data-player='%%%%'>x</a><a class='play-video' data-player='aHR0cHM6Ly92b2Uuc3gvZS94'>ok</a>"));assertEquals("Voe",source.servers(new Episode("",server.url("/ver/malformed").toString())).get(0).name());
    }
    @Test public void missingEpisodeMarkupFailsClearly()throws Exception {server.enqueue(new MockResponse().setBody("<html>Not found</html>"));try{source.servers(new Episode("",server.url("/ver/absent").toString()));fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("faltan"));}}
    @Test public void groupedCardStatusDoesNotProbeOtherAudioOrRequireEpisodes()throws Exception {
        fixture("catalog");Anime anime=source.catalog("",1,Map.of()).get(0);
        String html="<div class='serieimgficha'><img src='/cover.jpg'><button class='btn-estado estreno'>Estreno</button></div><div class='col-lg-9 col-md-8'><h2>Nuevo anime Latino</h2><span class='span-tiempo'>2026</span></div>";
        server.enqueue(new okhttp3.mockwebserver.MockResponse().setBody(html));Detail d=source.metadata(anime);
        assertEquals("Estreno",d.status());assertTrue(d.episodes().isEmpty());assertTrue(d.anime().url().contains("animetvAudio=all"));assertSame(d,source.metadata(anime));assertEquals(2,server.getRequestCount());
    }
}
