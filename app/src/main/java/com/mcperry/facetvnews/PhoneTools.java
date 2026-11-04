package com.mcperry.facetvnews;
import android.app.ActivityManager;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.provider.DocumentsContract;
import org.json.*;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
public final class PhoneTools {
 public static final class Doc { public String token,name,hash;public Uri uri;public long size; }
 public static long cacheBytes(File f){if(f==null||!f.exists())return 0;if(f.isFile())return f.length();long n=0;File[] files=f.listFiles();if(files!=null)for(File c:files)n+=cacheBytes(c);return n;}
 public static long clearCache(File f){long n=0;File[] files=f.listFiles();if(files!=null)for(File c:files){if(c.isDirectory()){n+=clearCache(c);c.delete();}else{long length=c.length();if(c.delete())n+=length;}}return n;}
 public static JSONObject status(Context c)throws Exception {ActivityManager.MemoryInfo m=new ActivityManager.MemoryInfo();((ActivityManager)c.getSystemService(Context.ACTIVITY_SERVICE)).getMemoryInfo(m);StatFs fs=new StatFs(c.getFilesDir().getAbsolutePath());return new JSONObject().put("cache",cacheBytes(c.getCacheDir())).put("freeStorage",fs.getAvailableBytes()).put("totalStorage",fs.getTotalBytes()).put("freeRam",m.availMem).put("totalRam",m.totalMem).put("lowMemory",m.lowMemory).put("android",Build.VERSION.RELEASE).put("patch",Build.VERSION.SECURITY_PATCH).put("sdk",Build.VERSION.SDK_INT);}
 public static String hash(Context c,Uri uri,AtomicBoolean cancel)throws Exception {MessageDigest digest=MessageDigest.getInstance("SHA-256");try(InputStream in=c.getContentResolver().openInputStream(uri)){if(in==null)throw new IOException("Cannot open file");byte[] buffer=new byte[65536];int n;long read=0;while((n=in.read(buffer))!=-1){if(cancel.get()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Scan cancelled");read+=n;if(read>536870912)throw new IOException("File exceeds 512 MB scan limit");digest.update(buffer,0,n);}}StringBuilder s=new StringBuilder();for(byte b:digest.digest())s.append(String.format(Locale.ROOT,"%02x",b&255));return s.toString();}
 private static void list(Context c,Uri tree,String parent,List<Doc> files,AtomicBoolean cancel,int depth,long[] budget)throws Exception {
  if(depth>30||cancel.get()||files.size()>=3000)return;
  Uri children=DocumentsContract.buildChildDocumentsUriUsingTree(tree,parent);
  try(Cursor cur=c.getContentResolver().query(children,new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID,DocumentsContract.Document.COLUMN_DISPLAY_NAME,DocumentsContract.Document.COLUMN_MIME_TYPE,DocumentsContract.Document.COLUMN_SIZE},null,null,null)){
   if(cur==null)return;while(cur.moveToNext()&&!cancel.get()&&files.size()<3000){String id=cur.getString(0);String name=cur.getString(1);String mime=cur.getString(2);long size=cur.getLong(3);if(DocumentsContract.Document.MIME_TYPE_DIR.equals(mime)){list(c,tree,id,files,cancel,depth+1,budget);}else if(size>0&&size<=536870912&&budget[0]+size<=10L*1024*1024*1024){Doc d=new Doc();d.token=UUID.randomUUID().toString();d.uri=DocumentsContract.buildDocumentUriUsingTree(tree,id);d.name=name;d.size=size;files.add(d);budget[0]+=size;}}
  }
 }
 public static JSONArray duplicates(Context c,Uri tree,Map<String,Doc> index,AtomicBoolean cancel)throws Exception {
  List<Doc> docs=new ArrayList<>();list(c,tree,DocumentsContract.getTreeDocumentId(tree),docs,cancel,0,new long[]{0});Map<Long,List<Doc>> sizes=new HashMap<>();for(Doc d:docs)sizes.computeIfAbsent(d.size,k->new ArrayList<>()).add(d);Map<String,List<Doc>> groups=new LinkedHashMap<>();
  for(List<Doc> same:sizes.values()){if(same.size()<2)continue;for(Doc d:same){if(cancel.get())throw new InterruptedIOException("Scan cancelled");try{d.hash=hash(c,d.uri,cancel);groups.computeIfAbsent(d.hash,k->new ArrayList<>()).add(d);}catch(InterruptedIOException e){throw e;}catch(Exception ignored){}}}
  JSONArray result=new JSONArray();index.clear();for(List<Doc> group:groups.values())if(group.size()>1){JSONArray files=new JSONArray();for(Doc d:group){index.put(d.token,d);files.put(new JSONObject().put("token",d.token).put("name",d.name).put("size",d.size));}result.put(new JSONObject().put("hash",group.get(0).hash).put("files",files));}return result;
 }
 public static void validateSelection(Map<String,Doc> index,JSONArray tokens)throws Exception {
  Set<String> selected=new HashSet<>();for(int i=0;i<tokens.length();i++){String token=tokens.getString(i);if(!index.containsKey(token)||!selected.add(token))throw new IOException("Invalid selection");}
  if(selected.isEmpty())throw new IOException("Select duplicate copies first.");
  for(String token:selected){String hash=index.get(token).hash;boolean retained=false;for(Doc d:index.values())if(hash.equals(d.hash)&&!selected.contains(d.token)){retained=true;break;}if(!retained)throw new IOException("Keep at least one copy from every duplicate group.");}
 }
 public static JSONObject remove(Context c,Map<String,Doc> index,JSONArray tokens)throws Exception {
  validateSelection(index,tokens);Set<String> selected=new HashSet<>();for(int i=0;i<tokens.length();i++)selected.add(tokens.getString(i));long reclaimed=0;int removed=0,failed=0;
  for(String token:selected){Doc d=index.get(token);boolean retained=false;for(Doc keep:index.values())if(d.hash.equals(keep.hash)&&!selected.contains(keep.token)){try{if(keep.hash.equals(hash(c,keep.uri,new AtomicBoolean(false)))){retained=true;break;}}catch(Exception ignored){}}
   try{if(!retained||!d.hash.equals(hash(c,d.uri,new AtomicBoolean(false)))){failed++;continue;}if(DocumentsContract.deleteDocument(c.getContentResolver(),d.uri)){removed++;reclaimed+=d.size;}else failed++;}catch(Exception e){failed++;}
  }index.clear();return new JSONObject().put("removed",removed).put("failed",failed).put("bytes",reclaimed);
 }
}
