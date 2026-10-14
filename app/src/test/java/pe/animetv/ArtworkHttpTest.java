package pe.animetv;

import okhttp3.*;
import okhttp3.mockwebserver.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class ArtworkHttpTest {
    private MockWebServer server;
    @Before public void start()throws Exception{server=new MockWebServer();server.start();}
    @After public void stop()throws Exception{server.shutdown();}
    @Test public void artworkSurvivesBitmapEvictionWithoutAnotherDownload()throws Exception{
        server.enqueue(new MockResponse().setHeader("Cache-Control","max-age=600").setBody("unchanged image bytes"));
        OkHttpClient client=ArtworkHttp.client(RuntimeEnvironment.getApplication());
        Request request=new Request.Builder().url(server.url("/poster")).build();
        try(Response first=client.newCall(request).execute()){assertEquals("unchanged image bytes",first.body().string());assertNotNull(first.networkResponse());}
        try(Response second=client.newCall(request).execute()){assertEquals("unchanged image bytes",second.body().string());assertNotNull(second.cacheResponse());assertNull(second.networkResponse());}
        assertEquals(1,server.getRequestCount());
    }
    @Test public void noStoreArtworkIsFetchedAgain()throws Exception{
        for(int n=0;n<2;n++)server.enqueue(new MockResponse().setHeader("Cache-Control","no-store").setBody("image"));
        OkHttpClient client=ArtworkHttp.client(RuntimeEnvironment.getApplication());
        Request request=new Request.Builder().url(server.url("/private-poster")).build();
        for(int n=0;n<2;n++)try(Response response=client.newCall(request).execute()){assertEquals("image",response.body().string());assertNull(response.cacheResponse());}
        assertEquals(2,server.getRequestCount());
    }
}
