package pe.animetv;
import org.junit.Test;
import static org.junit.Assert.*;
public class Mp4UploadTest {
 @Test public void extractsDirectPlayerSourceWithQuery(){assertEquals("https://cdn.example/video.mp4?token=abc&x=1",NativeStreams.mp4uploadUrl("player.src({src: 'https://cdn.example/video.mp4?token=abc&amp;x=1',type:'video/mp4'});"));}
 @Test public void decodesPackedSourceWithoutExecutingJavascript(){String packed="eval(function(p,a,c,k,e,d){return p;}('0.1({2:\"3://4/5.6\"})',7,7,'player|src|src|https|example.org|video|mp4'.split('|'),0,{}))";assertEquals("https://example.org/video.mp4",NativeStreams.mp4uploadUrl(PackedScript.decode(packed)));}
 @Test public void missingVideoDoesNotReturnAnUnrelatedLink(){assertEquals("",NativeStreams.mp4uploadUrl("window.location='https://ads.example/';"));}
}
