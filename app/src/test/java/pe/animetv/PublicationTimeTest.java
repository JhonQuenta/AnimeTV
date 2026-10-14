package pe.animetv;
import java.time.ZoneId;import org.junit.Test;import static org.junit.Assert.*;
public class PublicationTimeTest {
    @Test public void utcMicrosecondsBecomeReadableLocalTime(){assertEquals("Episodio 2 · 12 oct 2026 · 09:52",PublicationTime.display("Episodio 2 · 2026-10-12T14:52:28.000000Z",ZoneId.of("America/Lima")));}
    @Test public void conversionHandlesPreviousCalendarDayAndStoredHistory(){assertEquals("Episodio 1 · 11 oct 2026 · 22:15",PublicationTime.display("Episodio 1 · 2026-10-12T03:15:00Z",ZoneId.of("America/Lima")));}
    @Test public void existingRelativeDatesRemainIntact(){assertEquals("Episodio 3 · hace 15 horas",PublicationTime.display("Episodio 3 · hace 15 horas",ZoneId.of("America/Lima")));}
}
