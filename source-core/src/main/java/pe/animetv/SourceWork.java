package pe.animetv;
import java.util.*;
import java.util.concurrent.*;

/** Bounded I/O fan-out. Nested lookups run inline to avoid exhausting the worker pool. */
final class SourceWork {
    interface Lookup<I,O>{O load(I value)throws Exception;}
    record Result<T>(T value,Exception failure){}
    private static final ThreadLocal<Boolean> INSIDE=new ThreadLocal<>();
    private static final ExecutorService WORKERS=Executors.newFixedThreadPool(3,r->{Thread t=new Thread(r,"source-lookup");t.setDaemon(true);return t;});
    private static <I,O> Result<O> run(I value,Lookup<I,O> lookup){try{return new Result<>(lookup.load(value),null);}catch(Exception e){return new Result<>(null,e);}}
    static <I,O> List<Result<O>> map(List<I> input,Lookup<I,O> lookup)throws InterruptedException {
        List<Result<O>> out=new ArrayList<>();
        if(Boolean.TRUE.equals(INSIDE.get())){for(I value:input){if(Thread.currentThread().isInterrupted())throw new InterruptedException();out.add(run(value,lookup));}return out;}
        ArrayDeque<Future<Result<O>>> pending=new ArrayDeque<>();int next=0;
        try{
            while(next<input.size()||!pending.isEmpty()){
                while(next<input.size()&&pending.size()<3){I value=input.get(next++);pending.add(WORKERS.submit(()->{INSIDE.set(true);try{return run(value,lookup);}finally{INSIDE.remove();}}));}
                Future<Result<O>> current=pending.remove();try{out.add(current.get());}catch(InterruptedException cancelled){current.cancel(true);throw cancelled;}catch(ExecutionException impossible){throw new IllegalStateException(impossible.getCause());}
            }
            return out;
        }finally{for(Future<?> task:pending)task.cancel(true);}
    }
}
