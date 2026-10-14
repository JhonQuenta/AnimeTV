package pe.animetv;
import org.junit.Test;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.Assert.*;
public class TimedCacheTest {
    @Test public void expiresAndDoesNotCacheFailures()throws Exception{AtomicLong time=new AtomicLong();TimedCache<String> cache=new TimedCache<>(2,100,time::get);AtomicInteger count=new AtomicInteger();assertEquals("1",cache.get("a",()->""+count.incrementAndGet()));assertEquals("1",cache.get("a",()->"bad"));time.set(101);assertEquals("2",cache.get("a",()->""+count.incrementAndGet()));try{cache.get("b",()->{throw new java.io.IOException("network");});fail();}catch(java.io.IOException expected){}assertEquals("ok",cache.get("b",()->"ok"));cache.remove("a");assertEquals("fresh",cache.get("a",()->"fresh"));}
    @Test public void concurrentRequestsShareExtraction()throws Exception{TimedCache<String> cache=new TimedCache<>(2,10000);ExecutorService pool=Executors.newFixedThreadPool(2);CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);AtomicInteger count=new AtomicInteger();try{Future<String> first=pool.submit(()->cache.get("same",()->{count.incrementAndGet();entered.countDown();release.await();return "video";}));assertTrue(entered.await(2,TimeUnit.SECONDS));Future<String> second=pool.submit(()->cache.get("same",()->{count.incrementAndGet();return "duplicate";}));release.countDown();assertEquals("video",first.get(2,TimeUnit.SECONDS));assertEquals("video",second.get(2,TimeUnit.SECONDS));assertEquals(1,count.get());}finally{release.countDown();pool.shutdownNow();}}
}
