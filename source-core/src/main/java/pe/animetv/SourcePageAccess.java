package pe.animetv;
import java.io.IOException;
/** A source can request protected pages without depending on Activity, WebView or TV UI. */
public final class SourcePageAccess {
    public interface Host {String get(String url,boolean interactive)throws Exception;String catalog(String url)throws Exception;}
    private static volatile Host host;
    private SourcePageAccess(){}
    public static void install(Host value){host=value;}
    public static void clear(){host=null;}
    private static Host host()throws IOException{Host value=host;if(value==null)throw new IOException("Abre AnimeLatinoHD desde la app para verificar el acceso");return value;}
    public static String get(String url)throws Exception{return get(url,true);}
    public static String get(String url,boolean interactive)throws Exception{return host().get(url,interactive);}
    public static String catalog(String url)throws Exception{return host().catalog(url);}
}
