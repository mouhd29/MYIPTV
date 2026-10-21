package com.myiptv.player;
import android.content.Context;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
final class Vault {
    private static javax.crypto.SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);
        if(!store.containsAlias("myiptv.connection")){KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");g.init(new KeyGenParameterSpec.Builder("myiptv.connection",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();}
        return (javax.crypto.SecretKey)store.getKey("myiptv.connection",null);
    }
    static void save(Context c,String text) throws Exception {Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());c.getSharedPreferences("vault",0).edit().putString("connection",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(text.getBytes("UTF-8")),Base64.NO_WRAP)).apply();}
    static String read(Context c){try{String[] parts=c.getSharedPreferences("vault",0).getString("connection","").split(":");if(parts.length!=2)return "";Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));return new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),"UTF-8");}catch(Exception e){return "";}}
}
