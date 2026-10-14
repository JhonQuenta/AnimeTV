package pe.animetv;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class SourcesTest {
    @Test public void latanimeRoutesHistoryToItsOriginalProviderAndHasIndependentDefault(){assertEquals("latanime",Sources.forUrl("https://latanime.org/anime/frieren-latino",Sources.create("tioanime")).id());assertEquals("Latanime",Sources.nameForUrl("https://latanime.org/anime/frieren-latino",Sources.create("animeav1")));assertTrue(Sources.ids().contains("latanime"));Store store=new Store(RuntimeEnvironment.getApplication());store.serverDefault("latanime","Lulu");store.serverDefault("tioanime","Voe");assertEquals("Lulu",store.serverDefault("latanime"));assertEquals("Voe",store.serverDefault("tioanime"));}
    @Test public void favoritesAndPlaybackResolveOriginalProviderAfterSwitch(){
        assertEquals("animeav1",Sources.forUrl("https://animeav1.com/media/test",Sources.create("tioanime")).id());assertEquals("tioanime",Sources.forUrl("https://tioanime.com/anime/test",Sources.create("animeav1")).id());assertSame(Sources.create("tioanime"),Sources.create("tioanime"));
    }
    @Test public void sourcePreferenceSurvivesNewStoreInstance(){Store first=new Store(RuntimeEnvironment.getApplication());first.source("tioanime");assertEquals("tioanime",new Store(RuntimeEnvironment.getApplication()).source());first.source("animeav1");}
    @Test public void serverDefaultsAreIndependentAndMigrateManualMode(){Store store=new Store(RuntimeEnvironment.getApplication());store.automatic(false);assertEquals("ask",store.serverDefault("tioanime"));store.serverDefault("animeav1","Voe");store.serverDefault("tioanime","YourUpload");assertEquals("Voe",new Store(RuntimeEnvironment.getApplication()).serverDefault("animeav1"));assertEquals("YourUpload",new Store(RuntimeEnvironment.getApplication()).serverDefault("tioanime"));}
}
