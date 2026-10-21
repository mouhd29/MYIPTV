package com.myiptv.player;
import org.junit.Test;
import static org.junit.Assert.*;
import java.io.StringReader;
import java.util.List;
public class M3uParserTest {
    @Test public void importsLiveMovieSeriesAndTs() throws Exception {
        String m="#EXTM3U\n#EXTINF:-1 group-title=\"News\",News\nhttps://example.org/live.ts\n#EXTINF:-1 group-title=\"Movies\",Film\nhttps://example.org/movie.mp4\n#EXTINF:-1 group-title=\"Series\",Episode\nhttps://example.org/episode.m3u8\n";
        List<Channel> c=M3uParser.parse(new StringReader(m));assertEquals(3,c.size());assertEquals("Live TV",c.get(0).type);assertTrue(c.get(0).url.endsWith(".ts"));assertEquals("Movies",c.get(1).type);assertEquals("Series",c.get(2).type);
    }
    @Test public void ignoresCommentsAndResetsMetadata() throws Exception {
        List<Channel> c=M3uParser.parse(new StringReader("#EXTM3U\n#EXTINF:-1 group-title=\"Movies\",Film\nhttps://example.org/a\n#comment\nhttps://example.org/b.ts\n"));assertEquals("Live TV",c.get(1).type);assertEquals("Uncategorized",c.get(1).group);
    }
}
