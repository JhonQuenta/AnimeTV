package pe.animetv;
import org.junit.Test;import static org.junit.Assert.*;
public class SeekBurstTest {
 @Test public void threeForwardOrBackwardPressesAccumulate(){SeekBurst b=new SeekBurst();assertEquals(5,b.add(5,0));assertEquals(10,b.add(5,100));assertEquals(15,b.add(5,200));assertEquals(-5,b.add(-5,250));assertEquals(-10,b.add(-5,350));assertEquals(-15,b.add(-5,450));}
 @Test public void pauseAndNewEpisodeResetIndicator(){SeekBurst b=new SeekBurst();b.add(5,0);assertEquals(5,b.add(5,1300));b.reset();assertEquals(5,b.add(5,1310));}
}
