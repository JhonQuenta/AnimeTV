package pe.animetv;
import java.io.IOException;import static pe.animetv.Models.*;
/** One shared entry point for host protocols, reused by all five source modules. */
public final class StreamExtractors {
    private StreamExtractors(){}
    public static String userAgent(String server){return server.toLowerCase(java.util.Locale.ROOT).contains("byse")?ByseStreams.UA:SourceNetwork.USER_AGENT;}
    public static javax.crypto.Cipher megaCipher(String key,long position)throws java.io.IOException{return MegaStreams.cipher(key,position);}
    public static Stream resolve(Server server,String referer)throws Exception{return switch(server.name()){
        case "Voe"->NativeStreams.voe(server.url(),referer);
        case "MP4Upload"->NativeStreams.mp4upload(server.url(),referer);
        case "Byse"->ByseStreams.resolve(server.url(),referer);
        case "HLS"->ZillaStreams.resolve(server.url());
        case "YourUpload"->NativeStreams.yourupload(server.url(),referer);
        case "Mega"->MegaStreams.resolve(server.url());
        case "Mixdrop"->LatStreams.mixdrop(server.url(),referer);
        case "Filemoon"->LatStreams.filemoon(server.url(),referer);
        case "Lulu"->LatStreams.lulu(server.url(),referer);
        case "Dsvplay","Vide0","Do7go","Doodstream"->LatStreams.dood(server.url(),referer);
        case "Streamwish"->JkStreams.streamwish(server.url(),referer);
        case "Desu","Magi","Desuka"->JkStreams.own(server.url(),referer);
        default->throw new IOException("Extractor no disponible: "+server.name());
    };}
}
