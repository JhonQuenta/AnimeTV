package pe.animetv;
import java.util.*;
import org.json.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static pe.animetv.Models.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class CatalogRegressionTest {
    @Test public void latanimeStatusDoesNotConfusePremiereWithCompleted(){assertEquals("Estreno",LatanimeSource.statusLabel(org.jsoup.Jsoup.parse("<button class='btn-estado estreno'>Estreno</button>")));assertEquals("Finalizado",LatanimeSource.statusLabel(org.jsoup.Jsoup.parse("<button class='btn-estado finalizado'>Finalizado</button>")));assertEquals("En emisión",LatanimeSource.statusLabel(org.jsoup.Jsoup.parse("<button class='btn-estado emision'>En emisión</button>")));assertEquals("",LatanimeSource.statusLabel(org.jsoup.Jsoup.parse("<button id='follow_d' class='btn-estado'>Seguir</button>")));}
    @Test public void renderedCatalogWinsOverUnfilteredFlightPayload()throws Exception {
        String stale=AnimeLatinoHdSourceTest.page(new JSONObject().put("current_page",1).put("data",new JSONArray().put(new JSONObject().put("name","Unrelated").put("slug","unrelated"))));
        String rendered="<!-- animetv-rendered-catalog -->"+stale+"<main><a href='/anime/sword-art-online'><img src='https://image.tmdb.org/t/p/w300/sao.jpg'><h3>Sword Art Online</h3><p>2012</p></a></main>";
        var source=new AnimeLatinoHdSource("https://www.animelatinohd.com",u->stale,u->rendered);
        assertEquals(List.of("Sword Art Online"),source.catalog("Sword Art Online",1,Map.of("status","0")).stream().map(Anime::title).toList());
        assertEquals("Finalizado",source.catalog("Sword Art Online",1,Map.of("status","0")).get(0).status());
    }
    @Test public void officialRelationsRetainSequelBeforeGenreSuggestions()throws Exception {
        JSONObject a=new JSONObject().put("name","Sword Art Online").put("slug","sword-art-online");
        JSONObject data=new JSONObject().put("anime",a).put("episodes",new JSONArray()).put("relations",new JSONObject().put("sequel",new JSONArray().put(new JSONObject().put("name","Sword Art Online II").put("slug","sword-art-online-2").put("poster","/two.jpg"))));
        var source=new AnimeLatinoHdSource("https://www.animelatinohd.com",u->AnimeLatinoHdSourceTest.page(data));Detail detail=source.detail(new Anime("SAO","https://www.animelatinohd.com/anime/sword-art-online",""));
        assertEquals("Sword Art Online II",new Recommendations(source).load(detail).get(0).title());
    }
    @Test public void combinedSearchUsesMetadataAndPaginatesAfterFiltering()throws Exception {
        List<Anime> items=new ArrayList<>();for(int n=0;n<30;n++)items.add(new Anime("Sword Art Online "+n,"item"+n,""));
        Source source=new Source(){public String id(){return "test";}public String name(){return "Test";}public String baseUrl(){return "";}public List<Anime> catalog(String q,int p,Map<String,String> f){return List.of();}public List<Filter> filters(){return List.of();}public Detail detail(Anime a){return new Detail(a,"",List.of(),List.of(new Genre("Acción","accion")),"Serie",a.url().equals("item0")?"2014":"2012","Finalizado");}public List<Server> servers(Episode e){return List.of();}public Stream resolve(Server s,String e){return null;}public List<RecentEpisode> recent(){return List.of();}public List<CalendarEntry> calendar(){return List.of();}};
        List<Anime> filtered=SearchFilters.collect(source,p->p==1?items:List.of(),Map.of("fecha","2012","status","2","type[]","0","genre","accion"));assertEquals(29,filtered.size());assertEquals(24,SearchFilters.page(filtered,1).size());assertEquals(5,SearchFilters.page(filtered,2).size());assertEquals("item1",filtered.get(0).url());
        assertTrue(SearchFilters.collect(source,p->p==1?items:List.of(),Map.of("type[]","1")).isEmpty());
    }
    @Test public void artworkRequestsCarryHeadersRequiredByLatanime(){var request=ArtworkHttp.request("https://latanime.org/thumbs/imagen/a.webp?v=1.5");assertEquals(NativeStreams.UA,request.header("User-Agent"));assertEquals("https://latanime.org/",request.header("Referer"));}
    @Test public void namedInstallmentsAndDubSuffixesMatchWithoutConfusingOtherSeries(){assertTrue(Recommendations.sameFamily("Sword Art Online Extra Edition Latino","Sword Art Online"));assertTrue(Recommendations.sameFamily("Fullmetal Alchemist Brotherhood","Fullmetal Alchemist"));assertFalse(Recommendations.sameFamily("Sword Art Online","Sword Art Offline"));assertFalse(Recommendations.sameFamily("One Piece","One Punch Man"));}
}
