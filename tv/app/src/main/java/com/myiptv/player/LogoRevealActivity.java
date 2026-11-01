package com.myiptv.player;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

/** Offline branded startup. Completion, skip, Back, timeout and playback errors share one handoff. */
public class LogoRevealActivity extends AppCompatActivity {
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Runnable timeout=this::continueToApp;
    private ExoPlayer player;
    private PlayerView video;
    private long position=0;
    private boolean finished=false;
    private int dp(int value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
    private boolean arabic(){return getIntent().getBooleanExtra("arabic",getSharedPreferences("MainActivity",0).getBoolean("arabic",false));}
    private void immersive(){WindowCompat.setDecorFitsSystemWindows(getWindow(),false);WindowInsetsControllerCompat controls=WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView());controls.hide(WindowInsetsCompat.Type.systemBars());controls.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);if(state!=null)position=state.getLong("position",0);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(0xff050b15);
        video=new PlayerView(this);video.setUseController(false);video.setShutterBackgroundColor(0xff050b15);root.addView(video,new FrameLayout.LayoutParams(-1,-1));
        TextView skip=new TextView(this);skip.setText(arabic()?"تخطي المقدمة ›":"Skip Intro ›");skip.setTextColor(Color.WHITE);skip.setTextSize(14);skip.setGravity(Gravity.CENTER);skip.setFocusable(true);skip.setPadding(dp(18),dp(10),dp(18),dp(10));skip.setBackgroundColor(0xb0203048);
        skip.setOnFocusChangeListener((v,focused)->{v.setBackgroundColor(focused?0xff306cba:0xb0203048);});skip.setOnClickListener(v->continueToApp());
        FrameLayout.LayoutParams button=new FrameLayout.LayoutParams(-2,dp(46),Gravity.BOTTOM|Gravity.END);button.setMargins(dp(24),dp(20),dp(24),dp(22));root.addView(skip,button);
        setContentView(root);immersive();skip.requestFocus();
    }
    @Override protected void onStart(){
        super.onStart();if(finished)return;
        player=new ExoPlayer.Builder(this).build();video.setPlayer(player);player.setVolume(.6f);
        player.addListener(new Player.Listener(){
            @Override public void onPlaybackStateChanged(int state){if(state==Player.STATE_ENDED)continueToApp();}
            @Override public void onPlayerError(PlaybackException error){continueToApp();}
        });
        player.setMediaItem(MediaItem.fromUri("android.resource://"+getPackageName()+"/"+R.raw.logo_reveal));player.seekTo(position);player.prepare();player.play();handler.postDelayed(timeout,15000);
    }
    private void continueToApp(){if(finished||isFinishing()||isDestroyed())return;finished=true;handler.removeCallbacks(timeout);if(!getIntent().getBooleanExtra("preview",false))startActivity(new Intent(this,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP));finish();}
    @Override public void onBackPressed(){continueToApp();}
    @Override public void onWindowFocusChanged(boolean focus){super.onWindowFocusChanged(focus);if(focus)immersive();}
    @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putLong("position",player==null?position:player.getCurrentPosition());}
    @Override protected void onStop(){handler.removeCallbacks(timeout);if(player!=null){position=player.getCurrentPosition();ExoPlayer releasing=player;player=null;video.setPlayer(null);releasing.release();}super.onStop();}
    @Override protected void onDestroy(){handler.removeCallbacksAndMessages(null);super.onDestroy();}
}
