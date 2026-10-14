package pe.animetv;
import org.junit.*;import org.junit.runner.RunWith;import org.robolectric.RobolectricTestRunner;import org.robolectric.annotation.Config;
import org.json.*;import java.util.*;import java.nio.charset.StandardCharsets;import javax.crypto.*;import javax.crypto.spec.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class NativeStreamsTest {
 @Test public void extensionlessManifestHasExplicitHlsType(){assertEquals("application/x-mpegURL",new pe.animetv.Models.Stream("https://player.example/m3u8/id","https://player.example/play/id","application/x-mpegURL").mimeType());}
 @Test public void disguisedSegmentsAreCheckedByContent(){assertTrue(NativeStreams.videoSample(new byte[]{0,0,0,24,102,116,121,112}));assertFalse(NativeStreams.videoSample("<!DOCTYPE html><html>blocked</html>".getBytes(StandardCharsets.UTF_8)));assertFalse(NativeStreams.videoSample(new byte[0]));}
 @Test public void unsPayloadDecryptsAndRejectsMalformedHex()throws Exception{
  String original="{\"source\":\"/hls/test/master.m3u8\"}";Cipher c=Cipher.getInstance("AES/CBC/PKCS5Padding");c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec("kiemtienmua911ca".getBytes(StandardCharsets.UTF_8),"AES"),new IvParameterSpec("1234567890oiuytr".getBytes(StandardCharsets.UTF_8)));StringBuilder hex=new StringBuilder();for(byte b:c.doFinal(original.getBytes(StandardCharsets.UTF_8)))hex.append(String.format("%02x",b&255));assertEquals(original,NativeStreams.decryptUns(hex.toString()));try{NativeStreams.decryptUns("xz");fail();}catch(java.io.IOException expected){}
 }
 @Test public void unsHonorsDisabledNetworksAndRetainsNativeDelivery()throws Exception{
  JSONObject j=new JSONObject("{\"hlsVideoGoogle\":\"https://google.example/list.m3u8\",\"cf\":\"/hls/cf\",\"cfNative\":\"/hls/native\",\"source\":\"/hls/own\",\"streamingConfig\":\"{\\\"order\\\":[\\\"Google\\\",\\\"Cloudflare\\\",\\\"In-House\\\"],\\\"adjust\\\":{\\\"Google\\\":{\\\"disabled\\\":true}}}\"}");List<String> urls=NativeStreams.unsUrls("https://player.example",j);assertEquals(3,urls.size());assertFalse(urls.stream().anyMatch(u->u.contains("google.example")));assertTrue(urls.contains("https://player.example/hls/native"));assertTrue(urls.contains("https://player.example/hls/own"));
 }
 @Test public void voeDecodesActualLayerOrderWithNoise(){
  String original="{\"source\":\"https://media.example/master.m3u8\"}";String encoded=Base64.getEncoder().encodeToString(original.getBytes(StandardCharsets.UTF_8));StringBuilder shifted=new StringBuilder();for(char c:new StringBuilder(encoded).reverse().toString().toCharArray())shifted.append((char)(c+3));String first=Base64.getEncoder().encodeToString(shifted.toString().getBytes(StandardCharsets.ISO_8859_1));StringBuilder rot=new StringBuilder();for(char c:first.toCharArray())rot.append(c>='a'&&c<='z'?(char)('a'+(c-'a'+13)%26):c>='A'&&c<='Z'?(char)('A'+(c-'A'+13)%26):c);assertEquals(original,NativeStreams.decryptVoe(rot.substring(0,5)+"@$"+rot.substring(5)));
 }
}
