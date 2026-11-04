package com.mcperry.facetvnews;
import org.json.JSONArray;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
public class CoreChecks {
 public static void main(String[] args)throws Exception{
  if(!FeedRepository.https("https://facetvnews.com/news/"))throw new AssertionError();
  for(String url:new String[]{"http://example.com","javascript:alert(1)","file:///etc/passwd","https://user:pass@example.com"})if(FeedRepository.https(url))throw new AssertionError(url);
  File cache=Files.createTempDirectory("news-cache-test").toFile();Files.write(new File(cache,"a").toPath(),new byte[10]);File sub=new File(cache,"sub");sub.mkdir();Files.write(new File(sub,"b").toPath(),new byte[20]);
  if(PhoneTools.cacheBytes(cache)!=30||PhoneTools.clearCache(cache)!=30||PhoneTools.cacheBytes(cache)!=0)throw new AssertionError("cache");cache.delete();
  Map<String,PhoneTools.Doc> docs=new HashMap<>();for(String token:new String[]{"a","b"}){PhoneTools.Doc d=new PhoneTools.Doc();d.token=token;d.hash="same";docs.put(token,d);}
  PhoneTools.validateSelection(docs,new JSONArray("[\"a\"]"));
  for(String selection:new String[]{"[]","[\"a\",\"b\"]","[\"unknown\"]","[\"a\",\"a\"]"}){boolean rejected=false;try{PhoneTools.validateSelection(docs,new JSONArray(selection));}catch(Exception e){rejected=true;}if(!rejected)throw new AssertionError(selection);}
  System.out.println("Core checks passed: HTTPS validation, managed-cache deletion and retained-copy rules.");
 }
}
