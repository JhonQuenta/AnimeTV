package pe.animetv;
import java.util.*;
import android.net.Uri;
import javax.crypto.Cipher;
import okhttp3.mockwebserver.*;
import okio.Buffer;
import androidx.media3.datasource.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class MegaDataSourceTest {
    @Test public void decryptionSupportsUnalignedSeeksAndPartialReads()throws Exception{
        byte[] node=new byte[32],plain=new byte[4097];new Random(1).nextBytes(node);new Random(2).nextBytes(plain);
        String key=Base64.getUrlEncoder().withoutPadding().encodeToString(node);Cipher cipher=MegaStreams.cipher(key,0);byte[] encrypted=cipher.doFinal(plain);
        MegaDataSource source=new MegaDataSource(new ByteArrayDataSource(encrypted),key);
        for(int position:new int[]{0,1,15,16,17,1003,4000}){
            int length=Math.min(97,plain.length-position);source.open(new DataSpec.Builder().setUri(Uri.parse("https://example.org/video")).setPosition(position).setLength(length).build());
            byte[] result=new byte[length];int read=0;while(read<length){int n=source.read(result,read,Math.min(13,length-read));assertTrue(n>0);read+=n;}
            assertEquals(-1,source.read(new byte[1],0,1));assertArrayEquals(Arrays.copyOfRange(plain,position,position+length),result);source.close();
        }
    }
    @Test public void publicLinkFormatsAndInvalidKeysAreHandled()throws Exception{
        String key=Base64.getUrlEncoder().withoutPadding().encodeToString(new byte[32]);assertEquals("12345678",MegaStreams.link("https://mega.nz/embed/!12345678!"+key)[0]);assertEquals(key,MegaStreams.link("https://mega.nz/file/12345678#"+key)[1]);
        try{MegaStreams.cipher("short",0);fail();}catch(java.io.IOException expected){}
        try{MegaStreams.link("https://other.com/!12345678!"+key);fail();}catch(java.io.IOException expected){}
    }
    @Test public void media3HttpRangeIsDecryptedAtRequestedPosition()throws Exception{
        byte[] node=new byte[32],plain=new byte[4097];new Random(3).nextBytes(node);new Random(4).nextBytes(plain);
        String key=Base64.getUrlEncoder().withoutPadding().encodeToString(node);byte[] encrypted=MegaStreams.cipher(key,0).doFinal(plain);
        try(MockWebServer server=new MockWebServer()){
            server.start();
            for(int position:new int[]{0,17}){
                server.enqueue(new MockResponse().setResponseCode(206).setHeader("Content-Range","bytes "+position+"-"+(position+63)+"/"+plain.length).setBody(new Buffer().write(Arrays.copyOfRange(encrypted,position,position+64))));
                var stream=new Models.Stream(server.url("/encrypted").toString(),"https://mega.nz/","video/mp4",Map.of(),key);
                MegaDataSource source=new MegaDataSource(PlaybackDataSources.upstream(new Models.Server("Mega","https://mega.nz/file/test","SUB"),stream).createDataSource(),key);
                source.open(new DataSpec.Builder().setUri(Uri.parse(stream.url())).setPosition(position).setLength(64).build());
                byte[] result=new byte[64];int read=0;while(read<result.length){int n=source.read(result,read,result.length-read);assertTrue(n>0);read+=n;}source.close();
                assertArrayEquals(Arrays.copyOfRange(plain,position,position+64),result);
                RecordedRequest request=server.takeRequest();assertEquals("bytes="+position+"-"+(position+63),request.getHeader("Range"));assertEquals("https://mega.nz/",request.getHeader("Referer"));assertFalse(request.getHeaders().toString().contains(key));
            }
        }
    }
}
