package pe.animetv;
import java.util.*;
import okhttp3.mockwebserver.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static pe.animetv.Models.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class LatanimeAudioTest {
    private MockWebServer server;private LatanimeSource source;private String castYear="2023";
    @Before public void start()throws Exception{
        server=new MockWebServer();server.setDispatcher(new Dispatcher(){public MockResponse dispatch(RecordedRequest request){
            String path=request.getRequestUrl().encodedPath();String host=server.url("/").toString().replaceAll("/$","");
            if(path.equals("/buscar"))return new MockResponse().setBody("<a href='"+host+"/anime/mi-serie-latino'><div class='series'><h3>Mi Serie Latino</h3></div></a><a href='"+host+"/anime/mi-serie-castellano'><div class='series'><h3>Mi Serie Castellano</h3></div></a>");
            if(path.equals("/anime/mi-serie-latino")||path.equals("/anime/mi-serie-castellano")){
                boolean latin=path.endsWith("latino");String slug=latin?"mi-serie-latino":"mi-serie-castellano";
                String body="<div class='col-lg-9 col-md-8'><h2>Mi Serie "+(latin?"Latino":"Castellano")+"</h2><span class='span-tiempo'>Estreno "+(latin?"2023":castYear)+"</span>";
                for(int number=1;number<=(latin?2:1);number++)body+="<a href='"+host+"/ver/"+slug+"-episodio-"+number+"'><div class='cap-layout'>Episodio "+number+"</div></a>";
                return new MockResponse().setBody(body+"</div>");
            }
            if(path.startsWith("/ver/")){String player=path.contains("latino")?"https://voe.sx/e/latin":"https://www.mp4upload.com/embed-cast.html";String base64=Base64.getEncoder().encodeToString(player.getBytes(java.nio.charset.StandardCharsets.UTF_8));return new MockResponse().setBody("<a class='play-video' data-player='"+base64+"'>server</a>");}
            return new MockResponse().setResponseCode(404);
        }});server.start();source=new LatanimeSource(server.url("/").toString().replaceAll("/$",""));
    }
    @After public void stop()throws Exception{server.shutdown();}
    @Test public void catalogDetailAndEpisodeServersGroupBothAudioVersions()throws Exception {
        List<Anime> catalog=source.catalog("Mi Serie",1,Map.of());assertEquals(1,catalog.size());Detail detail=source.detail(catalog.get(0));assertEquals("Mi Serie",detail.anime().title());assertEquals(2,detail.episodes().size());
        List<Server> first=source.servers(detail.episodes().get(0));assertEquals(List.of("LAT","CAST"),first.stream().map(Server::language).toList());assertEquals(List.of("Voe","MP4Upload"),first.stream().map(Server::name).toList());
        assertTrue(source.playbackEpisode(first.get(0),detail.episodes().get(0).url()).endsWith("mi-serie-latino-episodio-1"));assertTrue(source.playbackEpisode(first.get(1),detail.episodes().get(0).url()).endsWith("mi-serie-castellano-episodio-1"));
        assertEquals(List.of("LAT"),source.servers(detail.episodes().get(1)).stream().map(Server::language).toList());
    }
    @Test public void savedGroupCanRebuildWithoutCatalogOrInMemorySeeds()throws Exception {
        Anime group=source.catalog("Mi Serie",1,Map.of()).get(0);LatanimeSource restored=new LatanimeSource(source.baseUrl());Detail detail=restored.detail(group);assertEquals(2,detail.episodes().size());assertEquals(2,restored.servers(detail.episodes().get(0)).size());
        while(server.getRequestCount()>0){RecordedRequest request=server.takeRequest(20,java.util.concurrent.TimeUnit.MILLISECONDS);if(request==null)break;assertFalse(request.getPath().contains("animetvAudio"));}
    }
    @Test public void differentYearIsNotMergedAsTheSameSeason()throws Exception {castYear="2024";Detail detail=source.detail(source.catalog("Mi Serie",1,Map.of()).get(0));assertEquals(List.of("LAT"),source.servers(detail.episodes().get(0)).stream().map(Server::language).toList());}
    @Test public void seasonAndMovieIdentifiersArePreserved(){assertEquals("frieren-2",LatanimeSource.seriesKey("frieren-2-castellano"));assertNotEquals(LatanimeSource.seriesKey("frieren-2-latino"),LatanimeSource.seriesKey("frieren-latino"));assertEquals("sao-pelicula",LatanimeSource.seriesKey("sao-pelicula-latino"));}
    @Test public void legacyDubPreferenceIncludesBothButSpecificAudioDoesNot(){assertTrue(PlaybackLanguages.matches("LAT","DUB"));assertTrue(PlaybackLanguages.matches("CAST","DUB"));assertFalse(PlaybackLanguages.matches("CAST","LAT"));assertTrue(PlaybackLanguages.matches("DUB","LAT"));assertEquals("Castellano",PlaybackLanguages.label("CAST"));assertEquals("Voe",PlayerActivity.serverCaption(new Server("Voe","","LAT"),"latanime"));}
}
