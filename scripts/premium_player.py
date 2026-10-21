from pathlib import Path
import sys
root=Path(sys.argv[1])/'app/src/main/java/com/myiptv/player'
main=root/'MainActivity.java'
s=main.read_text(encoding='utf-8')
s=s.replace('Color.rgb(9,7,15), PANEL=Color.rgb(26,20,39), PURPLE=Color.rgb(168,85,247), WHITE=Color.WHITE, MUTED=Color.rgb(177,165,192)','Color.rgb(7,10,18), PANEL=Color.rgb(19,30,49), PURPLE=Color.rgb(108,77,229), WHITE=Color.WHITE, MUTED=Color.rgb(172,188,215)')
s=s.replace('background(PANEL,12));pad(card,12,12);','background(PANEL,18));pad(card,16,17);')
s=s.replace('TextView play=text("▶",21,PURPLE,true);','TextView play=text("▶",25,PURPLE,true);')
s=s.replace('TextView title=text("MY IPTV",26,WHITE,true);','TextView title=text("MY IPTV",30,WHITE,true);')
main.write_text(s,encoding='utf-8')
player=root/'PlayerActivity.java'
s=player.read_text(encoding='utf-8')
if 'Player.Listener' not in s:
 s=s.replace('import androidx.media3.common.MediaItem;','import androidx.media3.common.MediaItem;\nimport androidx.media3.common.Player;\nimport androidx.media3.common.PlaybackException;')
 s=s.replace('        setContentView(root);','''        TextView status=new TextView(this);status.setText("Connecting / جارٍ الاتصال");status.setTextColor(Color.LTGRAY);status.setPadding(18,12,18,12);root.addView(status);
        Button retry=new Button(this);retry.setText("Retry playback / إعادة المحاولة");root.addView(retry);
        setContentView(root);''')
 s=s.replace('player=new ExoPlayer.Builder(this).build(); view.setPlayer(player);','''player=new ExoPlayer.Builder(this).build(); view.setPlayer(player);
        player.addListener(new Player.Listener(){
          @Override public void onPlaybackStateChanged(int state){
            if(state==Player.STATE_BUFFERING)status.setText("Buffering / جارٍ التحميل");
            else if(state==Player.STATE_READY)status.setText("Ready / جاهز");
            else if(state==Player.STATE_ENDED)status.setText("Stream ended / انتهى البث");
          }
          @Override public void onPlayerError(PlaybackException error){
            status.setText("Playback error / خطأ التشغيل: "+error.getErrorCodeName()+". Check codec, server access, credentials or required headers.");
          }
        });
        retry.setOnClickListener(v->{if(player!=null){player.prepare();player.play();}});''')
 player.write_text(s,encoding='utf-8')
print('Premium TV layout and playback diagnostics applied')
