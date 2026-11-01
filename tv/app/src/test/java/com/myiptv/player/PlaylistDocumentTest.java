package com.myiptv.player;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;

public class PlaylistDocumentTest {
    @Test public void migratesExistingSingleConnectionWithoutLosingCredentials() throws Exception {
        JSONObject doc=PlaylistDocument.load(new JSONObject().put("mode",0).put("server","https://example.org").put("user","test").put("password","secret").toString());
        JSONObject active=PlaylistDocument.active(doc);assertEquals("secret",active.getString("password"));assertFalse(active.getString("id").isEmpty());assertEquals(1,doc.getJSONArray("playlists").length());
        JSONObject restarted=PlaylistDocument.load(doc.toString());assertEquals(active.getString("id"),PlaylistDocument.active(restarted).getString("id"));
    }
    @Test public void addingAndSwitchingPreservesBothListsAndOriginalUntilCommitted() throws Exception {
        JSONObject first=PlaylistDocument.withPlaylist(PlaylistDocument.load(""),new JSONObject().put("mode",1).put("name","Home").put("url","https://example.org/a.m3u"));
        String firstId=PlaylistDocument.active(first).getString("id");
        JSONObject candidate=PlaylistDocument.withPlaylist(first,new JSONObject().put("mode",1).put("name","Travel").put("url","https://example.org/b.m3u"));
        assertEquals(1,first.getJSONArray("playlists").length());assertEquals(firstId,first.getString("active"));
        assertEquals(2,candidate.getJSONArray("playlists").length());assertEquals("Travel",PlaylistDocument.active(candidate).getString("name"));
        JSONObject switched=PlaylistDocument.withPlaylist(candidate,first.getJSONArray("playlists").getJSONObject(0));assertEquals(firstId,switched.getString("active"));assertEquals(2,switched.getJSONArray("playlists").length());
    }
    @Test(expected=IllegalArgumentException.class) public void rejectsUnnamedPlaylist() throws Exception {PlaylistDocument.withPlaylist(PlaylistDocument.load(""),new JSONObject().put("name","  ").put("mode",1));}
}
