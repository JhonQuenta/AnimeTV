package pe.animetv;
import java.util.*;
import okhttp3.mockwebserver.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class TioStreamsTest {
    private MockWebServer server;
    @Before public void start()throws Exception{server=new MockWebServer();server.start();}
    @After public void stop()throws Exception{server.shutdown();}
    @Test public void yourUploadExtractsMp4AndKeepsEmbedReferer()throws Exception{
        server.enqueue(new MockResponse().setBody("<script>var player={file:'https://vidcache.net:8161/video.mp4'};</script>"));String url=server.url("/embed/test").toString();var stream=NativeStreams.yourupload(url,"https://tioanime.com/ver/test-2");assertEquals("https://vidcache.net:8161/video.mp4",stream.url());assertEquals(url,stream.referer());assertEquals("video/mp4",stream.mimeType());
    }
    @Test public void megaRequestsHttpsAndNeverSendsPublicKeyToApi()throws Exception{
        server.enqueue(new MockResponse().setBody("[{\"g\":\"https://download.mega.co.nz/video\",\"s\":12345}]"));String key=Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);var stream=MegaStreams.resolve("https://mega.nz/file/12345678#"+key,server.url("/cs").toString());assertEquals(key,stream.megaKey());assertTrue(stream.headers().isEmpty());String body=server.takeRequest().getBody().readUtf8();assertTrue(body.contains("\"ssl\":2"));assertFalse(body.contains(key));
    }
    @Test public void megaQuotaErrorsAreNotTreatedAsVideoUrls()throws Exception{
        server.enqueue(new MockResponse().setBody("[-17]"));String key=Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);try{MegaStreams.resolve("https://mega.nz/file/12345678#"+key,server.url("/cs").toString());fail();}catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("-17"));}
    }
    @Test public void megaApiTimeoutAllowsTheObservedSlowResponse(){assertEquals(45000,MegaStreams.apiClient().callTimeoutMillis());assertTrue(MegaStreams.apiClient().readTimeoutMillis()>22000);assertEquals(10000,SourceNetwork.HTTP.callTimeoutMillis());}
    @Test public void megaApiCanRespondAfterThePreviousTenSecondLimit()throws Exception{
        server.enqueue(new MockResponse().setBodyDelay(12,java.util.concurrent.TimeUnit.SECONDS).setBody("[{\"g\":\"https://download.mega.co.nz/video\",\"s\":12345}]"));
        String key=Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);assertEquals("https://download.mega.co.nz/video",MegaStreams.resolve("https://mega.nz/file/12345678#"+key,server.url("/cs").toString()).url());
    }
}
