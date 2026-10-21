package com.myiptv.player;
import android.os.Bundle;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
public class PlayerActivity extends AppCompatActivity {
    private ExoPlayer player;private PlayerView view;private TextView status;private long position=0;private boolean playing=true;
    private String tr(String en,String ar){return getIntent().getBooleanExtra("arabic",false)?ar:en;}
    @Override public void onCreate(Bundle saved){super.onCreate(saved);if(saved!=null){position=saved.getLong("position");playing=saved.getBoolean("playing",true);}getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_FULLSCREEN);
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(Color.BLACK);
        LinearLayout bar=new LinearLayout(this);root.addView(bar);Button back=new Button(this);back.setText(tr("Back","رجوع"));bar.addView(back);back.setOnClickListener(v->finish());TextView title=new TextView(this);title.setText(getIntent().getStringExtra("name"));title.setTextColor(Color.WHITE);title.setTextSize(18);title.setGravity(Gravity.CENTER_VERTICAL);bar.addView(title,new LinearLayout.LayoutParams(0,-1,1));Button retry=new Button(this);retry.setText(tr("Retry","إعادة المحاولة"));bar.addView(retry);retry.setOnClickListener(v->{if(player!=null){player.prepare();player.play();}});
        view=new PlayerView(this);root.addView(view,new LinearLayout.LayoutParams(-1,0,1));status=new TextView(this);status.setTextColor(Color.LTGRAY);status.setPadding(16,4,16,4);root.addView(status);setContentView(root);view.requestFocus();
    }
    @Override protected void onStart(){super.onStart();String url=getIntent().getStringExtra("url");if(url==null||url.isEmpty()){finish();return;}player=new ExoPlayer.Builder(this).build();view.setPlayer(player);player.addListener(new Player.Listener(){public void onPlaybackStateChanged(int state){status.setText(state==Player.STATE_BUFFERING?tr("Buffering…","جارٍ التحميل…"):state==Player.STATE_ENDED?tr("Stream ended","انتهى البث"):"");}public void onPlayerError(PlaybackException e){status.setText(tr("Playback failed: ","تعذر التشغيل: ")+e.getErrorCodeName()+tr(" · Check server access, format or device codec support."," · تحقق من الخادم وصيغة البث ودعم الترميز على الجهاز."));}});player.setMediaItem(MediaItem.fromUri(url));player.seekTo(position);player.prepare();player.setPlayWhenReady(playing);}
    @Override public boolean dispatchKeyEvent(KeyEvent e){return view!=null&&view.dispatchKeyEvent(e)||super.dispatchKeyEvent(e);}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putLong("position",player==null?position:player.getCurrentPosition());b.putBoolean("playing",player==null?playing:player.getPlayWhenReady());}
    @Override protected void onStop(){if(player!=null){position=player.isCurrentMediaItemLive()?0:player.getCurrentPosition();playing=player.getPlayWhenReady();view.setPlayer(null);player.release();player=null;}super.onStop();}
}
