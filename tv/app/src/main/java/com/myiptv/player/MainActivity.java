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
    private static final int BG=0xff080b13,PANEL=0xff171e30,ACCENT=0xff96d52d,MUTED=0xffa3b0c8;
    private final ArrayList<Channel> channels=new ArrayList<>(),shown=new ArrayList<>();
    private final Set<String> favorites=new HashSet<>(),recent=new HashSet<>();
    private final ExecutorService network=Executors.newSingleThreadExecutor(),images=Executors.newFixedThreadPool(3);
    private final android.util.LruCache<String,Bitmap> art=new android.util.LruCache<String,Bitmap>(8*1024*1024){protected int sizeOf(String key,Bitmap b){return b.getByteCount();}};
    private String tab="Live TV",category="",query="";
    private JSONObject connection=new JSONObject(),document=new JSONObject();
    private WatchHistory history;
    private Bitmap backgroundPhoto;
    private String lastFocusedUrl="";
    private boolean busy=false,home=true;private int page=0;
    private RecyclerView grid;private LinearLayout categories;private TextView status,count;private Cards adapter;
    private boolean ar(){return getPreferences(0).getBoolean("arabic",false);}
    private String tr(String en,String arabic){return ar()?arabic:en;}
    private String label(String s){switch(s){case "Live TV":return tr("Live TV","البث المباشر");case "Movies":return tr("Movies","الأفلام");case "Series":return tr("Series","المسلسلات");case "Favorites":return tr("Favorites","المفضلة");case "Continue Watching":return tr("Continue Watching","متابعة المشاهدة");default:return tr("Recently Added","المضاف حديثاً");}}
    private String identity(String url){return ProgressPolicy.key(connection.optString("id"),url);}
    private String legacyIdentity(String url){try{byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest(url.getBytes("UTF-8"));StringBuilder out=new StringBuilder();for(byte b:digest){out.append(Character.forDigit((b>>4)&15,16));out.append(Character.forDigit(b&15,16));}return out.toString();}catch(Exception e){return "";}}
    private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
    private GradientDrawable bg(int c,boolean focused){GradientDrawable d=new GradientDrawable();d.setColor(c);d.setCornerRadius(dp(10));if(focused)d.setStroke(dp(3),ACCENT);return d;}
    private TextView text(String s,int size,int color){TextView v=new TextView(this);v.setText(s);v.setTextColor(color);v.setTextSize(size);return v;}
    private void focus(View v,int color){v.setFocusable(true);v.setBackground(bg(color,false));v.setOnFocusChangeListener((w,f)->{w.setBackground(bg(color,f));w.setElevation(f?dp(6):0);});}
    private TextView button(String s,Runnable action){TextView b=text(s,14,Color.WHITE);b.setGravity(Gravity.CENTER);b.setPadding(dp(12),dp(8),dp(12),dp(8));b.setMinimumHeight(dp(44));focus(b,PANEL);b.setOnClickListener(v->action.run());return b;}
    private void add(LinearLayout row,View v,int width,int height){LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(width,height);p.setMargins(dp(3),dp(3),dp(3),dp(3));row.addView(v,p);}
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().setFlags(1024,1024);
        favorites.addAll(getPreferences(0).getStringSet("favorites",new HashSet<>()));
        try{String saved=Vault.read(this);document=PlaylistDocument.load(saved);connection=PlaylistDocument.active(document);if(!saved.isEmpty()&&new JSONObject(saved).has("mode"))Vault.save(this,document.toString());}catch(Exception e){try{document=PlaylistDocument.load("");}catch(Exception ignored){}}
        history=new WatchHistory(this);build();loadSavedBackground();if(connection.has("mode"))connect(connection);
    }
    @Override protected void onResume(){
        super.onResume();if(history==null)return;history=new WatchHistory(this);
        if(home)build();else{render();restoreCardFocus();}
    }
    private void build(){
        if(home){showHome();return;}
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(24),dp(12),dp(24),dp(10));root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff19264a,BG,0xff07090e}));root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);setScreen(root);
        FrameLayout header=new FrameLayout(this);root.addView(header,new LinearLayout.LayoutParams(-1,dp(92)));
        ImageView brand=new ImageView(this);brand.setImageResource(R.drawable.myiptv_logo);brand.setScaleType(ImageView.ScaleType.FIT_CENTER);brand.setContentDescription("MY IPTV");FrameLayout.LayoutParams logoParams=new FrameLayout.LayoutParams(dp(170),dp(90),Gravity.CENTER);header.addView(brand,logoParams);
        LinearLayout settings=new LinearLayout(this);settings.setGravity(Gravity.CENTER_VERTICAL);FrameLayout.LayoutParams settingsParams=new FrameLayout.LayoutParams(-2,-2,Gravity.END|Gravity.CENTER_VERTICAL);header.addView(settings,settingsParams);
        add(settings,button(tr("Playlists","قوائم التشغيل"),this::connections),-2,dp(40));
        add(settings,button(ar()?"English":"العربية",()->{getPreferences(0).edit().putBoolean("arabic",!ar()).apply();build();}),-2,dp(40));
        TextView welcome=button(tr("‹ Home","الرئيسية ›"),()->{home=true;build();});FrameLayout.LayoutParams welcomeParams=new FrameLayout.LayoutParams(dp(125),dp(44),Gravity.START|Gravity.CENTER_VERTICAL);header.addView(welcome,welcomeParams);
        LinearLayout nav=new LinearLayout(this);root.addView(nav);TextView active=null;
        for(String name:new String[]{"Live TV","Movies","Series","Continue Watching","Favorites","Recently Added"}){
            TextView b=button(label(name),()->{tab=name;category="";page=0;build();});boolean primary=name.equals("Live TV")||name.equals("Movies")||name.equals("Series");String symbol=name.equals("Live TV")?"▣":name.equals("Movies")?"▶":name.equals("Series")?"▤":name.equals("Favorites")?"★":"＋";b.setText(symbol+"  "+label(name));b.setTextSize(primary?18:13);b.setTypeface(null,Typeface.BOLD);b.setTextColor(name.equals(tab)?ACCENT:Color.WHITE);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(62),primary?1.25f:.85f);p.setMargins(dp(3),dp(6),dp(3),dp(6));nav.addView(b,p);if(name.equals(tab))active=b;
        }
        LinearLayout tools=new LinearLayout(this);tools.setGravity(Gravity.CENTER_VERTICAL);root.addView(tools);
        EditText search=new EditText(this);search.setSingleLine(true);search.setTextColor(Color.WHITE);search.setHintTextColor(MUTED);search.setTextSize(14);search.setHint(tr("Search titles…","ابحث عن عنوان…"));search.setText(query);search.setPadding(dp(12),0,dp(12),0);focus(search,PANEL);tools.addView(search,new LinearLayout.LayoutParams(0,dp(42),1));
        count=text("",12,MUTED);count.setPadding(dp(14),0,dp(8),0);tools.addView(count);
        add(tools,button(tr("Previous","السابق"),()->{if(page>0){page--;render();}}),-2,dp(42));
        add(tools,button(tr("Next","التالي"),()->{if((page+1)*60<filtered().size()){page++;render();}}),-2,dp(42));
        LinearLayout body=new LinearLayout(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
        ScrollView sc=new ScrollView(this);categories=new LinearLayout(this);categories.setOrientation(1);sc.addView(categories);add(body,sc,dp(148),-1);
        grid=new RecyclerView(this);grid.setLayoutManager(new GridLayoutManager(this,tab.equals("Live TV")?4:5));grid.setClipToPadding(false);grid.setPadding(dp(8),dp(5),dp(8),dp(5));adapter=new Cards();grid.setAdapter(adapter);body.addView(grid,new LinearLayout.LayoutParams(0,-1,1));
        status=text(busy?tr("Connecting…","جارٍ الاتصال…"):tr("Select to play • Hold OK for favorites","اختر للتشغيل • اضغط مطولاً على موافق للمفضلة"),12,MUTED);root.addView(status);TextView website=text(connection.optString("name")+"  •  www.miptv.me",12,ACCENT);website.setGravity(Gravity.CENTER);root.addView(website);
        search.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int before,int c){query=s.toString();page=0;render();}public void afterTextChanged(Editable e){}});
        filters();render();if(active!=null)active.requestFocus();
    }
    private void showHome(){
        adapter=null;grid=null;categories=null;
        LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setPadding(dp(32),dp(16),dp(32),dp(16));root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{0xff1c2c53,0xff0c1221,0xff05080f}));root.setLayoutDirection(ar()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);setScreen(root);
        FrameLayout header=new FrameLayout(this);root.addView(header,new LinearLayout.LayoutParams(-1,dp(105)));
        ImageView logo=new ImageView(this);logo.setImageResource(R.drawable.myiptv_logo);logo.setScaleType(ImageView.ScaleType.FIT_CENTER);logo.setContentDescription("MY IPTV");header.addView(logo,new FrameLayout.LayoutParams(dp(180),dp(104),Gravity.CENTER));
        TextView identity=text("MY IPTV",18,Color.WHITE);identity.setTypeface(null,Typeface.BOLD);header.addView(identity,new FrameLayout.LayoutParams(-2,-2,Gravity.START|Gravity.CENTER_VERTICAL));
        LinearLayout settings=new LinearLayout(this);header.addView(settings,new FrameLayout.LayoutParams(-2,-2,Gravity.END|Gravity.CENTER_VERTICAL));add(settings,button(tr("Playlists","قوائم التشغيل"),this::connections),-2,dp(44));add(settings,button(ar()?"English":"العربية",()->{getPreferences(0).edit().putBoolean("arabic",!ar()).apply();build();}),-2,dp(44));
        TextView welcome=text(tr("Your entertainment starts here","ترفيهك يبدأ من هنا"),24,Color.WHITE);welcome.setTypeface(null,Typeface.BOLD);welcome.setGravity(Gravity.CENTER);root.addView(welcome,new LinearLayout.LayoutParams(-1,dp(40)));
        TextView tagline=text(connection.optString("name",tr("Add your first playlist","أضف قائمة التشغيل الأولى"))+"  •  "+tr("Your playlists. Your entertainment.","قوائمك. ترفيهك."),13,MUTED);tagline.setGravity(Gravity.CENTER);root.addView(tagline,new LinearLayout.LayoutParams(-1,dp(28)));
        LinearLayout sections=new LinearLayout(this);sections.setGravity(Gravity.CENTER_VERTICAL);root.addView(sections,new LinearLayout.LayoutParams(-1,0,1));
        String[] kinds={"Live TV","Movies","Series"};String[] symbols={"▣","▶","▤"};int[] accents={0xff6aabff,0xffb28cff,0xff96d52d};View first=null;
        for(int i=0;i<kinds.length;i++){
            final String kind=kinds[i];final int accent=accents[i];int total=0;for(Channel c:channels)if(kind.equals(c.type))total++;
            LinearLayout tile=new LinearLayout(this);tile.setOrientation(1);tile.setGravity(Gravity.CENTER);tile.setPadding(dp(18),dp(12),dp(18),dp(12));LinearLayout.LayoutParams tileParams=new LinearLayout.LayoutParams(0,dp(166),1);tileParams.setMargins(dp(7),dp(8),dp(7),dp(8));sections.addView(tile,tileParams);
            TextView icon=text(symbols[i],34,accent);icon.setGravity(Gravity.CENTER);tile.addView(icon,new LinearLayout.LayoutParams(-1,dp(50)));
            TextView name=text(kind.equals("Movies")?tr("MOVIES / VOD","الأفلام / VOD"):label(kind),24,Color.WHITE);name.setGravity(Gravity.CENTER);name.setTypeface(null,Typeface.BOLD);tile.addView(name,new LinearLayout.LayoutParams(-1,dp(40)));
            TextView detail=text(total+" "+tr(kind.equals("Live TV")?"channels":"titles",kind.equals("Live TV")?"قناة":"عنوان"),13,MUTED);detail.setGravity(Gravity.CENTER);tile.addView(detail);
            focus(tile,PANEL);tile.setBackground(homeTile(accent,false));tile.setOnFocusChangeListener((v,f)->{v.setBackground(homeTile(accent,f));v.setScaleX(f?1.025f:1f);v.setScaleY(f?1.025f:1f);v.setElevation(f?dp(10):0);});tile.setOnClickListener(v->{home=false;tab=kind;category="";query="";page=0;build();});if(first==null)first=tile;
        }
        LinearLayout shortcuts=new LinearLayout(this);shortcuts.setGravity(Gravity.CENTER);root.addView(shortcuts);
        for(String kind:new String[]{"Continue Watching","Favorites","Recently Added"}){add(shortcuts,button(label(kind),()->{home=false;tab=kind;category="";query="";page=0;build();}),dp(205),dp(44));}
        LinearLayout footer=new LinearLayout(this);footer.setGravity(Gravity.CENTER);root.addView(footer);
        TextView website=text("www.miptv.me",16,ACCENT);website.setGravity(Gravity.CENTER);add(footer,website,dp(220),dp(30));
        add(footer,button(tr("Background","الخلفية"),this::backgroundOptions),dp(140),dp(40));

        status=text(busy?tr("Connecting…","جارٍ الاتصال…"):channels.isEmpty()?tr("Open Playlists to add your playlist","افتح قوائم التشغيل لإضافة قائمتك"):tr("Ready • Choose a section to start watching","جاهز • اختر قسماً لبدء المشاهدة"),12,MUTED);status.setGravity(Gravity.CENTER);root.addView(status,new LinearLayout.LayoutParams(-1,dp(30)));if(first!=null)first.requestFocus();
    }
    private GradientDrawable homeTile(int accent,boolean focused){GradientDrawable d=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{focused?0xff253554:0xff1b2942,0xff101827});d.setCornerRadius(dp(18));d.setStroke(dp(focused?3:1),focused?accent:0xff34415a);return d;}
    private boolean matches(Channel c){return tab.equals("Favorites")?favorites.contains(identity(c.url)):tab.equals("Recently Added")?recent.contains(identity(c.url)):tab.equals(c.type);}
    private void filters(){categories.removeAllViews();LinkedHashSet<String> names=new LinkedHashSet<>();names.add("");for(Channel c:channels)if(matches(c))names.add(c.group);if(tab.equals("Continue Watching"))for(WatchHistory.Entry e:history.continuing(connection.optString("id")))names.add(e.channel().group);for(String g:names){TextView b=button(g.isEmpty()?tr("All categories","كل التصنيفات"):g,()->{category=g;page=0;for(int k=0;k<categories.getChildCount();k++)((TextView)categories.getChildAt(k)).setTextColor(g.equals(categories.getChildAt(k).getTag())?ACCENT:MUTED);render();});b.setTag(g);b.setMaxLines(2);b.setTextColor(category.equals(g)?ACCENT:MUTED);add(categories,b,-1,-2);}}
    private ArrayList<Channel> filtered(){
        ArrayList<Channel> pool=new ArrayList<>(),out=new ArrayList<>();
        if(tab.equals("Continue Watching")){for(WatchHistory.Entry e:history.continuing(connection.optString("id")))pool.add(e.channel());}else pool.addAll(channels);
        String q=query.toLowerCase(Locale.ROOT);for(Channel c:pool)if((tab.equals("Continue Watching")||matches(c))&&(category.isEmpty()||category.equals(c.group))&&(c.name+" "+c.group).toLowerCase(Locale.ROOT).contains(q))out.add(c);return out;
    }
    private void render(){if(adapter==null)return;ArrayList<Channel> all=filtered();if(page*60>=all.size())page=0;shown.clear();shown.addAll(all.subList(Math.min(page*60,all.size()),Math.min((page+1)*60,all.size())));adapter.notifyDataSetChanged();grid.scrollToPosition(0);count.setText(all.size()+tr(" titles"," عنوان")+" · "+(page+1)+"/"+Math.max(1,(all.size()+59)/60));if(!busy)status.setText(channels.isEmpty()?tr("Open Playlists to add Xtream Codes, M3U or a direct stream.","افتح قوائم التشغيل لإضافة إكستريم أو M3U أو رابط بث مباشر."):all.isEmpty()?tr("No matching titles in this section.","لا توجد عناوين في هذا القسم."):tr("Select to play • Hold OK for favorites","اختر للتشغيل • اضغط مطولاً على موافق للمفضلة"));}
    private class Holder extends RecyclerView.ViewHolder {ImageView image;TextView title,subtitle;ProgressBar progress;Holder(LinearLayout v,ImageView i,TextView t,TextView s,ProgressBar p){super(v);image=i;title=t;subtitle=s;progress=p;}}
    private class Cards extends RecyclerView.Adapter<Holder>{
        public int getItemCount(){return shown.size();}
        public Holder onCreateViewHolder(ViewGroup parent,int type){LinearLayout card=new LinearLayout(MainActivity.this);card.setOrientation(1);card.setPadding(dp(8),dp(8),dp(8),dp(8));RecyclerView.LayoutParams p=new RecyclerView.LayoutParams(-1,dp(tab.equals("Live TV")?146:194));p.setMargins(dp(5),dp(5),dp(5),dp(5));card.setLayoutParams(p);focus(card,PANEL);ImageView image=new ImageView(MainActivity.this);image.setScaleType(tab.equals("Live TV")?ImageView.ScaleType.FIT_CENTER:ImageView.ScaleType.CENTER_CROP);card.addView(image,new LinearLayout.LayoutParams(-1,0,1));TextView title=text("",14,Color.WHITE);title.setMaxLines(2);title.setEllipsize(TextUtils.TruncateAt.END);card.addView(title);TextView sub=text("",11,MUTED);sub.setSingleLine(true);sub.setEllipsize(TextUtils.TruncateAt.END);card.addView(sub);ProgressBar progress=new ProgressBar(MainActivity.this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(1000);progress.setProgressTintList(android.content.res.ColorStateList.valueOf(ACCENT));card.addView(progress,new LinearLayout.LayoutParams(-1,dp(5)));return new Holder(card,image,title,sub,progress);}
        public void onBindViewHolder(Holder h,int position){Channel c=shown.get(position);h.itemView.setTag(c.url);h.itemView.setOnFocusChangeListener((v,f)->{v.setBackground(bg(PANEL,f));v.setElevation(f?dp(6):0);if(f)lastFocusedUrl=c.url;});
            h.title.setText(titleFor(c));WatchHistory.Entry entry=history.forCard(connection.optString("id"),c);
            h.subtitle.setText(entry==null?c.group:entry.done()?tr("Watched","تمت المشاهدة"):tr("Resume at ","تابع عند ")+ProgressPolicy.time(entry.position()));
            h.progress.setVisibility(entry==null?View.GONE:View.VISIBLE);h.progress.setProgress(entry==null?0:entry.done()?1000:entry.duration()>0?(int)Math.min(1000,entry.position()*1000/entry.duration()):30);h.image.setImageResource(android.R.drawable.ic_media_play);h.image.setTag(c.logo);Bitmap cached=art.get(c.logo);if(cached!=null)h.image.setImageBitmap(cached);else if(c.logo.startsWith("https://")||c.logo.startsWith("http://")){images.execute(()->{try{HttpURLConnection con=(HttpURLConnection)new URL(c.logo).openConnection();con.setConnectTimeout(5000);con.setReadTimeout(5000);byte[] data;try(java.io.InputStream in=con.getInputStream();java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){byte[] buf=new byte[4096];int n;while((n=in.read(buf))!=-1){out.write(buf,0,n);if(out.size()>2*1024*1024)throw new java.io.IOException();}data=out.toByteArray();}finally{con.disconnect();}BitmapFactory.Options opt=new BitmapFactory.Options();opt.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,opt);opt.inSampleSize=1;while(opt.outWidth/opt.inSampleSize>480||opt.outHeight/opt.inSampleSize>480)opt.inSampleSize*=2;opt.inJustDecodeBounds=false;Bitmap b=BitmapFactory.decodeByteArray(data,0,data.length,opt);if(b!=null){art.put(c.logo,b);runOnUiThread(()->{if(c.logo.equals(h.image.getTag()))h.image.setImageBitmap(b);});}}catch(Exception ignored){}});}
            h.itemView.setOnClickListener(v->open(c));h.itemView.setOnLongClickListener(v->{if(!favorites.add(identity(c.url)))favorites.remove(identity(c.url));getPreferences(0).edit().putStringSet("favorites",new HashSet<>(favorites)).apply();h.title.setText(titleFor(c));Toast.makeText(MainActivity.this,tr("Favorites updated","تم تحديث المفضلة"),Toast.LENGTH_SHORT).show();if(tab.equals("Favorites"))render();return true;});
        }
    }
    private String titleFor(Channel c){WatchHistory.Entry e=history.forCard(connection.optString("id"),c);return (favorites.contains(identity(c.url))?"★ ":"")+(e==null?"":e.done()?"✓ ":"◐ ")+c.name;}
    private void restoreCardFocus(){if(grid==null||lastFocusedUrl.isEmpty())return;grid.post(()->{for(int j=0;j<shown.size();j++)if(shown.get(j).url.equals(lastFocusedUrl)){final int index=j;grid.scrollToPosition(index);grid.post(()->{RecyclerView.ViewHolder h=grid.findViewHolderForAdapterPosition(index);if(h!=null)h.itemView.requestFocus();});break;}});}
    private void open(Channel c){
        if(!c.url.startsWith("series:")){playChoice(c,"");return;}
        if(busy)return;busy=true;status.setText(tr("Loading episodes…","جارٍ تحميل الحلقات…"));
        final JSONObject selected=connection;
        network.execute(()->{try{List<Channel> episodes=Catalog.episodes(selected.optString("server"),selected.optString("user"),selected.optString("password"),c);runOnUiThread(()->{if(isDestroyed())return;busy=false;render();String[] names=new String[episodes.size()];for(int j=0;j<names.length;j++){Channel e=episodes.get(j);WatchHistory.Entry mark=history.get(selected.optString("id"),e.url);names[j]=(mark==null?"":mark.done()?"✓  ":"◐  "+ProgressPolicy.time(mark.position())+"  ·  ")+e.name;}
            AlertDialog dialog=new AlertDialog.Builder(this).setTitle(c.name).setItems(names,(d,w)->playChoice(episodes.get(w),c.url)).setNegativeButton(tr("Close","إغلاق"),null).create();
            dialog.setOnShowListener(d->{WatchHistory.Entry last=history.forCard(selected.optString("id"),c);if(last!=null)for(int j=0;j<episodes.size();j++)if(episodes.get(j).url.equals(last.channel().url)){dialog.getListView().setSelection(j);break;}});dialog.show();});}catch(Exception e){runOnUiThread(this::failure);}});
    }
    private void playChoice(Channel c,String parent){
        WatchHistory.Entry e=history.get(connection.optString("id"),c.url);
        String actualParent=parent.isEmpty()&&e!=null?e.parent():parent;
        if(!c.type.equals("Live TV")&&e!=null&&e.resumable()){
            new AlertDialog.Builder(this).setTitle(c.name).setItems(new String[]{tr("Resume from ","متابعة من ")+ProgressPolicy.time(e.position()),tr("Start from beginning","البدء من البداية")},(d,w)->{if(w==1){try{history.restart(this,connection.optString("id"),c.url);}catch(Exception error){Toast.makeText(this,tr("Could not reset progress","تعذر إعادة ضبط التقدم"),Toast.LENGTH_LONG).show();return;}}launch(c,actualParent,w==0?e.position():0);}).setNegativeButton(tr("Cancel","إلغاء"),null).show();
        }else launch(c,actualParent,0);
    }
    private void launch(Channel c,String parent,long position){
        Intent i=new Intent(this,PlayerActivity.class);i.putExtra("name",c.name);i.putExtra("url",c.url);i.putExtra("type",c.type);i.putExtra("group",c.group);i.putExtra("logo",c.logo);i.putExtra("parent",parent);i.putExtra("playlist",connection.optString("id"));i.putExtra("position",position);i.putExtra("arabic",ar());lastFocusedUrl=parent.isEmpty()||tab.equals("Continue Watching")?c.url:parent;startActivity(i);
    }
    private EditText input(LinearLayout box,String hint,boolean password,String value){EditText e=new EditText(this);e.setHint(hint);e.setSingleLine(true);e.setTextSize(15);e.setTextColor(Color.WHITE);e.setHintTextColor(MUTED);e.setInputType(password?129:1);e.setText(value);add(box,e,-1,dp(50));return e;}
    private void connections(){
        if(busy){Toast.makeText(this,tr("Please wait for the current playlist","يرجى انتظار تحميل القائمة الحالية"),Toast.LENGTH_SHORT).show();return;}
        JSONArray all=document.optJSONArray("playlists");if(all==null)all=new JSONArray();final JSONArray lists=all;
        String[] names=new String[lists.length()+1];names[0]=tr("+ Add playlist","+ إضافة قائمة تشغيل");
        for(int j=0;j<lists.length();j++){JSONObject item=lists.optJSONObject(j);names[j+1]=(item.optString("id").equals(connection.optString("id"))?"✓  ":"")+item.optString("name");}
        new AlertDialog.Builder(this).setTitle(tr("Playlists","قوائم التشغيل")).setItems(names,(d,index)->{if(index==0)addPlaylist();else playlistActions(lists.optJSONObject(index-1));}).setNeutralButton(tr("Background","الخلفية"),(d,w)->backgroundOptions()).setNegativeButton(tr("Close","إغلاق"),null).show();
    }
    private void addPlaylist(){new AlertDialog.Builder(this).setTitle(tr("Add playlist","إضافة قائمة تشغيل")).setItems(new String[]{"Xtream Codes",tr("M3U / M3U8 playlist URL","رابط قائمة M3U / M3U8"),tr("Direct stream URL (TS / HLS / MP4)","رابط بث مباشر (TS / HLS / MP4)")},(d,which)->connectionForm(which)).setNegativeButton(tr("Cancel","إلغاء"),null).show();}
    private void playlistActions(JSONObject playlist){
        new AlertDialog.Builder(this).setTitle(playlist.optString("name")).setItems(new String[]{tr("Open playlist","فتح القائمة"),tr("Rename","تغيير الاسم"),tr("Remove playlist","إزالة القائمة")},(d,index)->{if(index==0)connect(playlist);else if(index==1)renamePlaylist(playlist);else new AlertDialog.Builder(this).setTitle(tr("Remove playlist?","إزالة القائمة؟")).setMessage(playlist.optString("name")).setPositiveButton(tr("Remove","إزالة"),(dialog,w)->removePlaylist(playlist)).setNegativeButton(tr("Cancel","إلغاء"),null).show();}).setNegativeButton(tr("Close","إغلاق"),null).show();
    }
    private void renamePlaylist(JSONObject playlist){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(16),dp(8),dp(16),dp(8));box.setBackgroundColor(PANEL);
        EditText name=input(box,tr("Playlist name","اسم القائمة"),false,playlist.optString("name"));name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(tr("Rename playlist","تغيير اسم القائمة")).setView(box).setPositiveButton(tr("Save","حفظ"),null).setNegativeButton(tr("Cancel","إلغاء"),null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{try{JSONObject renamed=new JSONObject(playlist.toString()).put("name",name.getText().toString().trim());JSONObject candidate=PlaylistDocument.withPlaylist(document,renamed).put("active",document.optString("active"));Vault.save(this,candidate.toString());document=candidate;connection=PlaylistDocument.active(document);dialog.dismiss();build();}catch(Exception e){name.setError(tr("Enter a valid name; check device storage","أدخل اسماً صالحاً وتحقق من مساحة الجهاز"));}}));dialog.show();
    }
    private void removePlaylist(JSONObject playlist){
        try{JSONObject candidate=new JSONObject(document.toString());JSONArray lists=new JSONArray(),all=candidate.getJSONArray("playlists");for(int j=0;j<all.length();j++){JSONObject item=all.getJSONObject(j);if(!item.optString("id").equals(playlist.optString("id")))lists.put(item);}
            boolean active=playlist.optString("id").equals(connection.optString("id"));candidate.put("playlists",lists);if(active)candidate.put("active","");Vault.save(this,candidate.toString());document=candidate;
            if(active){connection=new JSONObject();channels.clear();recent.clear();home=true;}build();if(active&&lists.length()>0)connect(lists.getJSONObject(0));
        }catch(Exception e){Toast.makeText(this,tr("Could not save playlists","تعذر حفظ القوائم"),Toast.LENGTH_LONG).show();}
    }
    private void connectionForm(int mode){
        LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setPadding(dp(18),dp(8),dp(18),dp(8));box.setBackgroundColor(PANEL);ScrollView scroll=new ScrollView(this);scroll.addView(box);
        EditText name=input(box,tr("Playlist name (required)","اسم القائمة (مطلوب)"),false,"");name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(60)});
        EditText url=input(box,mode==0?tr("Server URL, including port","رابط الخادم مع المنفذ"):tr("HTTP(S) URL","رابط HTTP(S)"),false,"");url.setInputType(17);
        EditText user=mode==0?input(box,tr("Username","اسم المستخدم"),false,""):null;
        EditText pass=mode==0?input(box,tr("Password","كلمة المرور"),true,""):null;
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(tr("Add playlist","إضافة قائمة تشغيل")+" · "+(mode==0?"Xtream Codes":mode==1?"M3U":"TS / HLS")).setView(scroll).setPositiveButton(tr("Save and open","حفظ وفتح"),null).setNegativeButton(tr("Cancel","إلغاء"),null).create();
        dialog.setOnShowListener(d->dialog.getButton(-1).setOnClickListener(v->{String address=url.getText().toString().trim(),title=name.getText().toString().trim();if(title.isEmpty()){name.setError(tr("Playlist name is required","اسم القائمة مطلوب"));return;}try{JSONObject next=new JSONObject().put("name",title).put("mode",mode);
            if(mode==0){String username=user.getText().toString().trim(),password=pass.getText().toString();if(username.isEmpty()||password.isEmpty())throw new Exception();next.put("server",Catalog.base(address)).put("user",username).put("password",password);}
            else{URI uri=new URI(address);if(uri.getHost()==null||!("http".equalsIgnoreCase(uri.getScheme())||"https".equalsIgnoreCase(uri.getScheme())))throw new Exception();next.put("url",address);}
            dialog.dismiss();connect(next);
        }catch(Exception e){url.setError(tr("Check URL and required fields","تحقق من الرابط والحقول المطلوبة"));}}));dialog.show();
    }
    private void connect(JSONObject requested){
        if(busy)return;
        final JSONObject candidate,next;try{candidate=PlaylistDocument.withPlaylist(document,requested);next=PlaylistDocument.active(candidate);}catch(Exception e){Toast.makeText(this,tr("Enter a playlist name","أدخل اسم القائمة"),Toast.LENGTH_LONG).show();return;}
        busy=true;status.setText(tr("Loading playlist…","جارٍ تحميل القائمة…"));
        final Set<String> old=new HashSet<>();if(next.optString("id").equals(connection.optString("id")))for(Channel c:channels)old.add(c.url);
        network.execute(()->{try{
            int mode=next.getInt("mode");
            List<Channel> items=mode==0?Catalog.xtream(next.getString("server"),next.getString("user"),next.getString("password")):mode==1?Catalog.playlist(next.getString("url")):Collections.singletonList(new Channel(next.getString("name"),next.getString("url"),tr("Direct","مباشر"),"","Live TV"));
            Vault.save(this,candidate.toString());
            runOnUiThread(()->{if(isDestroyed())return;document=candidate;connection=next;recent.clear();String recentSlot="recent."+next.optString("id");
                if(old.isEmpty())recent.addAll(getPreferences(0).getStringSet(recentSlot,new HashSet<>()));
                if(!old.isEmpty()||recent.isEmpty()){recent.clear();for(int j=items.size()-1;j>=0&&recent.size()<100;j--)if(!old.contains(items.get(j).url))recent.add(identity(items.get(j).url));}
                for(Channel c:items)if(favorites.contains(legacyIdentity(c.url)))favorites.add(identity(c.url));
                getPreferences(0).edit().putStringSet(recentSlot,new HashSet<>(recent)).putStringSet("favorites",new HashSet<>(favorites)).remove("playlist").remove("xt_user").remove("xt_server").apply();
                channels.clear();channels.addAll(items);busy=false;category="";query="";lastFocusedUrl="";page=0;build();
            });
        }catch(Exception e){runOnUiThread(this::failure);}});
    }
    private void setScreen(LinearLayout root){
        if(backgroundPhoto==null){setContentView(root);return;}
        root.setBackgroundColor(Color.TRANSPARENT);FrameLayout frame=new FrameLayout(this);ImageView photo=new ImageView(this);photo.setImageBitmap(backgroundPhoto);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);frame.addView(photo,new FrameLayout.LayoutParams(-1,-1));View shade=new View(this);shade.setBackgroundColor(0xb0080e19);frame.addView(shade,new FrameLayout.LayoutParams(-1,-1));frame.addView(root,new FrameLayout.LayoutParams(-1,-1));setContentView(frame);
    }
    private void backgroundOptions(){
        new AlertDialog.Builder(this).setTitle(tr("Background photo","صورة الخلفية")).setItems(new String[]{tr("Choose photo from device","اختيار صورة من الجهاز"),tr("Use image URL","استخدام رابط صورة"),tr("Use default background","استخدام الخلفية الافتراضية")},(d,index)->{
            if(index==0){Intent choose=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("image/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);try{startActivityForResult(choose,74);}catch(ActivityNotFoundException e){Toast.makeText(this,tr("No photo picker installed. Use an image URL.","لا يوجد منتقي صور. استخدم رابط صورة."),Toast.LENGTH_LONG).show();}}
            else if(index==1){LinearLayout box=new LinearLayout(this);box.setOrientation(1);box.setBackgroundColor(PANEL);EditText url=input(box,tr("Image URL (HTTP/HTTPS)","رابط الصورة (HTTP/HTTPS)"),false,"");url.setInputType(17);AlertDialog dialog=new AlertDialog.Builder(this).setTitle(tr("Background image URL","رابط صورة الخلفية")).setView(box).setPositiveButton(tr("Apply","تطبيق"),null).setNegativeButton(tr("Cancel","إلغاء"),null).create();dialog.setOnShowListener(ignored->dialog.getButton(-1).setOnClickListener(v->{String address=url.getText().toString().trim();try{URI uri=new URI(address);if(uri.getHost()==null||!("https".equalsIgnoreCase(uri.getScheme())||"http".equalsIgnoreCase(uri.getScheme())))throw new Exception();dialog.dismiss();importBackground(null,address);}catch(Exception e){url.setError(tr("Enter a valid image URL","أدخل رابط صورة صالحاً"));}}));dialog.show();}
            else{java.io.File file=new java.io.File(getFilesDir(),"background.jpg");if(file.exists()&&!file.delete()){Toast.makeText(this,tr("Could not remove background","تعذر إزالة الخلفية"),Toast.LENGTH_LONG).show();return;}backgroundPhoto=null;build();}
        }).setNegativeButton(tr("Close","إغلاق"),null).show();
    }
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(request==74&&result==RESULT_OK&&data!=null&&data.getData()!=null)importBackground(data.getData(),null);}
    private Bitmap decodePhoto(byte[] data)throws Exception{BitmapFactory.Options options=new BitmapFactory.Options();options.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(data,0,data.length,options);if(options.outWidth<=0||options.outHeight<=0)throw new java.io.IOException();options.inSampleSize=1;while(options.outWidth/options.inSampleSize>1920||options.outHeight/options.inSampleSize>1080)options.inSampleSize*=2;options.inJustDecodeBounds=false;Bitmap bitmap=BitmapFactory.decodeByteArray(data,0,data.length,options);if(bitmap==null)throw new java.io.IOException();return bitmap;}
    private byte[] photoBytes(java.io.InputStream stream)throws Exception{try(java.io.InputStream in=stream;java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){if(in==null)throw new java.io.IOException();byte[] buffer=new byte[8192];int n;while((n=in.read(buffer))!=-1){out.write(buffer,0,n);if(out.size()>8*1024*1024)throw new java.io.IOException();}return out.toByteArray();}}
    private void loadSavedBackground(){java.io.File file=new java.io.File(getFilesDir(),"background.jpg");if(!file.exists())return;images.execute(()->{try{Bitmap bitmap=decodePhoto(photoBytes(new java.io.FileInputStream(file)));runOnUiThread(()->{if(!isDestroyed()){backgroundPhoto=bitmap;build();}});}catch(Exception ignored){}});}
    private void importBackground(android.net.Uri uri,String address){
        Toast.makeText(this,tr("Loading background…","جارٍ تحميل الخلفية…"),Toast.LENGTH_SHORT).show();
        network.execute(()->{java.io.File temporary=new java.io.File(getFilesDir(),"background.new");try{
            byte[] data;if(uri!=null)data=photoBytes(getContentResolver().openInputStream(uri));else{HttpURLConnection conn=(HttpURLConnection)new URL(address).openConnection();conn.setConnectTimeout(12000);conn.setReadTimeout(15000);try{if(conn.getResponseCode()!=200)throw new java.io.IOException();data=photoBytes(conn.getInputStream());}finally{conn.disconnect();}}
            Bitmap bitmap=decodePhoto(data);try(java.io.FileOutputStream out=new java.io.FileOutputStream(temporary)){if(!bitmap.compress(Bitmap.CompressFormat.JPEG,88,out))throw new java.io.IOException();}
            if(!temporary.renameTo(new java.io.File(getFilesDir(),"background.jpg")))throw new java.io.IOException();
            runOnUiThread(()->{if(!isDestroyed()){backgroundPhoto=bitmap;build();Toast.makeText(this,tr("Background updated","تم تحديث الخلفية"),Toast.LENGTH_SHORT).show();}});
        }catch(Exception e){temporary.delete();runOnUiThread(()->{if(!isDestroyed())Toast.makeText(this,tr("Use a JPG/PNG image under 8 MB and check access.","استخدم صورة JPG/PNG أقل من ٨ ميغابايت وتحقق من الوصول."),Toast.LENGTH_LONG).show();});}});
    }
    private void failure(){if(isDestroyed())return;busy=false;if(home)build();else render();new AlertDialog.Builder(this).setTitle(tr("Connection failed","فشل الاتصال")).setMessage(tr("Check the server URL, credentials and network. The provider may be unavailable, the account inactive, or the response invalid. Your previous catalog is retained.","تحقق من رابط الخادم وبيانات الدخول والشبكة. قد يكون الحساب غير نشط أو الخادم غير متاح أو الاستجابة غير صالحة. تم الاحتفاظ بالقائمة السابقة.")).setPositiveButton(tr("OK","حسناً"),null).show();}
    @Override public void onBackPressed(){if(!home){home=true;build();}else super.onBackPressed();}
    @Override protected void onDestroy(){network.shutdownNow();images.shutdownNow();super.onDestroy();}
}
