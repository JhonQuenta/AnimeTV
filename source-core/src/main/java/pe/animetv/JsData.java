package pe.animetv;

import java.util.*;
import java.util.regex.*;

/** Reads bounded data literals without executing remote JavaScript. */
public final class JsData {
    private JsData() {}
    public static String array(String input,String key) { return container(input,key,'[',']'); }
    public static String object(String input,String key) { return container(input,key,'{','}'); }
    private static String container(String input,String key,char opening,char closing) {
        Matcher keyMatch=Pattern.compile("\\b"+Pattern.quote(key)+"\\s*[:=]\\s*"+Pattern.quote(String.valueOf(opening))).matcher(input);
        if(!keyMatch.find())return ""; int start=keyMatch.end()-1; int end=closingAt(input,start,opening,closing); return end<0?"":input.substring(start+1,end);
    }
    private static int closingAt(String input,int start,char opening,char closing) {
        int depth=0; char quote=0; boolean escape=false;
        for(int n=start;n<input.length();n++) { char c=input.charAt(n);
            if(quote!=0) { if(escape)escape=false; else if(c=='\\')escape=true; else if(c==quote)quote=0; continue; }
            if(c=='\"' || c=='\'') { quote=c; continue; }
            if(c==opening)depth++; if(c==closing && --depth==0)return n;
        }
        return -1;
    }
    public static List<String> objects(String array) {
        List<String> out=new ArrayList<>(); for(int n=0;n<array.length();n++) if(array.charAt(n)=='{') { int end=closingAt(array,n,'{','}'); if(end<0)break; out.add(array.substring(n,end+1)); n=end; } return out;
    }
    public static String value(String input,String key) {
        Matcher m=Pattern.compile("\\b"+Pattern.quote(key)+"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"").matcher(input);
        if(!m.find())return ""; String value=m.group(1); StringBuilder out=new StringBuilder();
        for(int n=0;n<value.length();n++) { char c=value.charAt(n); if(c!='\\' || n+1>=value.length()) { out.append(c); continue; }
            char e=value.charAt(++n); if(e=='n')out.append('\n'); else if(e=='r')out.append('\r'); else if(e=='t')out.append('\t'); else if(e=='u' && n+4<value.length()) { try { out.append((char)Integer.parseInt(value.substring(n+1,n+5),16)); n+=4; } catch(NumberFormatException malformed) { out.append(e); } } else out.append(e);
        } return out.toString();
    }
    public static String number(String input,String key) { Matcher m=Pattern.compile("\\b"+Pattern.quote(key)+"\\s*:\\s*(\\d+(?:\\.\\d+)?)").matcher(input); return m.find()?m.group(1):""; }
}
