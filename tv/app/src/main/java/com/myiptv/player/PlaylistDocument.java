package com.myiptv.player;

import org.json.*;
import java.util.*;

/** Immutable candidate documents: a failed provider import never replaces the active playlist. */
final class PlaylistDocument {
    static JSONObject load(String raw) throws Exception {
        JSONObject doc=raw.isEmpty()?new JSONObject():new JSONObject(raw);
        if(doc.has("mode")){
            JSONObject previous=new JSONObject(doc.toString());previous.put("id",UUID.randomUUID().toString());previous.put("name","My Playlist");
            return new JSONObject().put("version",2).put("active",previous.getString("id")).put("playlists",new JSONArray().put(previous));
        }
        if(!doc.has("playlists"))doc.put("playlists",new JSONArray());return doc;
    }
    static JSONObject active(JSONObject doc){JSONArray lists=doc.optJSONArray("playlists");if(lists!=null)for(int i=0;i<lists.length();i++){JSONObject p=lists.optJSONObject(i);if(p!=null&&p.optString("id").equals(doc.optString("active")))return p;}return new JSONObject();}
    static JSONObject withPlaylist(JSONObject doc,JSONObject playlist) throws Exception {
        JSONObject next=new JSONObject(doc.toString()),item=new JSONObject(playlist.toString());
        if(item.optString("id").isEmpty())item.put("id",UUID.randomUUID().toString());
        String name=item.optString("name").trim();if(name.isEmpty()||name.length()>60)throw new IllegalArgumentException("Playlist name");item.put("name",name);
        JSONArray old=next.getJSONArray("playlists"),lists=new JSONArray();boolean replaced=false;
        for(int i=0;i<old.length();i++){JSONObject p=old.getJSONObject(i);if(p.optString("id").equals(item.getString("id"))){lists.put(item);replaced=true;}else lists.put(p);}
        if(!replaced)lists.put(item);return next.put("version",2).put("playlists",lists).put("active",item.getString("id"));
    }
}
