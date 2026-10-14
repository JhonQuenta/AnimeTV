package pe.animetv;
import java.util.*;
import java.io.IOException;
import okhttp3.*;
import okhttp3.mockwebserver.*;
import org.junit.*;
import org.jsoup.Jsoup;
import static org.junit.Assert.*;
import static pe.animetv.Models.*;

public class ZillaStreamsTest {
 private static final String EMBED="https://player.zilla-networks.com/play/c2ecdb30cc72f4a5ad1249d6cd3cc2f4";
 private MockWebServer server;
 @Before public void setup()throws Exception{server=new MockWebServer();server.start();}
 @After public void teardown()throws Exception{server.shutdown();}
 private Stream stream(){return new Stream(server.url("/m3u8/id").toString(),server.url("/play/id").toString(),"application/x-mpegURL",Map.of("Sec-Fetch-Site","same-origin"));}
 private MockResponse manifest(){return new MockResponse().setBody("#EXTM3U\n#EXT-X-MAP:URI=\"/segs/id/init.html\"\n#EXTINF:19,\n/segs/id/000.html\n#EXT-X-ENDLIST\n");}
 @Test public void extensionlessLinkHasHlsMimeAndProviderFetchHeader()throws Exception{Stream s=ZillaStreams.descriptor(EMBED);assertTrue(s.url().contains("/m3u8/"));assertEquals("application/x-mpegURL",s.mimeType());assertEquals(EMBED,s.referer());assertEquals("same-origin",s.headers().get("Sec-Fetch-Site"));assertTrue(new Stream("https://other/video.mp4","https://other/").headers().isEmpty());}
 @Test public void resolverChecksInitializationAndSegmentWithSameHeaders()throws Exception{
  server.enqueue(manifest());server.enqueue(new MockResponse().setHeader("Content-Type","text/html").setBody("\u0000\u0000\u0000 ftypiso5"));server.enqueue(new MockResponse().setHeader("Content-Type","text/html").setBody("\u0000\u0000\u0000 stypmsdh"));
  Stream s=stream();ZillaStreams.validate(new OkHttpClient(),s);
  for(String path:List.of("/m3u8/id","/segs/id/init.html","/segs/id/000.html")){RecordedRequest r=server.takeRequest();assertEquals(path,r.getPath());assertEquals("same-origin",r.getHeader("Sec-Fetch-Site"));assertEquals(s.referer(),r.getHeader("Referer"));assertEquals(NativeStreams.UA,r.getHeader("User-Agent"));}
 }
 @Test public void initialization403IsDetectedBeforePlayback()throws Exception{server.enqueue(manifest());server.enqueue(new MockResponse().setResponseCode(403));try{ZillaStreams.validate(new OkHttpClient(),stream());fail();}catch(IOException e){assertTrue(e.getMessage().contains("403"));}assertEquals(2,server.getRequestCount());}
 @Test public void htmlErrorWith200IsNotAcceptedAsVideo()throws Exception{server.enqueue(manifest());server.enqueue(new MockResponse().setBody("<!DOCTYPE html><html>Error</html>"));try{ZillaStreams.validate(new OkHttpClient(),stream());fail();}catch(IOException e){assertTrue(e.getMessage().contains("no disponible"));}}
 @Test public void providerHeadersAreNotSentToForeignSegmentHost()throws Exception{server.enqueue(new MockResponse().setBody("#EXTM3U\n#EXTINF:10,\nhttps://foreign.example/segment\n"));try{ZillaStreams.validate(new OkHttpClient(),stream());fail();}catch(IOException e){assertTrue(e.getMessage().contains("fuera del proveedor"));}assertEquals(1,server.getRequestCount());}
 @Test public void providerListIncludesOnlyValidZillaAndKeepsOtherServersFirst(){List<Server> list=AnimeAv1Source.parseServers(Jsoup.parse("<script>embeds:{SUB:[{server:\"HLS\",url:\""+EMBED+"\"},{server:\"Voe\",url:\"https://voe.example/e/id\"}]}</script>"));assertEquals(List.of("Voe","HLS"),list.stream().map(Server::name).toList());}
}
