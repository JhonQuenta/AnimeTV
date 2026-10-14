package pe.animetv;

import org.junit.*;
import static org.junit.Assert.*;
import okhttp3.mockwebserver.*;
import java.util.*;
import static pe.animetv.Models.*;

public class AnimeAv1SourceTest {
    private MockWebServer server;
    private AnimeAv1Source source;
    @Before public void before() throws Exception { server=new MockWebServer(); server.start(); source=new AnimeAv1Source(server.url("/").toString().replaceAll("/$","")); }
    @After public void after() throws Exception { server.shutdown(); }
    @Test public void backgroundBelongsToAnimeAndNeverUsesEpisodeOrRelatedArt()throws Exception{
        server.enqueue(new MockResponse().setBody("<main><img src='/covers/42.jpg'><img src='/screenshots/42/1.jpg'><img src='/backdrops/42.jpg' alt='Sample Backdrop'><script>episodes:[{number:1}]</script></main>"));
        Detail detail=source.detail(new Anime("Sample",server.url("/media/sample").toString(),""));assertTrue(detail.anime().image().endsWith("/covers/42.jpg"));assertTrue(detail.backdrop().endsWith("/backdrops/42.jpg"));
        server.enqueue(new MockResponse().setBody("<main><img src='/covers/43.jpg'><img src='/screenshots/43/1.jpg'><img src='/backdrops/42.jpg' alt='Sample Backdrop'><script>episodes:[{number:1}]</script></main>"));assertEquals("",source.detail(new Anime("Other",server.url("/media/other").toString(),"")).backdrop());
    }
    @Test public void repeatedDetailAndFiltersReuseSuccessfulResponses() throws Exception {
        server.enqueue(new MockResponse().setBody("<main><header><span>2026</span></header><script>episodes:[{number:1}]</script></main>"));
        Anime anime=new Anime("Prueba",server.url("/media/test").toString(),"");
        Detail first=source.detail(anime);assertSame(first,source.detail(anime));assertEquals(1,first.episodes().size());assertEquals(1,server.getRequestCount());
        server.enqueue(new MockResponse().setBody("<select name='status'><option value=''>Todos</option></select>"));
        assertSame(source.filters(),source.filters());assertEquals(2,server.getRequestCount());
    }
    @Test public void failedDetailCanBeRetriedWithoutWaitingForCacheExpiry() throws Exception {
        Anime anime=new Anime("Prueba",server.url("/media/retry").toString(),"");server.enqueue(new MockResponse().setResponseCode(503));
        try{source.detail(anime);fail("Expected source failure");}catch(java.io.IOException expected){}
        server.enqueue(new MockResponse().setBody("<main><script>episodes:[{number:2}]</script></main>"));assertEquals(1,source.detail(anime).episodes().size());assertEquals(2,server.getRequestCount());
    }
    @Test public void searchDoesNotTurnUserTextIntoQueryParameters() throws Exception {
        server.enqueue(new MockResponse().setBody("<article><a href='/media/sample'><h3>Serie de prueba</h3><img src='/cover.webp'></a></article>"));
        List<Anime> result=source.catalog("A & B?status=other",1,Map.of());
        assertEquals(1,result.size()); assertEquals("Serie de prueba",result.get(0).title());
        RecordedRequest request=server.takeRequest();
        assertEquals("A & B?status=other",request.getRequestUrl().queryParameter("search"));
        assertNull(request.getRequestUrl().queryParameter("status"));
        assertTrue(result.get(0).image().endsWith("/cover.webp"));
    }
    @Test public void sourceFailureIsNotReportedAsEmptyCatalog() throws Exception {
        server.enqueue(new MockResponse().setResponseCode(503));
        try { source.catalog("",1,Map.of()); fail("Expected an HTTP error"); } catch(java.io.IOException error) { assertTrue(error.getMessage().contains("503")); }
    }
    @Test public void removedProvidersDoNotFetchTheirHost() throws Exception {
        for(String name:List.of("UPNShare","direct","clear")) {
            try {source.resolve(new Server(name,"https://example.org/embed","SUB"),"https://example.org/episode");fail("Removed provider must be rejected");}
            catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("no disponible"));}
        }
        assertEquals(0,server.getRequestCount());
    }
}
