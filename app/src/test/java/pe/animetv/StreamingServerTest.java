package pe.animetv;
import org.junit.*;import org.jsoup.Jsoup;import java.util.*;import static org.junit.Assert.*;import static pe.animetv.Models.*;
public class StreamingServerTest {
 @Test public void invalidZillaLinkIsRejected()throws Exception{try{new AnimeAv1Source().resolve(new Server("HLS","https://player.zilla-networks.com/play/test-id","SUB"),"https://animeav1.com/media/test/2");fail();}catch(java.io.IOException expected){assertTrue(expected.getMessage().contains("inválido"));}}
 @Test public void streamingNeverIncludesDownloadsEvenWhenNamesOverlap(){
  String data="embeds:{SUB:[{server:\"HLS\",url:\"https://play.example/1\"},{server:\"MP4Upload\",url:\"https://mp4.example/embed\"}],DUB:[{server:\"Voe\",url:\"https://voe.example/dub\"}]},downloads:{SUB:[{server:\"1Fichier\",url:\"https://1fichier.com/abc\"},{server:\"MP4Upload\",url:\"https://mp4.example/download\"}],DUB:[{server:\"Mega\",url:\"https://mega.nz/id\"}]}";
  List<Server> servers=AnimeAv1Source.parseServers(Jsoup.parse("<script>"+data+"</script>"));assertEquals(2,servers.size());assertEquals(1,servers.stream().filter(s->s.language().equals("SUB")).count());assertEquals("Voe",servers.get(0).name());assertFalse(servers.stream().anyMatch(s->s.url().contains("download")||s.name().equals("Mega")||s.name().equals("1Fichier")));
 }
 @Test public void deletedOrDownloadOnlyEpisodeHasNoStreamingServers(){assertTrue(AnimeAv1Source.parseServers(Jsoup.parse("<script>downloads:{SUB:[{server:\"MP4Upload\",url:\"https://mp4.example/download\"}]}</script>")).isEmpty());}
 @Test public void byseIsAnAllowedStreamingBackup(){List<Server> servers=AnimeAv1Source.parseServers(Jsoup.parse("<script>embeds:{SUB:[{server:\"Byse\",url:\"https://byse.example/e/id\"},{server:\"Voe\",url:\"https://voe.example/e/id\"},{server:\"HLS\",url:\"https://player.zilla.example/play/id\"}]}</script>"));assertEquals(List.of("Voe","Byse"),servers.stream().map(Server::name).toList());}
 @Test public void catalogTypesComeFromSourceCategoryData(){String html="<script>results:[{categoryId:2,slug:\"film\"},{categoryId:3,slug:\"ova\"}],latestMedia:[{slug:\"series\",category:{id:1,name:\"TV Anime\"}}]</script>";Map<String,String> result=AnimeAv1Source.typeMap(Jsoup.parse(html,"https://animeav1.com/catalogo"));assertEquals("Película",result.get("https://animeav1.com/media/film"));assertEquals("OVA",result.get("https://animeav1.com/media/ova"));assertEquals("Serie",result.get("https://animeav1.com/media/series"));assertNull(result.get("https://animeav1.com/media/unknown"));}
}


