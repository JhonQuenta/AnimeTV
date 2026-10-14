package pe.animetv;
import org.junit.Test;
import static org.junit.Assert.*;
public class PackedScriptTest {
    @Test public void largeQuotedPayloadDoesNotUseRecursiveRegex(){String payload="x".repeat(30000);assertEquals(payload,PackedScript.decode("function(){}('"+payload+"',2,1,'x'.split('|'))"));}
    @Test public void dictionaryWordsAreReplacedWithoutExecutingCode() {
        String input="eval(function(p,a,c,k,e,d){return p;}('0.1({2:\"3://4/5.6\"})',7,7,'player|src|src|https|example.org|video|mp4'.split('|'),0,{}))";
        assertEquals("player.src({src:\"https://example.org/video.mp4\"})",PackedScript.decode(input));
    }
    @Test public void malformedPayloadIsLeftUntouched() { assertEquals("eval(broken)",PackedScript.decode("eval(broken)")); }
    @Test public void replacementIsSinglePass() { assertEquals("1",PackedScript.decode("function(){}('0',2,2,'1|wrong'.split('|'))")); }
}
