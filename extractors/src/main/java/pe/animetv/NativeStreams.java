package pe.animetv;

import okhttp3.*;
import org.json.*;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.*;
import javax.crypto.Cipher;
import javax.crypto.spec.*;
import static pe.animetv.Models.*;

/** Native adapters. Protocol reference: yuzono/anime-extensions, Apache-2.0. */
final class NativeStreams {
    static final String UA=SourceNetwork.USER_AGENT;
    static String get(String url,String referer) throws Exception {
        return SourceHttp.get(SourceNetwork.HTTP,new Request.Builder().url(url).header("User-Agent",UA).header("Referer",referer).build(),"Al extraer el video:");
    }
    static String decryptUns(String hex) throws Exception {
        if(hex.length()%2!=0 || hex.length()>2_000_000)throw new IOException("Respuesta UPNShare inválida");
        byte[] bytes=new byte[hex.length()/2];for(int n=0;n<bytes.length;n++){int a=Character.digit(hex.charAt(n*2),16),b=Character.digit(hex.charAt(n*2+1),16);if(a<0||b<0)throw new IOException("Respuesta UPNShare inválida");bytes[n]=(byte)((a<<4)|b);}
        Cipher cipher=Cipher.getInstance("AES/CBC/PKCS5Padding");cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec("kiemtienmua911ca".getBytes(StandardCharsets.UTF_8),"AES"),new IvParameterSpec("1234567890oiuytr".getBytes(StandardCharsets.UTF_8)));
        return new String(cipher.doFinal(bytes),StandardCharsets.UTF_8);
    }
    static List<String> unsUrls(String origin,JSONObject data) throws Exception {
        JSONObject config=new JSONObject(data.optString("streamingConfig","{}"));JSONArray order=config.optJSONArray("order");if(order==null)order=new JSONArray(List.of("Google","Cloudflare","In-House"));JSONObject adjust=config.optJSONObject("adjust");
        List<String> urls=new ArrayList<>();for(int n=0;n<order.length();n++) {
            String network=order.optString(n),path=switch(network){case "Tiktok"->data.optString("hlsVideoTiktok");case "Google"->data.optString("hlsVideoGoogle");case "Cloudflare"->data.optString("cf",data.optString("cfNative"));case "In-House"->data.optString("source");default->"";};
            if(path.isBlank())continue;JSONObject a=adjust==null?null:adjust.optJSONObject(network);if(a!=null&&a.optBoolean("disabled"))continue;
            HttpUrl resolved=HttpUrl.get(origin+"/").resolve(path);if(resolved==null||!resolved.isHttps())continue;HttpUrl.Builder u=resolved.newBuilder();
            if(a!=null){String domain=a.optString("domain");if(!domain.isBlank())u.encodedPath(resolved.encodedPath().replace("/hls/","/hlsmod/"+domain+"/"));JSONObject params=a.optJSONObject("params");if(params!=null){Iterator<String> keys=params.keys();while(keys.hasNext()){String key=keys.next();u.setQueryParameter(key,params.get(key).toString());}}}
            urls.add(u.build().toString());
        }
        String nativePath=data.optString("cfNative");if(!nativePath.isBlank()){HttpUrl nativeUrl=HttpUrl.get(origin+"/").resolve(nativePath);if(nativeUrl!=null&&nativeUrl.isHttps()&&!urls.contains(nativeUrl.toString()))urls.add(nativeUrl.toString());}
        return urls;
    }
    static Stream uns(Server server) throws Exception {
        HttpUrl player=HttpUrl.get(server.url());String id=player.fragment();if(id==null||id.isBlank())throw new IOException("UPNShare sin identificador de video");String origin="https://"+player.host();
        String api=HttpUrl.get(origin+"/api/v1/video").newBuilder().addQueryParameter("id",id.split("&")[0]).addQueryParameter("w","1920").addQueryParameter("h","1080").addQueryParameter("r","animeav1.com").build().toString();
        List<String> candidates=unsUrls(origin,new JSONObject(decryptUns(get(api,origin+"/").trim())));
        Exception last=null;for(String url:candidates)try {checkHls(url,origin+"/");return new Stream(url,origin+"/");}catch(Exception e){last=e;}
        throw new IOException("UPNShare: ninguna red HLS accesible"+(last==null?"":": "+last.getMessage()),last);
    }
    static void checkHls(String url,String referer) throws Exception {checkHls(url,referer,0);}
    private static void checkHls(String url,String referer,int depth) throws Exception {
        if(depth>3)throw new IOException("Lista HLS recursiva");String manifest=get(url,referer).trim();if(!manifest.startsWith("#EXTM3U"))throw new IOException("El servidor no devolvió una lista HLS");
        // Validate a real child playlist or segment, not only a 200 response from the master.
        for(String line:manifest.split("\\r?\\n"))if(!line.isBlank()&&!line.startsWith("#")) {
            HttpUrl child=HttpUrl.get(url).resolve(line.trim());if(child==null||!child.isHttps())throw new IOException("Recurso HLS inválido");
            if(manifest.contains("#EXT-X-STREAM-INF")){checkHls(child.toString(),referer,depth+1);return;}
            try(Response r=SourceNetwork.HTTP.newCall(new Request.Builder().url(child).header("User-Agent",UA).header("Referer",referer).build()).execute()) {if(!r.isSuccessful()||r.body()==null)throw new IOException("Segmento HLS: HTTP "+r.code());byte[] sample=r.peekBody(1024).bytes();if(!videoSample(sample))throw new IOException("Segmento HLS no disponible");}return;
        }throw new IOException("Lista HLS vacía");
    }
    static boolean videoSample(byte[] sample){if(sample.length==0)return false;String head=new String(sample,0,Math.min(sample.length,128),StandardCharsets.ISO_8859_1).stripLeading().toLowerCase(Locale.ROOT);return !head.startsWith("<!doctype")&&!head.startsWith("<html")&&!head.startsWith("<head")&&!head.startsWith("<body");}
    static String decryptVoe(String encoded) {
        StringBuilder rot=new StringBuilder();for(char c:encoded.toCharArray())rot.append(c>='a'&&c<='z'?(char)('a'+(c-'a'+13)%26):c>='A'&&c<='Z'?(char)('A'+(c-'A'+13)%26):c);
        String s=rot.toString();for(String noise:List.of("@$","^^","~@","%?","*~","!!","#&"))s=s.replace(noise,"");s=s.replace("_","");s=new String(Base64.getMimeDecoder().decode(s),StandardCharsets.ISO_8859_1);
        StringBuilder shifted=new StringBuilder();for(char c:s.toCharArray())shifted.append((char)(c-3));return new String(Base64.getMimeDecoder().decode(shifted.reverse().toString()),StandardCharsets.UTF_8);
    }
    static Stream yourupload(String url,String episode) throws Exception {
        Document document=org.jsoup.Jsoup.parse(get(url,episode),url);
        for(var script:document.select("script")){String direct=mp4uploadUrl(PackedScript.decode(script.data()));if(!direct.isBlank())return new Stream(direct,url,"video/mp4");}
        throw new IOException("YourUpload: no hay un archivo de video disponible");
    }
    static Stream mp4upload(String url,String episode) throws Exception {
        Document document=org.jsoup.Jsoup.parse(get(url,episode),url);
        for(var script:document.select("script")) { String direct=mp4uploadUrl(PackedScript.decode(script.data()));if(!direct.isBlank())return new Stream(direct,url,"video/mp4"); }
        throw new IOException("MP4Upload: no hay un enlace de video disponible");
    }
    static String mp4uploadUrl(String script) {
        Matcher match=Pattern.compile("(?:\\bsrc|\\bfile)\\s*:\\s*['\"](https?://[^'\"]+)['\"]").matcher(script);
        while(match.find()){String value=match.group(1).replace("\\/","/").replace("&amp;","&");if(value.startsWith("https://"))return value;}
        return "";
    }
    static Stream voe(String url,String episode) throws Exception {
        Document d=org.jsoup.Jsoup.parse(get(url,episode),url);Matcher redirect=Pattern.compile("window.location.href\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(d.select("script").stream().map(e->e.data()).collect(java.util.stream.Collectors.joining("\n")));
        if(redirect.find()){HttpUrl target=HttpUrl.get(url).resolve(redirect.group(1));if(target==null||!target.isHttps())throw new IOException("Redirección Voe inválida");url=target.toString();d=org.jsoup.Jsoup.parse(get(url,episode),url);}
        var data=d.selectFirst("script[type=application/json]");if(data==null)throw new IOException("Voe: no hay datos de reproducción nativa");JSONObject decoded=new JSONObject(decryptVoe(new JSONArray(data.data()).getString(0)));String hls=decoded.optString("source"),mp4=decoded.optString("direct_access_url");
        if(hls.startsWith("https://"))return new Stream(hls,url,"application/x-mpegURL");
        if(mp4.startsWith("https://"))return new Stream(mp4,url);throw new IOException("Voe: video no disponible");
    }
}
