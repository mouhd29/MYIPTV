from pathlib import Path
import sys
root=Path(sys.argv[1])
java=root/'app/src/main/java/com/myiptv/player/MainActivity.java'
s=java.read_text(encoding='utf-8')
if 'private String tr(String en,String ar)' in s:
    sys.exit(0)
s=s.replace('private String tab="Live TV", category="All";', '''private String tab="Live TV", category="All";
    private boolean arabic(){return getPreferences(0).getString("language","en").equals("ar");}
    private String tr(String en,String ar){return arabic()?ar:en;}
''')
s=s.replace('pad(root,16,20);scroll.addView(root);', '''pad(root,16,20);root.setLayoutDirection(arabic()?View.LAYOUT_DIRECTION_RTL:View.LAYOUT_DIRECTION_LTR);scroll.addView(root);''')
needle='TextView subtitle=text('
insert='''Button language=button(arabic()?"English":"العربية");root.addView(language);
        language.setOnClickListener(v->{getPreferences(0).edit().putString("language",arabic()?"en":"ar").apply();build();});
        addSpace(root,10);
        '''
s=s.replace(needle,insert+needle,1)
mapping={
'"YOUR ENTERTAINMENT. YOUR PLAYLIST."':'"ترفيهك، قائمتك الخاصة."',
'"M3U / M3U8 PLAYLIST URL"':'"رابط قائمة التشغيل M3U / M3U8"',
'"IMPORT PLAYLIST"':'"استيراد قائمة التشغيل"',
'"Search channels and titles..."':'"ابحث عن القنوات والعناوين..."',
'"Add an M3U playlist URL to begin."':'"أضف رابط قائمة تشغيل M3U للبدء."',
'"No items in this section. Try another category or search."':'"لا توجد عناصر هنا. جرّب تصنيفاً آخر أو ابحث."',
'" items • MYiptv Beta"':'" عنصر • MY IPTV تجريبي"',
'"Enter an HTTP(S) playlist URL"':'"أدخل رابط قائمة تشغيل HTTP(S)"',
'"Loading playlist..."':'"جارٍ تحميل قائمة التشغيل..."',
'"Imported "':'"تم استيراد "',
'" entries"':'" عنصر"',
'"Import failed: "':'"فشل الاستيراد: "',
}
for en,ar in mapping.items():
    if en in s:s=s.replace(en,'tr('+en+','+ar+')')
# nav display labels only; tab internal IDs remain English
s=s.replace('TextView b=text(name,13,WHITE,true);','''String shown=name.equals("Live TV")?tr("Live TV","البث المباشر"):name.equals("Movies")?tr("Movies","الأفلام"):name.equals("Series")?tr("Series","المسلسلات"):tr("Favorites","المفضلة");
            TextView b=text(shown,13,WHITE,true);''')
s=s.replace('TextView chip=text(g,12,WHITE,false);','TextView chip=text(g.equals("All")?tr("All","الكل"):g,12,WHITE,false);')
java.write_text(s,encoding='utf-8')
print('Localized Android source:',java)
