package pe.animetv;

import java.io.IOException;
import java.util.*;
import java.util.regex.*;
import okhttp3.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import static pe.animetv.Models.*;

/** Native host protocols; dictionary scripts are decoded as data, never evaluated. */
final class LatStreams {
    record Page(String url,Document document){}
    static Page page(String url,String referer)throws Exception {
        Request request=new Request.Builder().url(url).header("User-Agent",NativeStreams.UA).header("Referer",referer)
            .header("Accept","text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8").header("Accept-Language","es-ES,es;q=0.9")
            .header("Sec-Fetch-Dest","iframe").header("Sec-Fetch-Site","cross-site").header("Sec-Fetch-Mode","navigate").build();
        try(Response r=SourceNetwork.HTTP.newCall(request).execute()){
            if(!r.isSuccessful()||r.body()==null)throw new IOException("HTTP "+r.code()+" al obtener el reproductor");
            String finalUrl=r.request().url().toString();return new Page(finalUrl,Jsoup.parse(r.body().string(),finalUrl));
        }
    }
    static String doodPath(String html)throws IOException {
        Matcher path=Pattern.compile("['\"](/pass_md5/[^'\"\\s]+)['\"]").matcher(html);
        if(!path.find())throw new IOException("DoodStream: vídeo eliminado o reproductor no disponible");return path.group(1);
    }
    static Stream dood(String url,String episode)throws Exception {
        Page p=page(url.replace("/d/","/e/"),episode);HttpUrl pass=HttpUrl.get(p.url()).resolve(doodPath(p.document().html()));
        String prefix=NativeStreams.get(pass.toString(),p.url()).trim();
        if(!prefix.startsWith("https://"))throw new IOException("DoodStream: respuesta de reproducción inválida");
        String token=pass.pathSegments().get(pass.pathSegments().size()-1);
        String direct=prefix+UUID.randomUUID().toString().replace("-","").substring(0,10)+"?token="+token+"&expiry="+System.currentTimeMillis();
        return new Stream(direct,"https://"+HttpUrl.get(p.url()).host()+"/","video/mp4");
    }
    static String packedVideo(Document document){for(Element script:document.select("script")){
        String unpacked=PackedScript.decode(script.data());String file=NativeStreams.mp4uploadUrl(unpacked);if(!file.isBlank())return file;
    }return "";}
    static Stream lulu(String url,String episode)throws Exception {
        Document document=Jsoup.parse(NativeStreams.get(url,episode),url);String direct=packedVideo(document);if(direct.isBlank())throw new IOException("Lulu: vídeo eliminado o enlace no disponible");
        return new Stream(direct,url,direct.contains(".m3u8")?"application/x-mpegURL":"video/mp4");
    }
    static Stream filemoon(String url,String episode)throws Exception {
        Page p=page(url,episode);
        if(p.document().title().contains("Byse")||p.document().select("script[src*=/assets/index-]").size()>0)return ByseStreams.resolve(p.url(),episode);
        String direct=packedVideo(p.document());Element iframe=p.document().selectFirst("iframe[src]");
        if(direct.isBlank()&&iframe!=null){HttpUrl nested=HttpUrl.get(p.url()).resolve(iframe.attr("src"));if(nested!=null&&nested.isHttps()){p=page(nested.toString(),p.url());direct=packedVideo(p.document());}}
        if(direct.isBlank())throw new IOException("Filemoon: vídeo eliminado o enlace no disponible");return new Stream(direct,p.url());
    }
    static String mixdropUrl(String html)throws IOException {
        Document d=Jsoup.parse(html);for(Element script:d.select("script")){
            Matcher m=Pattern.compile("MDCore\\.wurl\\s*=\\s*['\"]([^'\"]+)['\"]").matcher(PackedScript.decode(script.data()));
            if(m.find()){String value=m.group(1).replace("\\/","/");if(value.startsWith("//"))value="https:"+value;if(value.startsWith("https://"))return value;}
        }throw new IOException("Mixdrop: vídeo eliminado o enlace no disponible");
    }
    static Stream mixdrop(String url,String episode)throws Exception {Page p=page(url,episode);return new Stream(mixdropUrl(p.document().html()),p.url(),"video/mp4");}
}
