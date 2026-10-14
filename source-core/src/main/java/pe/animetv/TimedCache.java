package pe.animetv;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.LongSupplier;

/** Small memory-only cache; concurrent consumers share one extraction. Failures are never cached. */
final class TimedCache<T> {
    private final Map<String,Entry<T>> entries=new LinkedHashMap<>(16,.75f,true);
    private final int capacity;private final long ttl;private final LongSupplier clock;
    private record Entry<T>(long time,FutureTask<T> task){}
    TimedCache(int capacity,long ttl){this(capacity,ttl,()->System.nanoTime()/1_000_000);}
    TimedCache(int capacity,long ttl,LongSupplier clock){this.capacity=capacity;this.ttl=ttl;this.clock=clock;}
    T get(String key,Callable<T> loader)throws Exception{
        Entry<T> entry;
        synchronized(this){entry=entries.get(key);if(entry==null||(entry.task.isDone()&&clock.getAsLong()-entry.time>=ttl)){entry=new Entry<>(clock.getAsLong(),new FutureTask<>(loader));entries.put(key,entry);while(entries.size()>capacity){Iterator<String> i=entries.keySet().iterator();i.next();i.remove();}}}
        entry.task.run();try{return entry.task.get();}catch(ExecutionException e){synchronized(this){if(entries.get(key)==entry)entries.remove(key);}Throwable cause=e.getCause();if(cause instanceof Exception)throw (Exception)cause;throw new RuntimeException(cause);}
    }
    synchronized void remove(String key){entries.remove(key);}
    synchronized void put(String key,T value){FutureTask<T> task=new FutureTask<>(()->value);task.run();entries.put(key,new Entry<>(clock.getAsLong(),task));while(entries.size()>capacity){Iterator<String> i=entries.keySet().iterator();i.next();i.remove();}}
}
