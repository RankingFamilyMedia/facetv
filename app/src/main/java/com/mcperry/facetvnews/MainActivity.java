package com.mcperry.facetvnews;
import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.webkit.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
 private WebView web;private final ExecutorService work=Executors.newSingleThreadExecutor();private final Map<String,PhoneTools.Doc> duplicates=new ConcurrentHashMap<>();private AtomicBoolean cancelled=new AtomicBoolean(false);private String fileHash="";private volatile boolean busyScan=false;
 @Override public void onCreate(Bundle b){super.onCreate(b);web=new WebView(this);setContentView(web);web.setBackgroundColor(0xfff9f7f3);web.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setSafeBrowsingEnabled(true);
  web.addJavascriptInterface(new Bridge(),"FaceNative");web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){return !r.getUrl().toString().startsWith("file:///android_asset/");}});web.loadUrl("file:///android_asset/index.html");NewsJob.schedule(this);if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->web.evaluateJavascript("window.backHome&&window.backHome()",null));
 }
 private void emit(String type,JSONObject data){try{JSONObject event=new JSONObject().put("type",type).put("data",data);runOnUiThread(()->{if(!isFinishing()&&!isDestroyed())web.evaluateJavascript("window.onNative&&window.onNative(JSON.parse("+JSONObject.quote(event.toString())+"))",null);});}catch(Exception ignored){}}
 private void error(String id,Exception e){try{emit("error",new JSONObject().put("id",id).put("message",e.getMessage()==null?"Unable to complete request.":e.getMessage()));}catch(Exception ignored){}}
 private void reader(String url,String title){if(FeedRepository.https(url))startActivity(new Intent(this,ReaderActivity.class).putExtra("url",url).putExtra("title",title));}
 private void settings(){try{emit("settings",new JSONObject().put("notifications",getSharedPreferences("news",0).getBoolean("notifications",false)).put("sources",new JSONArray(getSharedPreferences("news",0).getString("sources","[]"))));}catch(Exception e){error("settings",e);}}
 public final class Bridge {
  @JavascriptInterface public void command(String value){JSONObject d;try{d=new JSONObject(value);}catch(Exception e){return;}String action=d.optString("action");
   switch(action){
    case "ready":settings();break;
    case "exit":runOnUiThread(()->finish());break;
    case "open":runOnUiThread(()->reader(d.optString("url"),d.optString("title")));break;
    case "notifications":runOnUiThread(()->{
     boolean enabled=d.optBoolean("enabled");if(enabled)getSharedPreferences("news",0).edit().putBoolean("baseline",false).remove("seen").apply();getSharedPreferences("news",0).edit().putBoolean("notifications",enabled).apply();
     if(enabled&&Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},13);else {NewsJob.schedule(MainActivity.this);settings();}
    });break;
    case "system":runOnUiThread(()->{String which=d.optString("which");String setting=which.equals("storage")?Settings.ACTION_INTERNAL_STORAGE_SETTINGS:which.equals("battery")?Settings.ACTION_BATTERY_SAVER_SETTINGS:Settings.ACTION_SECURITY_SETTINGS;try{startActivity(new Intent(setting));}catch(Exception e){startActivity(new Intent(Settings.ACTION_SETTINGS));}});break;
    case "chooseFolder":runOnUiThread(()->{if(busyScan)return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);startActivityForResult(i,20);});break;
    case "chooseFile":runOnUiThread(()->{if(busyScan)return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivityForResult(i,21);});break;
    case "cancel":cancelled.set(true);break;
    case "lookup":runOnUiThread(()->{if(fileHash.length()!=64)return;new AlertDialog.Builder(MainActivity.this).setTitle("File fingerprint lookup").setMessage("Send this file's SHA-256 fingerprint to VirusTotal by opening its report? No file contents are uploaded. The fingerprint may identify the file. A missing report means unknown, not safe. Third-party privacy terms apply.").setNegativeButton("Cancel",null).setPositiveButton("Open report",(dialog,which)->reader("https://www.virustotal.com/gui/file/"+fileHash,"VirusTotal file report")).show();});break;
    case "clear":runOnUiThread(()->new AlertDialog.Builder(MainActivity.this).setTitle("Clear FaceTVNews cache?").setMessage("This clears this app's managed cache and cached headline copy. Saved story links and notification preferences remain. It does not clear other apps' data or guarantee faster performance.").setNegativeButton("Cancel",null).setPositiveButton("Clear",(a,b)->{web.clearCache(true);work.execute(()->{try{long freed=PhoneTools.clearCache(getCacheDir());getSharedPreferences("news",0).edit().remove("cached-home").apply();emit("cleaned",new JSONObject().put("bytes",freed));emit("tools",PhoneTools.status(MainActivity.this));}catch(Exception e){error("tools",e);}});}).show());break;
    case "delete":runOnUiThread(()->{try{JSONArray tokens=d.getJSONArray("tokens");PhoneTools.validateSelection(duplicates,tokens);new AlertDialog.Builder(MainActivity.this).setTitle("Permanently delete selected duplicates?").setMessage("Delete "+tokens.length()+" selected copies? At least one identical copy per group will be retained and rechecked. Your storage provider may not provide a recycle bin.").setNegativeButton("Cancel",null).setPositiveButton("Delete",(a,b)->work.execute(()->{try{emit("deleted",PhoneTools.remove(MainActivity.this,duplicates,tokens));}catch(Exception e){error("duplicates",e);}})).show();}catch(Exception e){error("duplicates",e);}});break;
    default:work.execute(()->background(action,d));
   }
  }
 }
 private void background(String action,JSONObject d){try{
  if(action.equals("news")){
   int page=d.optInt("page",1);if(page<1||page>10000)throw new Exception("Invalid page");String category=d.optString("category"),search=d.optString("search");
   try{JSONObject result=FeedRepository.primary(this,page,category,search).put("request",d.optString("request"));if(page==1&&category.isEmpty()&&search.isEmpty())getSharedPreferences("news",0).edit().putString("cached-home",result.toString()).apply();emit("news",result);}
   catch(Exception e){String cached=getSharedPreferences("news",0).getString("cached-home","");if(page==1&&category.isEmpty()&&search.isEmpty()&&!cached.isEmpty())emit("news",new JSONObject(cached).put("request",d.optString("request")).put("cached",true).put("warning","Source unavailable. Showing previously cached headlines."));else throw e;}
  }else if(action.equals("categories")){emit("categories",new JSONObject().put("categories",FeedRepository.categories(this)));}
  else if(action.equals("tools"))emit("tools",PhoneTools.status(this));
  else if(action.equals("feeds")){
   JSONArray sources=new JSONArray(getSharedPreferences("news",0).getString("sources","[]")),posts=new JSONArray(),failures=new JSONArray();
   for(int i=0;i<sources.length();i++){JSONObject src=sources.getJSONObject(i);try{JSONArray items=FeedRepository.rss(src.getString("url"),src.getString("name"));for(int j=0;j<items.length();j++)posts.put(items.getJSONObject(j));}catch(Exception e){failures.put(src.optString("name")+": "+e.getMessage());}}
   emit("feeds",new JSONObject().put("posts",posts).put("failures",failures));
  }else if(action.equals("addSource")){
   String name=d.optString("name").trim(),url=d.optString("url").trim();if(name.isEmpty()||name.length()>80||!FeedRepository.https(url))throw new Exception("Provide a source name and a valid HTTPS feed URL.");JSONArray sources=new JSONArray(getSharedPreferences("news",0).getString("sources","[]"));if(sources.length()>=5)throw new Exception("Maximum five external feeds per device.");for(int i=0;i<sources.length();i++)if(sources.getJSONObject(i).getString("url").equals(url))throw new Exception("This feed is already added.");sources.put(new JSONObject().put("name",name).put("url",url));getSharedPreferences("news",0).edit().putString("sources",sources.toString()).apply();settings();
  }else if(action.equals("removeSource")){
   JSONArray old=new JSONArray(getSharedPreferences("news",0).getString("sources","[]")),next=new JSONArray();for(int i=0;i<old.length();i++)if(!old.getJSONObject(i).getString("url").equals(d.optString("url")))next.put(old.getJSONObject(i));getSharedPreferences("news",0).edit().putString("sources",next.toString()).apply();settings();
  }
 }catch(Exception e){error(action,e);}}
 @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);if(result!=RESULT_OK||data==null||data.getData()==null){try{emit("scanCancelled",new JSONObject());}catch(Exception ignored){}return;}
  Uri uri=data.getData();cancelled=new AtomicBoolean(false);busyScan=true;emit("scanning",new JSONObject());
  work.execute(()->{try{if(request==20)emit("duplicates",new JSONObject().put("groups",PhoneTools.duplicates(this,uri,duplicates,cancelled)).put("limits","Scans up to 3,000 files, 10 GB of selected file sizes, 30 directory levels and 512 MB per file. Unreadable, empty, unknown-size and larger files are skipped."));else if(request==21){fileHash=PhoneTools.hash(this,uri,cancelled);emit("fingerprint",new JSONObject().put("hash",fileHash).put("status","Fingerprint calculated. No antivirus verdict has been issued."));}}catch(Exception e){error(request==20?"duplicates":"security",e);}finally{busyScan=false;}});
 }
 @Override public void onRequestPermissionsResult(int r,String[] permissions,int[] grants){super.onRequestPermissionsResult(r,permissions,grants);if(r==13){boolean ok=grants.length>0&&grants[0]==PackageManager.PERMISSION_GRANTED;getSharedPreferences("news",0).edit().putBoolean("notifications",ok).apply();NewsJob.schedule(this);settings();}}
 @Override public void onBackPressed(){web.evaluateJavascript("window.backHome&&window.backHome()",null);}
 @Override protected void onDestroy(){cancelled.set(true);work.shutdownNow();web.removeJavascriptInterface("FaceNative");web.destroy();super.onDestroy();}
}
