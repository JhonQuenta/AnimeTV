package pe.animetv;

import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
import static pe.animetv.Models.*;

public class RecommendationsTest {
    private static Anime anime(String title,String url){return new Anime(title,url,"");}
    private static Detail detail(String title){return new Detail(anime(title,"current"),"",List.of(),List.of(new Genre("Acción","action")),"TV Anime","","Finalizado");}
    private static class FakeSource implements Source {
        int calls;boolean failSearch,failGenre;final Map<Integer,List<Anime>> pages=new HashMap<>();List<Anime> genre=List.of();
        public synchronized List<Anime> catalog(String query,int page,Map<String,String> filters)throws Exception {
            calls++;if(query.isBlank()){if(failGenre)throw new java.io.IOException("genre unavailable");return genre;}
            assertEquals("sword art online",query);assertTrue(filters.isEmpty());if(failSearch)throw new java.io.IOException("search unavailable");return pages.getOrDefault(page,List.of());
        }
        public String id(){return "test";}public String name(){return "Test";}public String baseUrl(){return "https://example.org";}
        public List<Filter> filters(){return List.of();}public Detail detail(Anime a){return null;}public List<Server> servers(Episode e){return List.of();}public Stream resolve(Server s,String e){return null;}public List<RecentEpisode> recent(){return List.of();}public List<CalendarEntry> calendar(){return List.of();}
    }
    @Test public void familyTitlesComeBeforeGenreFillersAcrossSearchPages()throws Exception {
        FakeSource source=new FakeSource();
        source.pages.put(1,List.of(anime("Sword Art Online II","current"),anime("Sword Art Online: Alicization","sequel"),anime("Unrelated Art Online","other")));
        source.pages.put(2,List.of(anime("Sword Art Online Movie: Ordinal Scale","movie"),anime("Sword Art Online Alternative: Gun Gale Online","spin")));
        source.genre=List.of(anime("Naruto","filler"),anime("Sword Art Online Movie: Ordinal Scale","movie"),anime("Sword Art Online OVA","ova"));
        Recommendations loader=new Recommendations(source);List<Anime> result=loader.load(detail("Sword Art Online II"));
        assertEquals(List.of("sequel","movie","spin","ova","filler"),result.stream().map(Anime::url).toList());
        int calls=source.calls;assertEquals(result,loader.load(detail("Sword Art Online II")));assertEquals(calls,source.calls);
    }
    @Test public void oneUnavailableListDoesNotDiscardTheOther()throws Exception {
        FakeSource source=new FakeSource();source.failSearch=true;source.genre=List.of(anime("Naruto","filler"));assertEquals("filler",new Recommendations(source).load(detail("Sword Art Online")).get(0).url());
        source=new FakeSource();source.failGenre=true;source.pages.put(1,List.of(anime("Sword Art Online III","sequel")));assertEquals("sequel",new Recommendations(source).load(detail("Sword Art Online")).get(0).url());
    }
    @Test public void titleNormalizationPreservesDifferentFranchises() {
        assertEquals("sword art online",Recommendations.family("Sword Art Online 2nd Season"));
        assertTrue(Recommendations.sameFamily("Sword Art Online: Alicization","Sword Art Online Alternative: Gun Gale Online"));
        assertTrue(Recommendations.sameFamily("Naruto Shippuuden","Naruto Movie 01: Test"));
        assertFalse(Recommendations.sameFamily("Sword Art Online","Sword Art Offline"));
        assertFalse(Recommendations.sameFamily("One Piece","One Punch Man"));
    }
    @Test public void duplicatePagesStopAndRecommendationCountIsBounded()throws Exception {
        FakeSource source=new FakeSource();source.pages.put(1,List.of(anime("Sword Art Online Movie","movie")));source.pages.put(2,source.pages.get(1));
        List<Anime> fillers=new ArrayList<>();for(int n=0;n<20;n++)fillers.add(anime("Other "+n,"filler"+n));source.genre=fillers;
        List<Anime> result=new Recommendations(source).load(detail("Sword Art Online"));assertEquals(10,result.size());assertEquals("movie",result.get(0).url());assertEquals(3,source.calls);
    }
    @Test public void explicitSagaIsPublishedBeforeSlowFillersComplete()throws Exception{
        java.util.concurrent.CountDownLatch started=new java.util.concurrent.CountDownLatch(1),release=new java.util.concurrent.CountDownLatch(1);java.util.concurrent.atomic.AtomicBoolean published=new java.util.concurrent.atomic.AtomicBoolean();
        FakeSource source=new FakeSource(){public List<Anime> catalog(String query,int page,Map<String,String> f)throws Exception{if(query.isBlank()){started.countDown();assertTrue(release.await(3,java.util.concurrent.TimeUnit.SECONDS));return List.of(anime("Naruto","filler"));}return List.of();}public List<Anime> related(Detail d)throws Exception{assertTrue(started.await(3,java.util.concurrent.TimeUnit.SECONDS));return List.of(anime("Sword Art Online III","native-sequel"));}};
        List<Anime> result=new Recommendations(source).load(detail("Sword Art Online"),partial->{assertEquals("native-sequel",partial.get(0).url());published.set(true);release.countDown();});assertTrue(published.get());assertEquals(List.of("native-sequel","filler"),result.stream().map(Anime::url).toList());
    }
}
