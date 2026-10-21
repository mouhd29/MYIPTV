package com.myiptv.player;

import android.os.Bundle;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends AppCompatActivity {
    private ExoPlayer player;
    private PlayerView view;
    private TextView status;
    private LinearLayout toolbar;
    private Button retry,picture;
    private long position=0;
    private boolean playing=true,failed=false,pictureDialog=false;
    private int pictureMode;
    private String tr(String en,String ar){return getIntent().getBooleanExtra("arabic",false)?ar:en;}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private void immersive(){getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION|View.SYSTEM_UI_FLAG_LAYOUT_STABLE);}
    private Button action(String text){Button b=new Button(this);b.setText(text);b.setAllCaps(false);b.setTextSize(13);b.setTextColor(Color.WHITE);b.setBackgroundTintList(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_focused},new int[]{}},new int[]{0xff456126,0xff1b2538}));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-2,dp(44));p.setMargins(dp(4),0,dp(4),0);b.setLayoutParams(p);return b;}
    @Override public void onCreate(Bundle saved){
        super.onCreate(saved);if(saved!=null){position=saved.getLong("position");playing=saved.getBoolean("playing",true);}
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON|WindowManager.LayoutParams.FLAG_FULLSCREEN);immersive();
        FrameLayout root=new FrameLayout(this);root.setBackgroundColor(Color.BLACK);
        view=new PlayerView(this);view.setControllerAutoShow(false);view.setControllerHideOnTouch(true);view.setControllerShowTimeoutMs(4000);view.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);root.addView(view,new FrameLayout.LayoutParams(-1,-1));
        pictureMode=getSharedPreferences("player",0).getInt("pictureMode",AspectRatioFrameLayout.RESIZE_MODE_FIT);applyPicture();
        toolbar=new LinearLayout(this);toolbar.setGravity(Gravity.CENTER_VERTICAL);toolbar.setPadding(dp(14),dp(8),dp(14),dp(8));toolbar.setBackgroundColor(0xdd080d18);FrameLayout.LayoutParams bar=new FrameLayout.LayoutParams(-1,dp(64),Gravity.TOP);root.addView(toolbar,bar);
        if(getIntent().getBooleanExtra("arabic",false))toolbar.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        Button back=action(tr("Back","رجوع"));toolbar.addView(back);back.setOnClickListener(v->finish());
        TextView title=new TextView(this);title.setText(getIntent().getStringExtra("name"));title.setTextColor(Color.WHITE);title.setTextSize(16);title.setSingleLine(true);title.setEllipsize(android.text.TextUtils.TruncateAt.END);toolbar.addView(title,new LinearLayout.LayoutParams(0,-2,1));
        picture=action(tr("Picture size","حجم الصورة"));toolbar.addView(picture);picture.setOnClickListener(v->choosePicture());
        retry=action(tr("Retry","إعادة المحاولة"));toolbar.addView(retry);retry.setVisibility(View.GONE);retry.setOnClickListener(v->{if(player!=null){failed=false;retry.setVisibility(View.GONE);status.setVisibility(View.GONE);view.setControllerShowTimeoutMs(4000);view.hideController();player.prepare();player.play();}});
        status=new TextView(this);status.setTextColor(Color.WHITE);status.setTextSize(15);status.setGravity(Gravity.CENTER);status.setPadding(dp(24),dp(12),dp(24),dp(12));status.setBackgroundColor(0xcc080d18);FrameLayout.LayoutParams notice=new FrameLayout.LayoutParams(-1,-2,Gravity.CENTER);root.addView(status,notice);status.setVisibility(View.GONE);
        view.setControllerVisibilityListener((PlayerView.ControllerVisibilityListener)visibility->{toolbar.setVisibility(visibility);if(visibility!=View.VISIBLE){view.requestFocus();immersive();}});
        setContentView(root);toolbar.setVisibility(View.GONE);view.hideController();view.requestFocus();
    }
    private void applyPicture(){view.setResizeMode(pictureMode);}
    private void choosePicture(){
        pictureDialog=true;view.setControllerShowTimeoutMs(0);
        String[] labels={tr("Fit — show whole picture","ملاءمة — إظهار الصورة كاملة"),tr("Fill screen — crop edges","ملء الشاشة — قص الأطراف"),tr("Stretch — fill without black bars","تمديد — ملء الشاشة دون حواف سوداء")};
        int selected=pictureMode==AspectRatioFrameLayout.RESIZE_MODE_ZOOM?1:pictureMode==AspectRatioFrameLayout.RESIZE_MODE_FILL?2:0;
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(tr("Picture size","حجم الصورة")).setSingleChoiceItems(labels,selected,(d,index)->{pictureMode=index==1?AspectRatioFrameLayout.RESIZE_MODE_ZOOM:index==2?AspectRatioFrameLayout.RESIZE_MODE_FILL:AspectRatioFrameLayout.RESIZE_MODE_FIT;applyPicture();getSharedPreferences("player",0).edit().putInt("pictureMode",pictureMode).apply();d.dismiss();}).setNegativeButton(tr("Close","إغلاق"),null).create();
        dialog.setOnDismissListener(d->{pictureDialog=false;view.setControllerShowTimeoutMs(failed?0:4000);view.hideController();immersive();});dialog.show();
    }
    @Override protected void onStart(){
        super.onStart();String url=getIntent().getStringExtra("url");if(url==null||url.isEmpty()){finish();return;}
        player=new ExoPlayer.Builder(this).build();view.setPlayer(player);
        player.addListener(new Player.Listener(){
            @Override public void onPlaybackStateChanged(int state){
                if(state==Player.STATE_READY){failed=false;retry.setVisibility(View.GONE);status.setVisibility(View.GONE);view.setControllerShowTimeoutMs(4000);}
                else if(state==Player.STATE_ENDED){status.setText(tr("Stream ended. Press OK for controls.","انتهى البث. اضغط موافق لإظهار أدوات التحكم."));status.setVisibility(View.VISIBLE);}
            }
            @Override public void onPlayerError(PlaybackException e){failed=true;status.setText(tr("Playback failed: ","تعذر التشغيل: ")+e.getErrorCodeName()+tr(" · Check the connection or try again."," · تحقق من الاتصال أو أعد المحاولة."));status.setVisibility(View.VISIBLE);retry.setVisibility(View.VISIBLE);view.setControllerShowTimeoutMs(0);view.showController();retry.requestFocus();}
        });
        player.setMediaItem(MediaItem.fromUri(url));player.seekTo(position);player.prepare();player.setPlayWhenReady(playing);view.hideController();toolbar.setVisibility(View.GONE);
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        int key=event.getKeyCode();
        if(key==KeyEvent.KEYCODE_MENU){if(event.getAction()==KeyEvent.ACTION_DOWN){view.showController();picture.requestFocus();}return true;}
        boolean navigation=key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT;
        if(navigation&&!pictureDialog&&toolbar.getVisibility()!=View.VISIBLE){if(event.getAction()==KeyEvent.ACTION_DOWN){view.showController();if(key==KeyEvent.KEYCODE_DPAD_UP)picture.requestFocus();}return true;}
        if(view!=null&&view.dispatchMediaKeyEvent(event))return true;
        return super.dispatchKeyEvent(event);
    }
    @Override public void onBackPressed(){if(toolbar.getVisibility()==View.VISIBLE){view.hideController();}else super.onBackPressed();}
    @Override public void onWindowFocusChanged(boolean focused){super.onWindowFocusChanged(focused);if(focused)immersive();}
    @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putLong("position",player==null?position:player.getCurrentPosition());b.putBoolean("playing",player==null?playing:player.getPlayWhenReady());}
    @Override protected void onStop(){if(player!=null){position=player.isCurrentMediaItemLive()?0:player.getCurrentPosition();playing=player.getPlayWhenReady();view.setPlayer(null);player.release();player=null;}super.onStop();}
}
