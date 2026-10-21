from pathlib import Path
import sys
p=Path(sys.argv[1])/'app/src/main/java/com/myiptv/player/MainActivity.java'
s=p.read_text()
if 'private void loadXtream(' in s:quit()
s=s.replace('private String tab="Live TV", category="All";','private String tab="Live TV", category="All";\n    private final Set<String> recent=new LinkedHashSet<>();')
s=s.replace('favorites.addAll(getPreferences(0).getStringSet("favorites",new HashSet<>()));','favorites.addAll(getPreferences(0).getStringSet("favorites",new HashSet<>()));recent.addAll(getPreferences(0).getStringSet("recent",new HashSet<>()));')
s=s.replace('        LinearLayout nav=new LinearLayout(this);','''        TextView xt=text(tr("XTREAM CODES LOGIN","تسجيل إكستريم كودز"),15,WHITE,true);root.addView(xt);
        EditText server=new EditText(this),user=new EditText(this),pass=new EditText(this);
        for(EditText e:new EditText[]{server,user,pass}){e.setSingleLine(true);e.setTextColor(WHITE);e.setHintTextColor(MUTED);root.addView(e);}
        server.setHint(tr("Server URL","رابط الخادم"));user.setHint(tr("Username","اسم المستخدم"));pass.setHint(tr("Password","كلمة المرور"));pass.setInputType(129);
        server.setText(getPreferences(0).getString("xt_server",""));user.setText(getPreferences(0).getString("xt_user",""));
        Button connect=button(tr("Connect Xtream","اتصال إكستريم"));root.addView(connect);
        connect.setOnClickListener(v->loadXtream(server.getText().toString().trim(),user.getText().toString().trim(),pass.getText().toString()));
        addSpace(root,20);
        LinearLayout nav=new LinearLayout(this);''')
s=s.replace('new String[]{"Live TV","Movies","Series","Favorites"}','new String[]{"Live TV","Movies","Series","Favorites","Recently Added"}')
s=s.replace('tr("Favorites","المفضلة");','name.equals("Favorites")?tr("Favorites","المفضلة"):tr("Recently Added","المضاف حديثاً");')
s=s.replace('return tab.equals("Favorites")?favorites.contains(c.url):c.type.equals(tab);','return tab.equals("Favorites")?favorites.contains(c.url):tab.equals("Recently Added")?recent.contains(c.url):c.type.equals(tab);')
s=s.replace('channels.clear();channels.addAll(imported);getPreferences','channels.clear();channels.addAll(imported);saveRecent(imported);getPreferences')
s=s.replace('    private void loadPlaylist(String address){','''    private void saveRecent(List<Channel> items){recent.clear();for(int i=items.size()-1;i>=0&&i>=items.size()-100;i--)recent.add(items.get(i).url);getPreferences(0).edit().putStringSet("recent",new HashSet<>(recent)).apply();}
    private String api(String address)throws Exception{HttpURLConnection c=(HttpURLConnection)new URL(address).openConnection();c.setConnectTimeout(12000);c.setReadTimeout(25000);try{if(c.getResponseCode()!=200)throw new IOException("HTTP "+c.getResponseCode());try(InputStream in=c.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>12000000)throw new IOException("Response too large");}return out.toString("UTF-8");}}finally{c.disconnect();}}
    private void loadXtream(String server,String username,String password){
      if(!server.matches("https?://.+")||username.isEmpty()||password.isEmpty()){Toast.makeText(this,tr("Enter server and credentials","أدخل الخادم وبيانات الدخول"),Toast.LENGTH_LONG).show();return;}
      new Thread(()->{try{
        String base=server.replaceAll("/+$",""),creds="username="+URLEncoder.encode(username,"UTF-8")+"&password="+URLEncoder.encode(password,"UTF-8");
        org.json.JSONObject auth=new org.json.JSONObject(api(base+"/player_api.php?"+creds));
        if(auth.optJSONObject("user_info")==null||!"Active".equalsIgnoreCase(auth.getJSONObject("user_info").optString("status")))throw new IOException("Account not active");
        ArrayList<Channel> found=new ArrayList<>();
        String[] actions={"get_live_streams","get_vod_streams"},kinds={"Live TV","Movies"},paths={"live","movie"};
        for(int k=0;k<actions.length;k++){
          org.json.JSONArray arr=new org.json.JSONArray(api(base+"/player_api.php?"+creds+"&action="+actions[k]));
          for(int j=0;j<arr.length();j++){org.json.JSONObject o=arr.optJSONObject(j);if(o==null)continue;String id=o.optString("stream_id");if(id.isEmpty())continue;
            String ext=k==0?"ts":o.optString("container_extension","mp4");
            String link=base+"/"+paths[k]+"/"+URLEncoder.encode(username,"UTF-8")+"/"+URLEncoder.encode(password,"UTF-8")+"/"+id+"."+ext;
            found.add(new Channel(o.optString("name","Stream"),link,o.optString("category_id","Uncategorized"),o.optString("stream_icon",""),kinds[k]));
          }
        }
        if(found.isEmpty())throw new IOException("No streams found");
        runOnUiThread(()->{channels.clear();channels.addAll(found);saveRecent(found);getPreferences(0).edit().putString("xt_server",base).putString("xt_user",username).apply();tab="Live TV";category="All";build();Toast.makeText(this,tr("Xtream connected","تم الاتصال بإكستريم"),Toast.LENGTH_LONG).show();});
      }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"Xtream: "+e.getMessage(),Toast.LENGTH_LONG).show());}}).start();
    }
    private void loadPlaylist(String address){''')
p.write_text(s)
