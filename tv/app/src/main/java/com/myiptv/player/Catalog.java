package com.myiptv.player;

import org.json.*;
import java.net.*;
import java.io.*;
import java.util.*;

final class Catalog {
    static String encode(String s) throws Exception {return URLEncoder.encode(s,"UTF-8").replace("+","%20");}
    static String base(String s) throws Exception {
        URI u=new URI(s.trim());
        if(!("http".equalsIgnoreCase(u.getScheme())||"https".equalsIgnoreCase(u.getScheme()))||u.getHost()==null||u.getRawQuery()!=null||u.getUserInfo()!=null)throw new IOException("Server URL");
        String p=u.getPath()==null?"":u.getPath().replaceAll("/+$","").replaceAll("/(player_api\\.php|get\\.php)$","");
        return new URI(u.getScheme(),null,u.getHost(),u.getPort(),p,null,null).toString();
    }
    static String get(String url) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(15000);c.setReadTimeout(30000);c.setRequestProperty("User-Agent","MYIPTV/0.3");
        try {int code=c.getResponseCode();if(code<200||code>=300)throw new IOException("HTTP "+code);
            try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
                byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>40*1024*1024)throw new IOException("Catalog too large");}return out.toString("UTF-8");
            }
        } finally {c.disconnect();}
    }
    static List<Channel> playlist(String url) throws Exception {
        URI u=new URI(url);if(!"http".equalsIgnoreCase(u.getScheme())&&!"https".equalsIgnoreCase(u.getScheme()))throw new IOException("URL");
        String data=get(url);
        if(!data.replace("\uFEFF","").trim().startsWith("#EXTM3U"))throw new IOException("M3U required");
        List<Channel> items=M3uParser.parse(new StringReader(data));if(items.isEmpty())throw new IOException("Empty playlist");return items;
    }
    static String api(String b,String u,String p) throws Exception {return b+"/player_api.php?username="+encode(u)+"&password="+encode(p);}
    static List<Channel> xtream(String b,String u,String p) throws Exception {
        String api=api(b,u,p);JSONObject auth=new JSONObject(get(api));JSONObject info=auth.optJSONObject("user_info");
        if(info==null||info.optInt("auth",0)!=1||!"Active".equalsIgnoreCase(info.optString("status")))throw new IOException("Account inactive");
        ArrayList<Channel> out=new ArrayList<>();
        String[] kinds={"Live TV","Movies","Series"},actions={"live","vod","series"},paths={"live","movie","series"};
        for(int k=0;k<3;k++){
            Map<String,String> categories=new HashMap<>();
            JSONArray cats=new JSONArray(get(api+"&action=get_"+actions[k]+"_categories"));
            for(int i=0;i<cats.length();i++){JSONObject c=cats.optJSONObject(i);if(c!=null)categories.put(c.optString("category_id"),c.optString("category_name"));}
            JSONArray arr=new JSONArray(get(api+"&action=get_"+(k==2?"series":actions[k]+"_streams")));
            for(int i=0;i<arr.length();i++){
                JSONObject c=arr.optJSONObject(i);if(c==null)continue;String id=c.optString(k==2?"series_id":"stream_id");if(!id.matches("[0-9]+"))continue;
                String ext=k==0?"ts":c.optString("container_extension","mp4");if(!ext.matches("[a-zA-Z0-9]+"))ext="mp4";
                String link=k==2?"series:"+id:b+"/"+paths[k]+"/"+encode(u)+"/"+encode(p)+"/"+id+"."+ext;
                out.add(new Channel(c.optString("name","Untitled"),link,categories.getOrDefault(c.optString("category_id"),"Uncategorized"),c.optString(k==2?"cover":"stream_icon"),kinds[k]));
            }
        }
        if(out.isEmpty())throw new IOException("Empty catalog");return out;
    }
    static List<Channel> episodes(String b,String u,String p,Channel series) throws Exception {
        JSONObject data=new JSONObject(get(api(b,u,p)+"&action=get_series_info&series_id="+encode(series.url.substring(7))));
        JSONObject seasons=data.optJSONObject("episodes");if(seasons==null)throw new IOException("No episodes");
        List<String> keys=new ArrayList<>();Iterator<String> iterator=seasons.keys();while(iterator.hasNext())keys.add(iterator.next());
        keys.sort((a,z)->{try{return Integer.compare(Integer.parseInt(a),Integer.parseInt(z));}catch(Exception e){return a.compareTo(z);}});
        ArrayList<Channel> result=new ArrayList<>();
        for(String season:keys){JSONArray arr=seasons.optJSONArray(season);if(arr==null)continue;for(int i=0;i<arr.length();i++){
            JSONObject e=arr.optJSONObject(i);if(e==null)continue;String id=e.optString("id"),ext=e.optString("container_extension","mp4");if(!id.matches("[0-9]+"))continue;if(!ext.matches("[a-zA-Z0-9]+"))ext="mp4";
            result.add(new Channel("S"+season+" · "+e.optString("title","Episode "+(i+1)),b+"/series/"+encode(u)+"/"+encode(p)+"/"+id+"."+ext,series.name,series.logo,"Series"));
        }}if(result.isEmpty())throw new IOException("No episodes");return result;
    }
}
