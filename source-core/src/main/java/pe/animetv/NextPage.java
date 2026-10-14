package pe.animetv;

import org.json.*;
import org.jsoup.nodes.*;
import java.io.IOException;
import java.util.*;

/** Reads React Flight JSON records from public script text without executing remote code. */
final class NextPage {
    static List<Object> records(Document document)throws Exception {
        StringBuilder flight=new StringBuilder();for(Element script:document.select("script:not([src])")){
            String text=script.data();int start=text.indexOf("self.__next_f.push(");if(start<0)continue;
            try{Object value=new JSONTokener(text.substring(start+19)).nextValue();if(value instanceof JSONArray){JSONArray array=(JSONArray)value;if(array.optInt(0)==1&&array.opt(1) instanceof String)flight.append(array.getString(1));}}catch(JSONException malformed){/* Skip non-JSON script records. */}
            if(flight.length()>8_000_000)throw new IOException("AnimeLatinoHD: datos demasiado grandes");
        }
        // Flight T records contain raw UTF-8 text of an explicit byte length. They can
        // contain newlines and the next JSON record can follow without a separator.
        byte[] bytes=flight.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
        List<Object> out=new ArrayList<>();int position=0;
        while(position<bytes.length){
            while(position<bytes.length&&(bytes[position]=='\n'||bytes[position]=='\r'))position++;
            int colon=position;while(colon<bytes.length&&bytes[colon]!=':'&&bytes[colon]!='\n')colon++;
            if(colon==bytes.length)break;
            if(bytes[colon]!=':'){position=colon+1;continue;}
            int payload=colon+1;
            if(payload<bytes.length&&bytes[payload]=='T'){
                int comma=payload+1;while(comma<bytes.length&&comma-payload<=16&&bytes[comma]!=',')comma++;
                try{
                    if(comma>=bytes.length||bytes[comma]!=',')throw new NumberFormatException();
                    long length=Long.parseLong(new String(bytes,payload+1,comma-payload-1,java.nio.charset.StandardCharsets.US_ASCII),16);
                    if(length<0||length>bytes.length-comma-1)throw new IOException("AnimeLatinoHD: bloque de texto incompleto");
                    position=comma+1+(int)length;continue;
                }catch(NumberFormatException invalid){throw new IOException("AnimeLatinoHD: longitud de texto inválida",invalid);}
            }
            int end=payload;while(end<bytes.length&&bytes[end]!='\n')end++;
            try{Object value=new JSONTokener(new String(bytes,payload,end-payload,java.nio.charset.StandardCharsets.UTF_8)).nextValue();if(value instanceof JSONArray||value instanceof JSONObject)out.add(value);}catch(JSONException ignored){}
            position=end+1;
        }
        return out;
    }
    static JSONObject find(Document document,String... keys)throws Exception {for(Object value:records(document)){JSONObject found=find(value,keys,0);if(found!=null)return found;}throw new IOException("AnimeLatinoHD: faltan datos de "+String.join("/",keys));}
    private static JSONObject find(Object value,String[] keys,int depth)throws JSONException {
        if(depth>32)return null;if(value instanceof JSONObject){JSONObject o=(JSONObject)value;boolean match=true;for(String key:keys)match&=o.has(key);if(match)return o;Iterator<String> names=o.keys();while(names.hasNext()){JSONObject found=find(o.get(names.next()),keys,depth+1);if(found!=null)return found;}}
        else if(value instanceof JSONArray){JSONArray a=(JSONArray)value;for(int n=0;n<a.length();n++){JSONObject found=find(a.get(n),keys,depth+1);if(found!=null)return found;}}return null;
    }
}
