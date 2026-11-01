package com.myiptv.player;

import android.content.Context;
import org.json.*;
import java.util.*;

final class WatchHistory {
    static final class Entry {
        final JSONObject data;
        Entry(JSONObject data){this.data=data;}
        long position(){return data.optLong("position");}
        long duration(){return data.optLong("duration");}
        boolean done(){return data.optBoolean("completed");}
        boolean resumable(){return ProgressPolicy.resumable(position(),duration(),done());}
        Channel channel(){return new Channel(data.optString("name"),data.optString("url"),data.optString("group"),data.optString("logo"),data.optString("type"));}
        String parent(){return data.optString("parent");}
    }
    private final LinkedHashMap<String,Entry> entries=new LinkedHashMap<>();
    WatchHistory(Context context){try{JSONArray all=new JSONArray(Vault.read(context,"watch_history"));for(int i=0;i<all.length();i++){JSONObject e=all.getJSONObject(i);entries.put(ProgressPolicy.key(e.optString("playlist"),e.optString("url")),new Entry(e));}}catch(Exception ignored){}}
    Entry get(String playlist,String url){return entries.get(ProgressPolicy.key(playlist,url));}
    Entry forCard(String playlist,Channel channel){Entry direct=get(playlist,channel.url);if(direct!=null)return direct;Entry latest=null;for(Entry e:entries.values())if(playlist.equals(e.data.optString("playlist"))&&channel.url.equals(e.parent())&&(latest==null||e.data.optLong("updated")>latest.data.optLong("updated")))latest=e;return latest;}
    List<Entry> continuing(String playlist){ArrayList<Entry> result=new ArrayList<>();for(Entry e:entries.values())if(playlist.equals(e.data.optString("playlist"))&&e.resumable())result.add(e);result.sort((a,b)->Long.compare(b.data.optLong("updated"),a.data.optLong("updated")));return result;}
    void save(Context context,String playlist,Channel channel,String parent,long position,long duration,boolean ended) throws Exception {
        if(position<5000&&!ended)return;
        JSONObject d=new JSONObject().put("playlist",playlist).put("url",channel.url).put("name",channel.name).put("group",channel.group).put("logo",channel.logo).put("type",channel.type).put("parent",parent).put("position",Math.max(0,position)).put("duration",Math.max(0,duration)).put("completed",ProgressPolicy.completed(position,duration,ended)).put("updated",System.currentTimeMillis());
        String key=ProgressPolicy.key(playlist,channel.url);entries.remove(key);entries.put(key,new Entry(d));while(entries.size()>200)entries.remove(entries.keySet().iterator().next());persist(context);
    }
    void restart(Context context,String playlist,String url) throws Exception {entries.remove(ProgressPolicy.key(playlist,url));persist(context);}
    private void persist(Context context) throws Exception {JSONArray all=new JSONArray();for(Entry e:entries.values())all.put(e.data);Vault.save(context,"watch_history",all.toString());}
}
