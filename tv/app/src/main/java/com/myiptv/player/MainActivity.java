package com.myiptv.player;

import android.content.*;
import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.*;
import androidx.recyclerview.widget.*;
import org.json.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends AppCompatActivity {
    private static final int BG=0xff080e19,PANEL=0xff142238,ACCENT=0xff27d9c2,MUTED=0xff9badc6;
    private final ArrayList<Channel> channels=new ArrayList<>(),shown=new ArrayList<>();
    private final Set<String> favorites=new HashSet<>(),recent=new HashSet<>();
    private final ExecutorService network=Executors.newSingleThreadExecutor(),images=Executors.newFixedThreadPool(3);
    private final android.util.LruCache<String,Bitmap> art=new android.util.LruCache<String,Bitmap>(8*1024*1024){protected int sizeOf(String key,Bitmap b){return b.getByteCount();}};
    private String tab="Live TV",category="",query="";
    private JSONObject connection=new JSONObject();
    private boolean busy=false;private int page=0;
    private RecyclerView grid;private LinearLayout categories;private TextView status,count;private Cards adapter;
    private boolean ar(){return getPreferences(0).getBoolean("arabic",false);}
    private String tr(String en,String arabic){return ar()?arabic:en;}
    private String label(String s){switch(s){case "Live TV":return tr("Live TV","البث المباشر");case "Movies":return tr("Movies","الأفلام");case "Series":return tr("Series","المسلسلات");case "Favorites":return tr("Favorites","المفضلة");default:return tr("Recently Added","المضاف حديثاً");}}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int c,boolean focused){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(10));if(focused)d.setStroke(dp(3),ACCENT);return d;}
    private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(size);return v;}
    private void focus(View v,int color){v.setFocusable(true);v.setBackground(bg(color,false));v.setOnFocusChangeListener((w,f)->{w.setBackground(bg(color,f));w.setElevation(f?dp(6):0);});}
    private TextView button(String s,Runnable action){TextView b=text(s,14,Color.WHITE);b.setGravity(Gravity.CENTER);b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setMinimumHeight(dp(44));focus(b,PANEL);b.setOnClickListener(v->action.run());return b;}
    private void add(LinearLayout row,View v,int width,int height){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(width,height);p.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(v,p);}
    @Override public void onCreate(Bundle state){super.onCreate(state);getWindow().setFlags(1024,1024);favorites.addAll(getPreferences(0).getStringSet("favorites",new HashSet<>()));recent.addAll(getPreferences(0).getStringSet("recent",new HashSet<>()));try{connection=new JSONObject(Vault.read(this));}catch(Exception ignored){}build();if(connection.has("mode"))connect(connection);}
    private void build(){
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(24),dp(12),dp(24),dp(10));root.setBackgroundColor(BG);root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);setContentView(root);
        LinearLayout header=new LinearLayout(this);header.setGravity(Gravity.CENTER_VERTICAL);root.addView(header);
        TextView brand=text("MY IPTV",25,ACCENT);brand.setTypeface(null,Typeface.BOLD);header.addView(brand,new LinearLayout.LayoutParams(0,-2,1));
        add(header,button(tr("Connections","الاتصالات"),this::connections),-2,dp(44));
        add(header,button(ar()?"English":"العربية",()->{getPreferences(0).edit().putBoolean("arabic",!ar()).apply();build();}),-2,dp(44));
        LinearLayout nav=new LinearLayout(this);root.addView(nav);TextView active=null;
        for(String name:new String[]{"Live TV","Movies","Series","Favorites","Recently Added"}){
            TextView b=button(label(name),()->{tab=name;category="";page=0;build();});b.setTextColor(name.equals(tab)?ACCENT:Color.WHITE);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(46),1);p.setMargins(dp(3),dp(6),dp(3),dp(6));nav.addView(b,p);if(name.equals(tab))active=b;
        }
        LinearLayout tools=new LinearLayout(this);tools.setGravity(Gravity.CENTER_VERTICAL);root.addView(tools);
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(Color.WHITE);search.setHintTextColor(MUTED);search.setTextSize(14);search.setHint(tr("Search titles…","ابحث عن عنوان…"));search.setText(query);search.setPadding(dp(12),0,dp(12),0);focus(search,PANEL);tools.addView(search,new LinearLayout.LayoutParams(0,dp(42),1));
        count=text("",12,MUTED);count.setPadding(dp(14),0,dp(8),0);tools.addView(count);
        add(tools,button(tr("Previous","السابق"),()->{if(page>0){page--;render();}}),-2,dp(42));
        add(tools,button(tr("Next","التالي"),()->{if((page+1)*60<filtered().size()){page++;render();}}),-2,dp(42));
        LinearLayout body=new LinearLayout(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView sc=new ScrollView(this);categories=new LinearLayout(this);categories.setOrientation(1);sc.addView(categories);add(body,sc,dp(160),-1);
        grid=new RecyclerView(this);grid.setLayoutManager(new GridLayoutManager(this,4));grid.setClipToPadding(false);grid.setPadding(dp(8),dp(5),dp(8),dp(5));adapter=new Cards();grid.setAdapter(adapter);body.addView(grid,new LinearLayout.LayoutParams(0,-1,1));
        status=text(busy?tr("Connecting…","جارٍ الاتصال…"):tr("Select to play • Hold OK for favorites","اختر للتشغيل • اضغط مطولاً على موافق للمفضلة"),12,MUTED);root.addView(status);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int c){query=s.toString();page=0;render();}public void afterTextChanged(Editable e){}});
        filters();render();if(active!=null)active.requestFocus();
    }
    private boolean matches(Channel c){return tab.equals("Favorites")?favorites.contains(c.url):tab.equals("Recently Added")?recent.contains(c.url):tab.equals(c.type);}
    private void filters(){categories.removeAllViews();LinkedHashSet<String> names=new LinkedHashSet<>();names.add("");for(Channel c:channels)if(matches(c))names.add(c.group);for(String g:names){TextView b=button(g.isEmpty()?tr("All categories","كل التصنيفات"):g,()->{category=g;page=0;filters();render();});b.setMaxLines(2);b.setTextColor(category.equals(g)?ACCENT:MUTED);add(categories,b,-1,-2);}}
    private ArrayList<Channel> filtered(){ArrayList<Channel> out=new ArrayList<>();String q=query.toLowerCase(Locale.ROOT);for(Channel c:channels)if(matches(c)&&(category.isEmpty()||category.equals(c.group))&&(c.name+" "+c.group).toLowerCase(Locale.ROOT).contains(q))out.add(c);return out;}
    private void render(){if(adapter==null)return;ArrayList<Channel> all=filtered();if(page*60>=all.size())page=0;shown.clear();shown.addAll(all.subList(Math.min(page*60,all.size()),Math.min((page+1)*60,all.size())));adapter.notifyDataSetChanged();grid.scrollToPosition(0);count.setText(all.size()+tr(" titles"," عنوان")+" · "+(page+1)+"/"+Math.max(1,(all.size()+59)/60));if(!busy)status.setText(channels.isEmpty()?tr("Open Connections to add Xtream Codes, M3U or a direct stream.","افتح الاتصالات لإضافة إكستريم أو M3U أو رابط بث مباشر."):all.isEmpty()?tr("No matching titles in this section.","لا توجد عناوين في هذا القسم."):tr("Select to play • Hold OK for favorites","اختر للتشغيل • اضغط مطولاً على موافق للمفضلة"));}
    private class Holder extends RecyclerView.ViewHolder {ImageView image;TextView title,subtitle;Holder(LinearLayout v,ImageView i,TextView t,TextView s){super(v);image=i;title=t;subtitle=s;}}
    private class Cards extends RecyclerView.Adapter<Holder>{
        public int getItemCount(){return shown.size();}
        public Holder onCreateViewHolder(ViewGroup parent,int type){LinearLayout card=new LinearLayout(MainActivity.this);card.setOrientation(1);card.setPadding(dp(8),dp(8),dp(8),dp(8));RecyclerView.LayoutParams p=new RecyclerView.LayoutParams(-1,dp(160));p.setMargins(dp(5),dp(5),dp(5),dp(5));card.setLayoutParams(p);focus(card,PANEL);ImageView image=new ImageView(MainActivity.this);image.setScaleType(ImageView.ScaleType.FIT_CENTER);card.addView(image,new LinearLayout.LayoutParams(-1,0,1));TextView title=text("",14,Color.WHITE);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);card.addView(title);TextView sub=text("",11,MUTED);sub.setSingleLine(true);sub.setEllipsize(TextUtils.TruncateAt.END);card.addView(sub);return new Holder(card,image,title,sub);}
        public void onBindViewHolder(Holder h,int position){Channel c=shown.get(position);h.title.setText((favorites.contains(c.url)?"★ ":"")+c.name);h.subtitle.setText(c.group);h.image.setImageResource(android.R.drawable.ic_media_play);h.image.setTag(c.logo);Bitmap cached=art.get(c.logo);if(cached!=null)h.image.setImageBitmap(cached);else if(c.logo.startsWith("https://")||c.logo.startsWith("http://")){images.execute(()->{try{HttpURLConnection con=(HttpURLConnection)new URL(c.logo).openConnection();con.setConnectTimeout(5000);con.setReadTimeout(5000);byte[] data;try(java.io.InputStream in=con.getInputStream();java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>2*1024*1024)throw new java.io.IOException();}data=out.toByteArray();}finally{con.disconnect();}BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,opt);opt.inSampleSize=1;while(opt.outWidth/opt.inSampleSize>480||opt.outHeight/opt.inSampleSize>480)opt.inSampleSize*=2;opt.inJustDecodeBounds=false;Bitmap b=BitmapFactory.decodeByteArray(data,0,data.length,opt);if(b!=null){art.put(c.logo,b);runOnUiThread(()->{if(c.logo.equals(h.image.getTag()))h.image.setImageBitmap(b);});}}catch(Exception ignored){}});}
            h.itemView.setOnClickListener(v->open(c));h.itemView.setOnLongClickListener(v->{if(!favorites.add(c.url))favorites.remove(c.url);getPreferences(0).edit().putStringSet("favorites",new HashSet<>(favorites)).apply();h.title.setText((favorites.contains(c.url)?"★ ":"")+c.name);Toast.makeText(MainActivity.this,tr("Favorites updated","تم تحديث المفضلة"),Toast.LENGTH_SHORT).show();if(tab.equals("Favorites"))render();return true;});
        }
    }
    private void open(Channel c){if(!c.url.startsWith("series:")){Intent i=new Intent(this,PlayerActivity.class);i.putExtra("name",c.name);i.putExtra("url",c.url);i.putExtra("arabic",ar());startActivity(i);return;}if(busy)return;busy=true;status.setText(tr("Loading episodes…","جارٍ تحميل الحلقات…"));network.execute(()->{try{List<Channel> episodes=Catalog.episodes(connection.optString("server"),connection.optString("user"),connection.optString("password"),c);runOnUiThread(()->{busy=false;render();String[] names=new String[episodes.size()];for(int j=0;j<names.length;j++)names[j]=episodes.get(j).name;new AlertDialog.Builder(this).setTitle(c.name).setItems(names,(d,w)->open(episodes.get(w))).setNegativeButton(tr("Close","إغلاق"),null).show();});}catch(Exception e){runOnUiThread(()->failure());}});}
    private EditText input(LinearLayout box,String hint,boolean password,String value){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);e.setTextSize(15);e.setTextColor(Color.WHITE);e.setHintTextColor(MUTED);e.setInputType(password?129:1);e.setText(value);add(box,e,-1,dp(50));return e;}
    private void connections(){if(busy)return;new AlertDialog.Builder(this).setTitle(tr("Add connection","إضافة اتصال")).setItems(new String[]{"Xtream Codes",tr("M3U / M3U8 playlist URL","رابط قائمة M3U / M3U8"),tr("Direct stream URL (TS / HLS / MP4)","رابط بث مباشر (TS / HLS / MP4)")},(d,which)->connectionForm(which)).setNegativeButton(tr("Close","إغلاق"),null).show();}
    private void connectionForm(int mode){LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(18),dp(8),dp(18),dp(8));box.setBackgroundColor(PANEL);ScrollView scroll=new ScrollView(this);scroll.addView(box);
        EditText url=input(box,mode==0?tr("Server URL, including port","رابط الخادم مع المنفذ"):tr("HTTP(S) URL","رابط HTTP(S)"),false,mode==0?connection.optString("server"):connection.optString("url"));url.setInputType(17);
        EditText user=mode==0?input(box,tr("Username","اسم المستخدم"),false,connection.optString("user")):null;
        EditText pass=mode==0?input(box,tr("Password","كلمة المرور"),true,""):null;
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(mode==0?"Xtream Codes":mode==1?"M3U / M3U8":tr("Direct stream","بث مباشر")).setView(scroll).setPositiveButton(tr("Connect","اتصال"),null).setNegativeButton(tr("Cancel","إلغاء"),null).create();dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{String address=url.getText().toString().trim();try{JSONObject next=new JSONObject();next.put("mode",mode);if(mode==0){String username=user.getText().toString().trim(),password=pass.getText().toString();if(username.isEmpty()||password.isEmpty())throw new Exception();next.put("server",Catalog.base(address)).put("user",username).put("password",password);}else{URI uri=new URI(address);if(uri.getHost()==null||!(uri.getScheme().equalsIgnoreCase("http")||uri.getScheme().equalsIgnoreCase("https")))throw new Exception();next.put("url",address);}dialog.dismiss();connect(next);}catch(Exception e){url.setError(tr("Check URL and required fields","تحقق من الرابط والحقول المطلوبة"));}}));dialog.show();}
    private void connect(JSONObject next){if(busy)return;busy=true;status.setText(tr("Connecting…","جارٍ الاتصال…"));network.execute(()->{try{int mode=next.getInt("mode");List<Channel> items=mode==0?Catalog.xtream(next.getString("server"),next.getString("user"),next.getString("password")):mode==1?Catalog.playlist(next.getString("url")):Collections.singletonList(new Channel(tr("Direct stream","بث مباشر"),next.getString("url"),tr("Direct","مباشر"),"","Live TV"));Vault.save(this,next.toString());runOnUiThread(()->{Set<String> old=new HashSet<>();for(Channel c:channels)old.add(c.url);recent.clear();for(int j=items.size()-1;j>=0&&recent.size()<100;j--)if(!old.contains(items.get(j).url))recent.add(items.get(j).url);getPreferences(0).edit().putStringSet("recent",new HashSet<>(recent)).remove("playlist").remove("xt_user").remove("xt_server").apply();channels.clear();channels.addAll(items);connection=next;busy=false;category="";query="";page=0;build();});}catch(Exception e){runOnUiThread(this::failure);}});}
    private void failure(){busy=false;render();new AlertDialog.Builder(this).setTitle(tr("Connection failed","فشل الاتصال")).setMessage(tr("Check the server URL, credentials and network. The provider may be unavailable, the account inactive, or the response invalid. Your previous catalog is retained.","تحقق من رابط الخادم وبيانات الدخول والشبكة. قد يكون الحساب غير نشط أو الخادم غير متاح أو الاستجابة غير صالحة. تم الاحتفاظ بالقائمة السابقة.")).setPositiveButton(tr("OK","حسناً"),null).show();}
    @Override protected void onDestroy(){network.shutdownNow();images.shutdownNow();super.onDestroy();}
}
