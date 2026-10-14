package pe.animetv;
import java.io.*;
import java.nio.charset.StandardCharsets;
import okhttp3.mockwebserver.*;
import org.json.*;
import org.jsoup.Jsoup;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class LatStreamsTest {
    private String fixture(String name)throws Exception{try(InputStream in=getClass().getResourceAsStream("/latanime/"+name+".html")){assertNotNull(in);return new String(in.readAllBytes(),StandardCharsets.UTF_8);}}
    @Test public void doodRequiresPassPathAndConstructsTokenStreamWithBrowserHeaders()throws Exception {
        try(MockWebServer server=new MockWebServer()){
            server.start();server.enqueue(new MockResponse().setBody("<script>$.get('/pass_md5/check/token123',function(data){});</script>"));server.enqueue(new MockResponse().setBody("https://cdn.example.test/video/"));
            var result=LatStreams.dood(server.url("/e/test").toString(),"https://latanime.org/ver/test");assertTrue(result.url().startsWith("https://cdn.example.test/video/"));assertTrue(result.url().contains("token=token123"));var first=server.takeRequest();assertEquals("iframe",first.getHeader("Sec-Fetch-Dest"));assertTrue(first.getHeader("Accept").contains("text/html"));assertEquals("/pass_md5/check/token123",server.takeRequest().getPath());
        }
    }
    @Test public void missingDoodDataFailsClearly()throws Exception{try{LatStreams.doodPath("Video not found");fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("eliminado"));}}
    @Test public void luluPackedDictionaryProducesRealHlsAndNoAdUrl()throws Exception{String direct=LatStreams.packedVideo(Jsoup.parse(fixture("lulu")));assertTrue(direct.startsWith("https://cdn"));assertTrue(direct.contains("master.m3u8"));}
    @Test public void mixdropSupportsProtocolRelativePackedStream()throws Exception {String url=LatStreams.mixdropUrl(fixture("mixdrop"));assertTrue(url.startsWith("https://"));assertTrue(url.contains("mxcontent.net"));assertTrue(url.contains(".mp4"));}
    @Test public void realDoodFixtureHasPassPath()throws Exception{assertTrue(LatStreams.doodPath(fixture("dood")).startsWith("/pass_md5/"));}
    @Test public void dnsFallbackParsesOnlySuccessfulIpAnswers()throws Exception{var addresses=LatanimeDns.addresses("latanime.org",new JSONObject("{\"Status\":0,\"Answer\":[{\"type\":5,\"data\":\"alias.invalid\"},{\"type\":1,\"data\":\"104.21.74.227\"}]}"));assertEquals(1,addresses.size());assertEquals("latanime.org",addresses.get(0).getHostName());}
    @Test public void dnsFailureIsNotReportedAsSuccessfulLookup()throws Exception{try{LatanimeDns.addresses("latanime.org",new JSONObject("{\"Status\":3}"));fail();}catch(java.net.UnknownHostException expected){assertEquals("latanime.org",expected.getMessage());}}
}
