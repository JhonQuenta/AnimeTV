package pe.animetv;
import org.junit.Test;
import static org.junit.Assert.*;
import org.jsoup.Jsoup;
import java.time.ZoneId;
import java.util.*;
import static pe.animetv.Models.*;
public class FeedTest {
    @Test public void recentEpisodesAreNotCatalogTitlesOrDuplicateLinks() {
        String html="<article><a href='/media/series/3'></a><img src='/thumbnails/1.jpg'><header>Mi serie</header></article><article><a href='/media/series'></a><header>Catalog title</header></article><article><a href='/media/series/3'></a><header>Duplicate</header></article>";
        List<RecentEpisode> feed=AnimeAv1Source.parseRecent(Jsoup.parse(html,"https://example.org")); assertEquals(1,feed.size()); assertEquals("Episodio 3",feed.get(0).episode().name()); assertEquals("https://example.org/media/series",feed.get(0).anime().url());
    }
    @Test public void calendarUsesPublicationTimeInLocalDayNotPremiereDate() {
        String script="media:[{id:1,title:\"Escaped \\\"title\\\" [test]\",slug:\"test\",startDate:\"2020-01-01\",latestEpisode:{number:7,createdAt:\"2026-10-10T02:30:00+00:00\"}}]";
        List<CalendarEntry> entries=AnimeAv1Source.parseCalendar(script,"https://example.org",ZoneId.of("America/Lima")); assertEquals(1,entries.size()); assertEquals(5,entries.get(0).day()); assertEquals("21:30",entries.get(0).label()); assertEquals("Escaped \"title\" [test]",entries.get(0).anime().title());
    }
    @Test public void missingOrInvalidScheduleIsNotInvented() { assertTrue(AnimeAv1Source.parseCalendar("media:[{id:1,title:\"Test\",slug:\"test\",latestEpisode:null}]","https://example.org",ZoneId.of("UTC")).isEmpty()); }
}
