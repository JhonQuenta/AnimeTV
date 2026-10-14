package pe.animetv;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;
public class EpisodeWindowTest {
    @Test public void boundaryDoesNotLoseOrRepeatEpisodes(){for(int count:new int[]{0,1,99,100,101,200,1151}){List<Integer> all=new ArrayList<>();for(int group=0;group<EpisodeWindow.groups(count);group++){List<Integer> part=EpisodeWindow.indices(count,group,false);assertTrue(part.size()<=100);all.addAll(part);}assertEquals(count,all.size());assertEquals(count,new HashSet<>(all).size());for(int n=0;n<count;n++)assertEquals(n,(int)all.get(n));}}
    @Test public void descendingKeepsOriginalPlaybackIndices(){assertEquals(List.of(204,203,202,201,200),EpisodeWindow.indices(205,2,true));assertEquals("201-205",EpisodeWindow.label(205,2));assertEquals(100,(int)EpisodeWindow.indices(205,1,false).get(0));}
}
