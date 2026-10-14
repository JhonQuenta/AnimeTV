package pe.animetv;
import android.net.Uri;
import java.util.Map;
import androidx.media3.datasource.*;
import okhttp3.mockwebserver.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static pe.animetv.Models.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class HlsDataSourceTest {
 @Test public void media3SegmentRequestsKeepZillaHeadersAndAcceptBinaryWithHtmlContentType()throws Exception {
  try(MockWebServer server=new MockWebServer()){
   server.start();server.enqueue(new MockResponse().setHeader("Content-Type","text/html").setBody("\u0000\u0000\u0000 stypmsdh"));
   Stream stream=new Stream(server.url("/m3u8/id").toString(),server.url("/play/id").toString(),"application/x-mpegURL",Map.of("Sec-Fetch-Site","same-origin"));
   DataSource data=PlaybackDataSources.upstream(new Server("HLS","test","SUB"),stream).createDataSource();
   try {data.open(new DataSpec(Uri.parse(server.url("/segs/id/000.html").toString())));byte[] bytes=new byte[32];assertTrue(data.read(bytes,0,bytes.length)>0);}finally{data.close();}
   RecordedRequest request=server.takeRequest();assertEquals("same-origin",request.getHeader("Sec-Fetch-Site"));assertEquals(stream.referer(),request.getHeader("Referer"));assertEquals(NativeStreams.UA,request.getHeader("User-Agent"));
  }
 }
}
