package pe.animetv;
import java.util.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import okhttp3.mockwebserver.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static pe.animetv.Models.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class TioAnimeSourceTest {
    private MockWebServer server;private TioAnimeSource source;
    @Before public void start()throws Exception{server=new MockWebServer();server.start();source=new TioAnimeSource(server.url("/").toString().replaceAll("/$",""));}
    @After public void stop()throws Exception{server.shutdown();}
    private void fixture(String name)throws Exception{try(InputStream stream=getClass().getResourceAsStream("/tioanime/"+name+".html")){assertNotNull(stream);server.enqueue(new MockResponse().setBody(new String(stream.readAllBytes(),StandardCharsets.UTF_8)));}}
    @Test public void liveDetailHasFixedMetadataAndAscendingEpisodes()throws Exception{
        fixture("detail");Detail detail=source.detail(new Anime("dark",server.url("/anime/dark").toString(),""));
        assertEquals("Dark Summoner to Dekiteiru",detail.anime().title());assertEquals("Serie",detail.type());assertEquals("En emisión",detail.status());assertEquals("2026",detail.year());assertEquals(3,detail.genres().size());
        assertEquals(List.of("Episodio 1","Episodio 2"),detail.episodes().stream().map(Episode::name).toList());assertTrue(detail.episodes().get(1).url().endsWith("dark-summoner-to-dekiteiru-2"));assertTrue(detail.anime().image().endsWith("/portadas/4526.jpg"));
        assertTrue(detail.backdrop().endsWith("/uploads/fondos/4526.jpg"));assertNotEquals(detail.anime().image(),detail.backdrop());assertNotEquals(detail.episodes().get(0).image(),detail.backdrop());assertSame(detail,source.detail(detail.anime()));assertEquals(1,server.getRequestCount());
    }
    @Test public void recentLinksOpenAnimeAndRetainEpisodeToConfirm()throws Exception{
        fixture("home");RecentEpisode item=source.recent().get(0);assertEquals("Dark Summoner to Dekiteiru",item.anime().title());assertTrue(item.anime().url().endsWith("/anime/dark-summoner-to-dekiteiru"));assertTrue(item.episode().url().endsWith("-2"));assertTrue(item.anime().image().contains("/portadas/"));
    }
    @Test public void calendarUsesIsoWeekdaysAndTrimmedLinks()throws Exception{
        fixture("calendar");List<CalendarEntry> entries=source.calendar();assertTrue(entries.size()>10);assertTrue(entries.stream().anyMatch(e->e.day()==7&&e.anime().title().equals("Dark Summoner to Dekiteiru")));assertTrue(entries.stream().allMatch(e->e.day()>=1&&e.day()<=7&&e.anime().url().startsWith(server.url("/").toString())));
    }
    @Test public void upcomingCalendarResolvesCanonicalAnimeByCoverId()throws Exception{
        fixture("calendar");Anime upcoming=source.calendar().stream().filter(e->e.anime().title().equals("Dark Summoner to Dekiteiru")).findFirst().orElseThrow().anime();
        assertTrue(upcoming.url().contains("calendarCover=4526"));fixture("search");fixture("detail");Detail resolved=source.detail(upcoming);assertTrue(resolved.anime().url().endsWith("/anime/dark-summoner-to-dekiteiru"));assertEquals(2,resolved.episodes().size());assertEquals(3,server.getRequestCount());
    }
    @Test public void catalogQueryAndGenreUseSiteParameters()throws Exception{
        fixture("catalog");assertFalse(source.catalog("",2,Map.of("genre","fantasia","type[]","0")).isEmpty());var url=server.takeRequest().getRequestUrl();assertNull(url.queryParameter("q"));assertEquals("2",url.queryParameter("page"));assertEquals("fantasia",url.queryParameter("genero[]"));assertEquals("0",url.queryParameter("type[]"));
    }
    @Test public void filtersHaveAllOptionAndCacheResponses()throws Exception{
        fixture("catalog");List<Filter> filters=source.filters();assertEquals(4,filters.size());assertTrue(filters.stream().anyMatch(f->f.key().equals("genre")&&f.values().contains("fantasia")));assertTrue(filters.stream().allMatch(f->f.values().get(0).isEmpty()));assertSame(filters,source.filters());assertEquals(1,server.getRequestCount());
    }
    @Test public void liveEpisodeHasAllThreeNativeServersWithVoeFirst()throws Exception{
        fixture("episode");List<Server> servers=source.servers(new Episode("2",server.url("/ver/test-2").toString()));assertEquals(List.of("Voe","YourUpload","Mega"),servers.stream().map(Server::name).toList());assertTrue(servers.stream().allMatch(s->s.language().equals("SUB")));
    }
    @Test public void changedOrMissingVideoDataFailsClearly()throws Exception{
        server.enqueue(new MockResponse().setBody("<html><script>var advertisement=[];</script></html>"));try{source.servers(new Episode("2",server.url("/ver/missing-2").toString()));fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("videos"));}
    }
    @Test public void foreignHostCannotMasqueradeAsSupportedProvider()throws Exception{
        server.enqueue(new MockResponse().setBody("<script>var videos=[[\"Mega\",\"https://example.com/file/123\",0,0]];</script>"));assertTrue(source.servers(new Episode("1",server.url("/ver/foreign-1").toString())).isEmpty());
    }
}
