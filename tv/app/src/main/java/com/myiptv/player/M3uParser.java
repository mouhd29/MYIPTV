package com.myiptv.player;

import java.io.*;
import java.util.*;
import java.util.regex.*;

public final class M3uParser {
    private static final Pattern ATTRIBUTE = Pattern.compile("([\\w-]+)\\s*=\\s*\"([^\"]*)\"");
    private M3uParser() {}
    public static List<Channel> parse(Reader input) throws IOException {
        List<Channel> result = new ArrayList<>();
        BufferedReader reader = new BufferedReader(input);
        String line, name="", group="Uncategorized", logo="", type="Live TV";
        while ((line=reader.readLine())!=null) {
            line=line.trim();
            if (line.startsWith("#EXTINF")) {
                Map<String,String> attrs=new HashMap<>();
                Matcher m=ATTRIBUTE.matcher(line);
                while(m.find()) attrs.put(m.group(1).toLowerCase(Locale.ROOT),m.group(2));
                boolean quoted=false;int comma=-1;for(int i=0;i<line.length();i++){char ch=line.charAt(i);if(ch=='"')quoted=!quoted;else if(ch==','&&!quoted){comma=i;break;}}
                name=comma>=0 ? line.substring(comma+1).trim() : "Untitled";
                group=attrs.getOrDefault("group-title","Uncategorized");
                logo=attrs.getOrDefault("tvg-logo","");
                String explicit=attrs.getOrDefault("content-type",attrs.getOrDefault("type",""));
                String hint=(explicit+" "+group).toLowerCase(Locale.ROOT);
                if (hint.contains("series") || hint.contains("episode") || hint.contains("show") || hint.contains("مسلسل") || hint.contains("مسلسلات")) type="Series";
                else if (hint.contains("movie") || hint.contains("film") || hint.contains("vod") || hint.contains("أفلام") || hint.contains("افلام")) type="Movies";
                else type="Live TV";
            } else if (line.startsWith("#EXTGRP:")) {
                group=line.substring(8).trim();
            } else if (!line.isEmpty() && !line.startsWith("#")) {
                if (line.startsWith("http://") || line.startsWith("https://") || line.startsWith("rtsp://")) {
                    result.add(new Channel(name.isEmpty()?"Stream "+(result.size()+1):name,line,group,logo,type));
                }
                name=""; group="Uncategorized"; logo=""; type="Live TV";
            }
        }
        return result;
    }
}
