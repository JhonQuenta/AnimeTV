package pe.animetv;

import android.net.Uri;
import androidx.media3.datasource.*;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import okhttp3.mockwebserver.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.util.Map;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class PlaybackDataSourcesTest {
    @Test public void cachedVideoRangePreservesRefererAndAvoidsSecondDownload()throws Exception {
        try(MockWebServer server=new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(206).setHeader("Content-Range","bytes 4-7/12").setBody("efgh"));
            server.start();
            OkHttpDataSource.Factory upstream=new OkHttpDataSource.Factory(PlaybackDataSources.HTTP).setDefaultRequestProperties(Map.of("Referer","https://example.org/embed"));
            DataSource.Factory factory=PlaybackDataSources.cached(RuntimeEnvironment.getApplication(),upstream);
            DataSpec spec=new DataSpec.Builder().setUri(Uri.parse(server.url("/video.mp4?token=unique").toString())).setPosition(4).setLength(4).build();
            for(int n=0;n<2;n++) {
                DataSource data=factory.createDataSource();
                try { data.open(spec);byte[] bytes=new byte[4];int offset=0;while(offset<4){int read=data.read(bytes,offset,4-offset);assertTrue(read>0);offset+=read;}assertEquals("efgh",new String(bytes,java.nio.charset.StandardCharsets.UTF_8)); }
                finally {data.close();}
            }
            assertEquals(1,server.getRequestCount());
            RecordedRequest request=server.takeRequest();assertEquals("bytes=4-7",request.getHeader("Range"));assertEquals("https://example.org/embed",request.getHeader("Referer"));
        }
    }
}
