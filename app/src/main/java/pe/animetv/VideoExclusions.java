package pe.animetv;
import android.content.*;import static pe.animetv.Models.*;
/** File-specific exclusions expire: a host name is never enough to infer its codec. */
final class VideoExclusions {
    private final SharedPreferences prefs;
    VideoExclusions(Context context){prefs=context.getSharedPreferences("incompatible-video",Context.MODE_PRIVATE);}
    private String key(Server server,String episode){return episode+"|"+server.url()+"|"+server.language();}
    boolean blocked(Server server,String episode,long now){return prefs.getLong(key(server,episode),0)>now;}
    void block(Server server,String episode,long now){prefs.edit().putLong(key(server,episode),now+86400000).apply();}
}
