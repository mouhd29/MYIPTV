package com.myiptv.player;

final class ProgressPolicy {
    static boolean completed(long position,long duration,boolean ended){
        return ended || (duration>0 && position>=duration-60000 && (double)position/duration>=.95);
    }
    static boolean resumable(long position,long duration,boolean completed){
        return !completed && position>=10000 && (duration<=0 || position<duration);
    }
    static String time(long milliseconds){long seconds=Math.max(0,milliseconds)/1000;return String.format(java.util.Locale.ROOT,"%d:%02d:%02d",seconds/3600,(seconds/60)%60,seconds%60);}
    static String key(String playlist,String url){try{byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest((playlist+"\n"+url).getBytes("UTF-8"));StringBuilder out=new StringBuilder();for(byte b:digest){out.append(Character.forDigit((b>>4)&15,16));out.append(Character.forDigit(b&15,16));}return out.toString();}catch(Exception e){throw new IllegalStateException(e);}}
}
