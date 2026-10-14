package pe.animetv;
import java.io.IOException;
import java.util.regex.*;
import okhttp3.HttpUrl;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import static pe.animetv.Models.*;
final class JkStreams {
    static String ownUrl(Document d)throws Exception{
        Element source=d.selectFirst("video source[src]");if(source!=null)return source.absUrl("src");
        for(Element script:d.select("script")){Matcher m=Pattern.compile("\\burl\\s*:\\s*['\"](https://[^'\"]+)['\"]").matcher(script.data());if(m.find())return m.group(1);}
        throw new IOException("JKAnime: vídeo eliminado o reproductor no disponible");
    }
    static Stream own(String url,String episode)throws Exception{String direct=ownUrl(Jsoup.parse(NativeStreams.get(url,episode),url));HttpUrl u=HttpUrl.parse(direct);if(u==null||!u.isHttps()||!(u.host().equals("playmudos.com")||u.host().endsWith(".playmudos.com")||u.host().equals("jkplayers.com")))throw new IOException("JKAnime: origen de vídeo no reconocido");return new Stream(direct,url,direct.contains(".m3u8")?"application/x-mpegURL":"video/mp4");}
    static java.util.List<String> wishUrls(Document d,String page)throws Exception{
        java.util.LinkedHashSet<String> candidates=new java.util.LinkedHashSet<>();
        for(Element script:d.select("script")){
            String unpacked=PackedScript.decode(script.data());
            Matcher variable=Pattern.compile("\\b(?:var|let|const)\\s+([A-Za-z_$][\\w$]*)\\s*=\\s*\\{").matcher(unpacked);
            while(variable.find()){
                JSONObject data;try{data=new JSONObject("{"+JsData.object(unpacked,variable.group(1))+"}");}catch(Exception bad){continue;}
                for(String key:java.util.List.of("hls4","hls","hls3","hls2","http")){
                    String link=data.optString(key);if(link.isBlank())continue;
                    HttpUrl resolved=HttpUrl.get(page).resolve(link);if(resolved!=null&&resolved.isHttps())candidates.add(resolved.toString());
                }
            }
            if(candidates.isEmpty()){String direct=NativeStreams.mp4uploadUrl(unpacked);if(!direct.isBlank())candidates.add(direct);}
        }
        if(candidates.isEmpty())throw new IOException("Streamwish: vídeo eliminado o enlace no disponible");
        return java.util.List.copyOf(candidates);
    }
    static String wishUrl(Document d,String page)throws Exception{return wishUrls(d,page).get(0);}
    static Stream streamwish(String url,String episode)throws Exception{
        LatStreams.Page page=LatStreams.page(url,episode);Exception last=null;
        for(String direct:wishUrls(page.document(),page.url()))try{
            boolean hls=direct.contains(".m3u8")||direct.contains("master.txt");
            if(hls)NativeStreams.checkHls(direct,page.url());
            return new Stream(direct,page.url(),hls?"application/x-mpegURL":"video/mp4");
        }catch(Exception failure){last=failure;}
        throw new IOException("Streamwish: ninguna ruta de vídeo accesible",last);
    }
}
