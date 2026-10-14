package pe.animetv;

import java.util.regex.*;

/** Decodes the common dictionary transport format; never evaluates JavaScript. */
public final class PackedScript {
    private static final Pattern PACKED_START=Pattern.compile("\\}\\s*\\(\\s*'");
    private static final String DIGITS="0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static String decode(String script) {
        if(script.length()>500000)return script;
        Matcher match=PACKED_START.matcher(script); if(!match.find())return script;
        try {
            Reader reader=new Reader(script,match.end()-1);String payload=unescape(reader.quoted());reader.take(',');int radix=reader.number();reader.take(',');int count=reader.number();reader.take(',');String words=reader.quoted();
            reader.skip();if(!script.startsWith(".split",reader.position))return script;
            if(radix<2 || radix>62 || count>10000)return script;
            String[] dictionary=unescape(words).split("\\|",-1);
            // Single pass: replacements themselves are not treated as encoded tokens.
            Matcher token=Pattern.compile("\\b[0-9a-zA-Z]+\\b").matcher(payload); StringBuffer out=new StringBuffer();
            while(token.find()) { long number=0; boolean valid=true;
                for(char c:token.group().toCharArray()) { int digit=DIGITS.indexOf(c); if(digit<0 || digit>=radix) { valid=false; break; } number=number*radix+digit; if(number>=count) { valid=false; break; } }
                String replacement=valid && number<dictionary.length && !dictionary[(int)number].isEmpty()?dictionary[(int)number]:token.group();
                token.appendReplacement(out,Matcher.quoteReplacement(replacement));
            }
            token.appendTail(out); return out.toString();
        } catch(RuntimeException malformed) { return script; }
    }
    /** Iterative quoted-literal reader avoids regex recursion on large Lulu dictionaries. */
    private static final class Reader {
        final String source;int position;
        Reader(String source,int position){this.source=source;this.position=position;}
        void skip(){while(position<source.length()&&Character.isWhitespace(source.charAt(position)))position++;}
        void take(char expected){skip();if(position>=source.length()||source.charAt(position++)!=expected)throw new IllegalArgumentException();}
        String quoted(){take('\'');int start=position;while(position<source.length()){char c=source.charAt(position++);if(c=='\\'){if(position<source.length())position++;}else if(c=='\'')return source.substring(start,position-1);}throw new IllegalArgumentException();}
        int number(){skip();int start=position;while(position<source.length()&&Character.isDigit(source.charAt(position)))position++;return Integer.parseInt(source.substring(start,position));}
    }
    private static String unescape(String value) { return value.replace("\\'","'").replace("\\/","/").replace("\\\\","\\"); }
}
