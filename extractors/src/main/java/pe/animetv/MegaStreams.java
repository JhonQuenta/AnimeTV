package pe.animetv;

import java.util.*;
import java.util.regex.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import okhttp3.*;
import org.json.*;
import static pe.animetv.Models.*;

/** Public file-link protocol; keys stay local and are never sent in HTTP headers. */
final class MegaStreams {
    private static final OkHttpClient API=SourceNetwork.HTTP.newBuilder().connectTimeout(8,java.util.concurrent.TimeUnit.SECONDS).readTimeout(35,java.util.concurrent.TimeUnit.SECONDS).callTimeout(45,java.util.concurrent.TimeUnit.SECONDS).build();
    static OkHttpClient apiClient(){return API;}
    private MegaStreams() {}
    static String[] link(String url)throws IOException{
        HttpUrl parsed=HttpUrl.parse(url);if(parsed==null||!parsed.isHttps()||!parsed.host().equals("mega.nz"))throw new IOException("Mega: enlace inválido");
        Matcher legacy=Pattern.compile("!([A-Za-z0-9_-]{8})!([A-Za-z0-9_-]{43})").matcher(url);
        if(legacy.find())return new String[]{legacy.group(1),legacy.group(2)};
        Matcher modern=Pattern.compile("/(?:file|embed)/([A-Za-z0-9_-]{8})#([A-Za-z0-9_-]{43})$").matcher(url);
        if(modern.find())return new String[]{modern.group(1),modern.group(2)};
        throw new IOException("Mega: se necesita un enlace público de archivo con su clave");
    }
    static Stream resolve(String url)throws Exception{return resolve(url,"https://g.api.mega.co.nz/cs");}
    static Stream resolve(String url,String api)throws Exception{
        String[] file=link(url);cipher(file[1],0);
        String body=new JSONArray().put(new JSONObject().put("a","g").put("g",1).put("p",file[0]).put("ssl",2)).toString();
        Request request=new Request.Builder().url(api).post(RequestBody.create(body,MediaType.get("application/json"))).build();
        try(Response response=API.newCall(request).execute()){
            if(!response.isSuccessful()||response.body()==null)throw new IOException("Mega: HTTP "+response.code());
            Object data=new JSONArray(response.body().string()).get(0);
            if(!(data instanceof JSONObject))throw new IOException("Mega: archivo no disponible o límite de transferencia (código "+data+")");
            JSONObject result=(JSONObject)data;HttpUrl download=HttpUrl.parse(result.optString("g"));
            if(download==null||!download.isHttps())throw new IOException("Mega: enlace de descarga HTTPS no disponible");
            return new Stream(download.toString(),"https://mega.nz/","video/mp4",Map.of(),file[1]);
        }
    }
    static Cipher cipher(String encoded,long position)throws IOException{
        try{
            byte[] node=Base64.getUrlDecoder().decode(encoded);if(node.length!=32||position<0)throw new IOException("Mega: clave o posición inválida");
            byte[] key=new byte[16];for(int n=0;n<16;n++)key[n]=(byte)(node[n]^node[n+16]);
            byte[] iv=new byte[16];System.arraycopy(node,16,iv,0,8);ByteBuffer.wrap(iv,8,8).putLong(position/16);
            Cipher cipher=Cipher.getInstance("AES/CTR/NoPadding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new IvParameterSpec(iv));
            int skip=(int)(position%16);if(skip>0)cipher.update(new byte[skip]);return cipher;
        }catch(IOException e){throw e;}catch(Exception e){throw new IOException("Mega: no se pudo iniciar el descifrado",e);}
    }
}
