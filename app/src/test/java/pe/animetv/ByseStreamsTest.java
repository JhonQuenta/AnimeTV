package pe.animetv;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.json.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import java.nio.charset.StandardCharsets;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE)
public class ByseStreamsTest {
    @Test public void decryptsAuthenticatedPlaybackWithVersionedKeyParts()throws Exception{
        byte[] key=new byte[32],iv=new byte[12];Arrays.fill(key,(byte)7);Arrays.fill(iv,(byte)9);
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));String plain="{\"sources\":[{\"url\":\"https://cdn.example/master.m3u8\"}]}";
        Base64.Encoder encoder=Base64.getUrlEncoder().withoutPadding();String part=encoder.encodeToString(Arrays.copyOf(key,16));
        JSONObject data=new JSONObject().put("version","2").put("key_parts",new JSONArray(List.of("ignored",part,part,"ignored"))).put("iv",encoder.encodeToString(iv)).put("payload",encoder.encodeToString(cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8))));
        assertEquals(plain,ByseStreams.decrypt(data));data.put("payload",encoder.encodeToString(new byte[32]));try{ByseStreams.decrypt(data);fail("Tampered playback must fail authentication");}catch(java.security.GeneralSecurityException expected){}
    }
    @Test public void excessiveChallengeDifficultyFailsQuickly()throws Exception{try{ByseStreams.solve("nonce",30);fail("Unbounded challenge");}catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("costosa"));}}
    @Test public void canceledChallengeStopsWork()throws Exception{Thread.currentThread().interrupt();try{ByseStreams.solve("nonce",8);fail("Canceled challenge");}catch(java.io.IOException expected){}finally{Thread.interrupted();}}
}
