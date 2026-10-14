package pe.animetv;
import java.io.IOException;import java.util.*;import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.RobolectricTestRunner;import org.robolectric.annotation.Config;import static org.junit.Assert.*;import static pe.animetv.Models.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class SourceModuleContractTest {
    @After public void reset(){SourcePageAccess.clear();}
    @Test public void stableIdentifiersAndDomainRoutingSurviveModuleSplit(){
        assertEquals(List.of("animeav1","tioanime","latanime","animelatinohd","jkanime"),Sources.ids());Source fallback=Sources.create("animeav1");for(String id:Sources.ids()){Source module=Sources.create(id);for(String host:module.domains()){assertSame(module,Sources.forUrl("https://"+host+"/anime/example",fallback));assertSame(module,Sources.forUrl("https://"+host+"/anime/example",null));}assertEquals(module.defaultServers(),Sources.servers(id));assertFalse(module.defaultServers().isEmpty());}assertSame(fallback,Sources.forUrl("https://jkanime.net.attacker.test/psyren",fallback));assertSame(fallback,Sources.create("missing"));
    }
    @Test public void precacheIsAnExplicitCapabilityAndKeepsPreviousBehaviour(){for(String id:Sources.ids())assertEquals(List.of("animeav1","tioanime","latanime").contains(id),Sources.create(id).supportsPreload());assertEquals(1,Source.API_VERSION);assertEquals(10000,SourceNetwork.HTTP.callTimeoutMillis());}
    @Test public void protectedPageContractDelegatesToHostWithoutItsUi(){
        List<String> calls=new ArrayList<>();SourcePageAccess.install(new SourcePageAccess.Host(){public String get(String url,boolean interactive){calls.add(url+":"+interactive);return "detail";}public String catalog(String url){calls.add(url+":catalog");return "catalog";}});
        try{assertEquals("detail",SourcePageAccess.get("https://www.animelatinohd.com/anime/test",false));assertEquals("catalog",SourcePageAccess.catalog("https://www.animelatinohd.com/directorio"));assertTrue(calls.get(0).endsWith(":false"));SourcePageAccess.clear();try{SourcePageAccess.get("https://www.animelatinohd.com/anime/test");fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("verificar"));}}catch(Exception unexpected){throw new AssertionError(unexpected);}
    }
    @Test public void sharedExtractorRejectsUnknownProtocolBeforeAnyDownload()throws Exception{try{StreamExtractors.resolve(new Server("Unsupported","https://example.test/video","SUB"),"https://example.test/episode");fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("Unsupported"));}}
}
