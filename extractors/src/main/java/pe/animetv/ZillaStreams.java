package pe.animetv;

import java.io.IOException;
import java.util.Map;
import java.util.regex.*;
import okhttp3.*;
import static pe.animetv.Models.*;

/** Zilla publishes extensionless fMP4 HLS and requires same-origin fetch metadata. */
final class ZillaStreams {
    static boolean supports(String embed) {
        try { HttpUrl u=HttpUrl.get(embed);return u.isHttps()&&u.host().equals("player.zilla-networks.com")&&u.encodedPath().matches("/play/[a-fA-F0-9]{32}"); }
        catch(IllegalArgumentException e){return false;}
    }
    static Stream descriptor(String embed) throws IOException {
        if(!supports(embed))throw new IOException("Enlace HLS/Zilla inválido");
        HttpUrl url=HttpUrl.get(embed);
        return new Stream(url.newBuilder().encodedPath(url.encodedPath().replace("/play/","/m3u8/")).query(null).fragment(null).build().toString(),
            url.newBuilder().query(null).fragment(null).build().toString(),"application/x-mpegURL",Map.of("Sec-Fetch-Site","same-origin"));
    }
    static Stream resolve(String embed) throws IOException {
        Stream stream=descriptor(embed);validate(SourceNetwork.HTTP,stream);return stream;
    }
    static void validate(OkHttpClient client,Stream stream) throws IOException {
        HttpUrl manifest=HttpUrl.get(stream.url());String text;
        try(Response r=client.newCall(request(manifest,stream)).execute()) {
            if(!r.isSuccessful()||r.body()==null)throw new IOException("Lista HLS: HTTP "+r.code());
            text=r.peekBody(1024*1024).string();
        }
        if(!text.stripLeading().startsWith("#EXTM3U"))throw new IOException("Zilla no devolvió una lista HLS");
        Matcher init=Pattern.compile("#EXT-X-MAP:.*?URI=\"([^\"]+)\"").matcher(text);
        if(init.find())checkResource(client,manifest,init.group(1),stream);
        for(String line:text.split("\\r?\\n"))if(!line.isBlank()&&!line.startsWith("#")) {
            if(text.contains("#EXT-X-STREAM-INF"))throw new IOException("Lista maestra Zilla no reconocida");
            checkResource(client,manifest,line.trim(),stream);return;
        }
        throw new IOException("Lista HLS vacía");
    }
    private static Request request(HttpUrl url,Stream stream){
        Request.Builder request=new Request.Builder().url(url).header("User-Agent",NativeStreams.UA).header("Referer",stream.referer());
        stream.headers().forEach(request::header);return request.build();
    }
    private static void checkResource(OkHttpClient client,HttpUrl manifest,String path,Stream stream) throws IOException {
        HttpUrl url=manifest.resolve(path);
        if(url==null||!url.scheme().equals(manifest.scheme())||!url.host().equals(manifest.host())||url.port()!=manifest.port())throw new IOException("Recurso HLS fuera del proveedor");
        try(Response r=client.newCall(request(url,stream)).execute()) {
            if(!r.isSuccessful()||r.body()==null)throw new IOException("Segmento HLS: HTTP "+r.code());
            if(!NativeStreams.videoSample(r.peekBody(256).bytes()))throw new IOException("Segmento HLS no disponible");
        }
    }
}
