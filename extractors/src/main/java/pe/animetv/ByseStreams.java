package pe.animetv;

import okhttp3.*;
import org.json.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import static pe.animetv.Models.*;

/** Native Byse protocol adapted from yuzono FilemoonExtractor (Apache-2.0).
 * Its machine challenge runs off the UI thread with strict time/iteration limits. */
final class ByseStreams {
    static final String UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36";
    static Stream resolve(String url,String episode) throws Exception {
        HttpUrl page=HttpUrl.get(url);String id=page.pathSegments().get(page.pathSegments().size()-1);
        String original="https://"+page.host();
        JSONObject details=request(original+"/api/videos/"+id+"/embed/details",null,new Headers.Builder().add("User-Agent",UA).build());
        HttpUrl embed=HttpUrl.get(details.getString("embed_frame_url"));if(!embed.isHttps())throw new IOException("Byse: enlace inseguro");String origin="https://"+embed.host();
        Headers headers=new Headers.Builder().add("User-Agent",UA).add("Accept","*/*").add("Referer",episode).add("Origin",origin).build();
        JSONObject challenge=request(origin+"/api/videos/access/challenge",new JSONObject(),headers);
        KeyPairGenerator generator=KeyPairGenerator.getInstance("EC");generator.initialize(new ECGenParameterSpec("secp256r1"));KeyPair pair=generator.generateKeyPair();ECPublicKey pub=(ECPublicKey)pair.getPublic();
        JSONObject key=new JSONObject().put("alg","ES256").put("crv","P-256").put("ext",true).put("key_ops",new JSONArray(List.of("verify"))).put("kty","EC").put("x",b64(coordinate(pub.getW().getAffineX().toByteArray()))).put("y",b64(coordinate(pub.getW().getAffineY().toByteArray())));
        Signature signature=Signature.getInstance("SHA256withECDSA");signature.initSign(pair.getPrivate());signature.update(challenge.getString("nonce").getBytes(StandardCharsets.UTF_8));
        JSONObject client=new JSONObject().put("user_agent",UA).put("pixel_ratio",1).put("screen_width",1920).put("screen_height",1080).put("color_depth",24).put("languages",new JSONArray(List.of("en-US","en"))).put("timezone","America/New_York").put("hardware_concurrency",4).put("touch_points",0).put("webgl_vendor","Google Inc. (Intel)").put("webgl_renderer","ANGLE (Intel, Intel(R) UHD Graphics 630, OpenGL 4.5)").put("media_devices","ai0ao0vi0").put("pointer_type","fine,hover").put("extra",new JSONObject().put("vendor","").put("appVersion","5.0 (X11)"));
        SecureRandom random=new SecureRandom();for(String field:List.of("canvas_hash","audio_hash","webgl_params_hash","fonts_hash","codecs_hash")){byte[] bytes=new byte[32];random.nextBytes(bytes);client.put(field,b64(bytes));}
        JSONObject attest=request(origin+"/api/videos/access/attest",new JSONObject().put("viewer_id","").put("device_id","").put("challenge_id",challenge.getString("challenge_id")).put("nonce",challenge.getString("nonce")).put("signature",b64(signature.sign())).put("public_key",key).put("client",client).put("storage",new JSONObject()).put("attributes",new JSONObject().put("entropy","low")),headers);
        JSONObject fingerprint=new JSONObject().put("token",attest.getString("token")).put("viewer_id",attest.getString("viewer_id")).put("device_id",attest.getString("device_id")).put("confidence",attest.getDouble("confidence"));
        JSONObject payload=new JSONObject().put("fingerprint",fingerprint);
        headers=headers.newBuilder().add("X-Embed-Origin",HttpUrl.get(episode).host()).add("X-Embed-Referer","https://"+HttpUrl.get(episode).host()+"/").add("X-Embed-Parent",url).add("Cookie","byse_viewer_id="+attest.getString("viewer_id")+"; byse_device_id="+attest.getString("device_id")).build();
        JSONObject pow=request(origin+"/api/videos/"+id+"/embed/captcha",payload,headers);
        String solution=solve(pow.getString("pow_nonce"),pow.getInt("pow_difficulty"));
        JSONObject verification=request(origin+"/api/videos/"+id+"/embed/captcha/verify",new JSONObject().put("pow_token",pow.getString("pow_token")).put("solution",solution).put("fingerprint",fingerprint),headers);
        if(!verification.optString("status").equals("ok"))throw new IOException("Byse: no se autorizó la reproducción");
        JSONObject playback=request(origin+"/api/videos/"+id+"/embed/playback",payload,headers.newBuilder().add("X-Captcha-Token",verification.getString("token")).build());
        if(playback.optJSONArray("sources")==null)playback=new JSONObject(decrypt(playback.getJSONObject("playback")));
        JSONArray sources=playback.getJSONArray("sources");Exception last=null;
        for(int n=0;n<sources.length();n++){JSONObject source=sources.getJSONObject(n);String video=source.optString("url",source.optString("file"));if(video.startsWith("https://"))return new Stream(video,origin,"application/x-mpegURL");}
        throw new IOException("Byse: ningún video nativo accesible",last);
    }
    private static JSONObject request(String url,JSONObject body,Headers headers)throws Exception{
        Request.Builder r=new Request.Builder().url(url).headers(headers);if(body!=null)r.post(RequestBody.create(body.toString(),MediaType.get("application/json")));
        try(Response response=SourceNetwork.HTTP.newCall(r.build()).execute()){if(!response.isSuccessful()||response.body()==null)throw new IOException("Byse: HTTP "+response.code());return new JSONObject(response.body().string());}
    }
    private static byte[] coordinate(byte[] value){byte[] out=new byte[32];int count=Math.min(32,value.length);System.arraycopy(value,value.length-count,out,32-count,count);return out;}
    private static String b64(byte[] value){return Base64.getUrlEncoder().withoutPadding().encodeToString(value);}
    static String decrypt(JSONObject data)throws Exception{
        JSONArray parts=data.getJSONArray("key_parts");List<byte[]> chunks=new ArrayList<>();String version=data.optString("version");int v=version.isBlank()?0:Integer.parseInt(version);
        if(v>0&&parts.length()>=v){chunks.add(Base64.getUrlDecoder().decode(parts.getString(v-1)));chunks.add(Base64.getUrlDecoder().decode(parts.getString(parts.length()-v)));}else for(int n=0;n<parts.length();n++)chunks.add(Base64.getUrlDecoder().decode(parts.getString(n)));
        int length=chunks.stream().mapToInt(a->a.length).sum();byte[] key=new byte[length];int index=0;for(byte[] chunk:chunks){System.arraycopy(chunk,0,key,index,chunk.length);index+=chunk.length;}
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,Base64.getUrlDecoder().decode(data.getString("iv"))));return new String(cipher.doFinal(Base64.getUrlDecoder().decode(data.getString("payload"))),StandardCharsets.UTF_8);
    }
    static String solve(String nonce,int difficulty)throws IOException{
        if(difficulty<0||difficulty>20)throw new IOException("Byse: verificación demasiado costosa para TV");long deadline=System.nanoTime()+8_000_000_000L;int[] buffer=new int[512];
        for(int counter=0;counter<=200000;counter++){if((counter&63)==0&&(Thread.currentThread().isInterrupted()||System.nanoTime()>deadline))throw new IOException("Byse: tiempo de verificación agotado");if(Integer.numberOfLeadingZeros(hash(nonce+":"+counter,buffer))>=difficulty)return Integer.toString(counter);}
        throw new IOException("Byse: verificación no completada");
    }
    static int hash(String value,int[] buffer){
        int[] s={1779033703,(int)3144134277L,1013904242,(int)2773480762L};
        for(byte b:value.getBytes(StandardCharsets.ISO_8859_1)){s[0]+=b&255;s[0]=Integer.rotateLeft(s[0],7);s[0]+=s[1];s[3]=Integer.rotateLeft(s[3]^s[0],16);s[2]+=s[3];s[1]=Integer.rotateLeft(s[1]^s[2],12);s[0]+=s[1];s[3]=Integer.rotateLeft(s[3]^s[0],8);s[2]+=s[3];s[1]=Integer.rotateLeft(s[1]^s[2],7);}
        for(int n=0;n<8;n++)round(s);for(int n=0;n<512;n++){round(s);buffer[n]=s[0]^s[2];}
        for(int pass=0;pass<2;pass++)for(int n=0;n<512;n++){int c=Integer.rotateLeft(buffer[n]+buffer[buffer[n]&511],13)^(buffer[(n+1)&511]*(int)2654435761L);buffer[n]=c;s[0]^=c;round(s);}
        round(s);int out=s[0];for(int n=0;n<64;n++)out=Integer.rotateLeft(out+buffer[n],5)^(buffer[n]*(int)2246822519L);return out^s[2];
    }
    private static void round(int[] s){s[0]+=s[1];s[3]=Integer.rotateLeft(s[3]^s[0],16);s[2]+=s[3];s[1]=Integer.rotateLeft(s[1]^s[2],12);s[0]+=s[1];s[3]=Integer.rotateLeft(s[3]^s[0],8);s[2]+=s[3];s[1]=Integer.rotateLeft(s[1]^s[2],7);}
}
