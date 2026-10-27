package pe.animetv;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.*;
import java.util.*;
import static pe.animetv.Models.*;

public final class Store {
    private final SharedPreferences p;
    public Store(Context c) { p = c.getSharedPreferences("anime-tv", Context.MODE_PRIVATE); }
    public float speed(){return p.getFloat("playback-speed",1f);}
    public void speed(float speed){if(!Float.isNaN(speed)&&!Float.isInfinite(speed)&&speed>=.25f&&speed<=2f)p.edit().putFloat("playback-speed",speed).apply();}
    public String source(){return p.getString("source","animeav1");}
    public void source(String id){p.edit().putString("source",id).apply();}
    public String type(String url) { return p.getString("type:"+url,""); }
    public String poster(String url){return p.getString("poster:"+url,"");}
    public void poster(String url,String image){if(!image.isBlank())p.edit().putString("poster:"+url,image).apply();}
    public void type(String url,String type) { if(!type.isBlank())p.edit().putString("type:"+url,type).apply(); }
    public String status(String url){return System.currentTimeMillis()-p.getLong("status-time:"+url,0)<86400000?p.getString("status:"+url,""):"";}
    public void status(String url,String value){if(!value.isBlank())p.edit().putString("status:"+url,value).putLong("status-time:"+url,System.currentTimeMillis()).apply();}
    public String backdrop(String url){return p.getString("backdrop:"+url,"");}
    public void backdrop(String url,String image){if(!image.isBlank())p.edit().putString("backdrop:"+url,image).apply();}
    public String publication(String url){return p.getString("publication:"+url,"");}
    public void publication(String url,String value){if(!value.isBlank())p.edit().putString("publication:"+url,value).apply();}
    public int seconds(String key) { return p.getInt(key, key.equals("timeline")?30:key.equals("forward") || key.equals("backward") ? 5 : 85); }
    public void seconds(String key, int value) { p.edit().putInt(key, Math.max(1, Math.min(value, 600))).apply(); }
    public String language() { return p.getString("language","SUB"); }
    public void language(String language) { p.edit().putString("language",language).apply(); }
    public boolean automatic() { return p.getBoolean("automatic", true); }
    public void automatic(boolean value) { p.edit().putBoolean("automatic", value).apply(); }
    public String serverDefault(String source){return p.getString("server-default:"+source,source.equals("latanime")||source.equals("animelatinohd")||source.equals("jkanime")?"ask":p.getBoolean("automatic",true)?"auto":"ask");}
    public void serverDefault(String source,String server){p.edit().putString("server-default:"+source,server).apply();}
    public boolean textureSurface() { return p.getBoolean("texture-surface",true); }
    public void textureSurface(boolean value) { p.edit().putBoolean("texture-surface",value).apply(); }
    public String diagnostics() { return p.getString("diagnostics",""); }
    public void diagnostic(String line) { String old=diagnostics(); String next=old+"\n"+line; if(next.length()>4000)next=next.substring(next.length()-4000); p.edit().putString("diagnostics",next).apply(); }
    public long position(String url) { return p.getLong("position:" + url, 0); }
    public void position(String url, long value) { p.edit().putLong("position:" + url, Math.max(0, value)).apply(); }
    public long duration(String url) { return p.getLong("duration:"+url,0); }
    public boolean watched(String url) { return p.getBoolean("watched:"+url,false); }
    public void watched(String url,boolean value) {
        p.edit().putBoolean("watched:"+url,value).apply();
    }
    public void markPreviousEpisodes(List<Models.Episode> episodes,int exclusiveEnd) {
        if(exclusiveEnd<0||exclusiveEnd>episodes.size())throw new IllegalArgumentException("Episodio fuera de rango");
        SharedPreferences.Editor edit=p.edit();
        for(int n=0;n<exclusiveEnd;n++)edit.putBoolean("watched:"+episodes.get(n).url(),true);
        edit.apply();
    }
    public int progress(String url) { long d=duration(url);return d>0?(int)Math.min(1000,position(url)*1000/d):0; }
    public void progress(String url,long position,long duration) { if(duration<=0)return; p.edit().putLong("position:"+url,Math.max(0,position)).putLong("duration:"+url,duration).putBoolean("watched:"+url,watched(url)||position>=duration*0.95).apply(); }
    public String lastEpisode(String anime) { return p.getString("last-episode:"+anime,""); }
    public void lastEpisode(String anime,String episode) { if(anime!=null&&!anime.isBlank())p.edit().putString("last-episode:"+anime,episode).apply(); }
    public boolean episodeList(){return p.getBoolean("episode-list",false);}
    public void episodeList(boolean value){p.edit().putBoolean("episode-list",value).apply();}
    public String preferredHost() { return p.getString("preferred-host", ""); }
    public void preferredHost(String value) { p.edit().putString("preferred-host", value).apply(); }
    public List<String> categories() {
        List<String> out = new ArrayList<>(); out.add("Por defecto");
        try { JSONArray a = new JSONArray(p.getString("categories", "[]")); for(int i=0;i<a.length();i++) out.add(a.getString(i)); } catch(JSONException ignored) {}
        return out;
    }
    public void addCategory(String name) {
        name = name.trim(); if(name.isEmpty() || categories().contains(name)) return;
        List<String> names = categories(); names.remove(0); names.add(name);
        p.edit().putString("categories", new JSONArray(names).toString()).apply();
    }
    public JSONArray favorites() { try { return new JSONArray(p.getString("favorites", "[]")); } catch(JSONException e) { return new JSONArray(); } }
    public JSONArray history() { try { return new JSONArray(p.getString("history","[]")); } catch(JSONException e) { return new JSONArray(); } }
    public void removeContinueWatching(String animeUrl) {
        JSONArray kept=new JSONArray(),old=history();
        for(int n=0;n<old.length();n++){JSONObject item=old.optJSONObject(n);if(item!=null&&!item.optString("url").equals(animeUrl))kept.put(item);}
        p.edit().putString("history",kept.toString()).apply();
    }
    public void clearContinueWatching(){p.edit().putString("history","[]").apply();}
    public void rememberAnime(Anime anime) {
        type(anime.url(),anime.type());
        JSONArray next=new JSONArray(),old=history();
        try { next.put(new JSONObject().put("title",anime.title()).put("url",anime.url()).put("image",anime.image())); } catch(JSONException ignored) {}
        for(int n=0;n<old.length() && next.length()<40;n++) if(!old.optJSONObject(n).optString("url").equals(anime.url())) next.put(old.optJSONObject(n));
        p.edit().putString("history",next.toString()).apply();
    }
    public boolean isFavorite(String url) { JSONArray a=favorites(); for(int i=0;i<a.length();i++) if(a.optJSONObject(i).optString("url").equals(url)) return true; return false; }
    public void favorite(Anime anime, String category, boolean enabled) {
        type(anime.url(),anime.type());
        JSONArray next = new JSONArray(), old=favorites();
        for(int i=0;i<old.length();i++) if(!old.optJSONObject(i).optString("url").equals(anime.url())) next.put(old.optJSONObject(i));
        if(enabled) try { next.put(new JSONObject().put("title",anime.title()).put("url",anime.url()).put("image",anime.image()).put("category",category)); } catch(JSONException ignored) {}
        p.edit().putString("favorites",next.toString()).apply();
    }
    public void renameCategory(String oldName, String newName) {
        newName=newName.trim(); if(oldName.equals("Por defecto") || newName.isEmpty() || categories().contains(newName)) return;
        replaceCategory(oldName,newName);
    }
    public void deleteCategory(String name) { if(!name.equals("Por defecto")) replaceCategory(name,"Por defecto"); }
    private void replaceCategory(String oldName, String newName) {
        List<String> names=categories(); names.remove(0); names.remove(oldName);
        if(!newName.equals("Por defecto")) names.add(newName);
        JSONArray a=favorites(); for(int i=0;i<a.length();i++) try { if(a.getJSONObject(i).optString("category").equals(oldName)) a.getJSONObject(i).put("category",newName); } catch(JSONException ignored) {}
        p.edit().putString("categories",new JSONArray(names).toString()).putString("favorites",a.toString()).apply();
    }
}


