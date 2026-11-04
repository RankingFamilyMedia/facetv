package com.mcperry.facetvnews;
import android.content.Context;
import android.text.Html;
import android.util.Xml;
import org.json.*;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.*;

public final class FeedRepository {
 public static JSONObject config(Context c) throws Exception { try(InputStream in=c.getAssets().open("sources.json")){return new JSONObject(new String(read(in),StandardCharsets.UTF_8));} }
 private static byte[] read(InputStream in)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] a=new byte[8192];int n;while((n=in.read(a))!=-1)b.write(a,0,n);return b.toByteArray();}
 public static boolean https(String url){try { URI u=new URI(url);return "https".equalsIgnoreCase(u.getScheme())&&u.getHost()!=null&&u.getUserInfo()==null;}catch(Exception e){return false;}}
 private static String endpoint(String base,String suffix){
  int query=suffix.indexOf('?');
  if(!base.contains("rest_route="))return base+suffix;
  return base+suffix.substring(0,query<0?suffix.length():query)+(query<0?"":"&"+suffix.substring(query+1));
 }
 public static byte[] fetch(String target) throws Exception {
  String url=target;
  for(int redirects=0;redirects<5;redirects++) {
   if(!https(url))throw new IOException("Only HTTPS sources are allowed.");
   HttpURLConnection h=(HttpURLConnection)new URL(url).openConnection();h.setConnectTimeout(12000);h.setReadTimeout(15000);h.setInstanceFollowRedirects(false);h.setRequestProperty("User-Agent","FaceTVNews-Android/1.0");h.setRequestProperty("Accept","application/json,application/rss+xml,application/atom+xml,text/xml");
   try {
    int code=h.getResponseCode();if(code>=300&&code<400){String location=h.getHeaderField("Location");if(location==null)throw new IOException("Missing redirect location");url=new URI(url).resolve(location).toString();continue;}
    if(code!=200){String detail="";if(code==400){try(InputStream err=h.getErrorStream()){if(err!=null){byte[] small=new byte[4096];int n=err.read(small);if(n>0)detail=new String(small,0,n,StandardCharsets.UTF_8);}}}throw new IOException("Source returned HTTP "+code+ (detail.contains("rest_post_invalid_page_number")?" (rest_post_invalid_page_number)":""));}
    try(InputStream in=h.getInputStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
     byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>5_000_000)throw new IOException("Feed too large");out.write(b,0,n);}return out.toByteArray();
    }
   }finally{h.disconnect();}
  }throw new IOException("Too many redirects");
 }
 public static String plain(String html){return Html.fromHtml(html,Html.FROM_HTML_MODE_LEGACY).toString().trim();}
 public static JSONObject post(JSONObject p,String publisher)throws Exception {
  JSONObject r=new JSONObject();r.put("id",p.optString("id"));r.put("title",plain(p.optJSONObject("title")==null?"Untitled":p.optJSONObject("title").optString("rendered")));r.put("excerpt",plain(p.optJSONObject("excerpt")==null?"":p.optJSONObject("excerpt").optString("rendered")));r.put("url",p.optString("link"));r.put("date",p.optString("date_gmt")+"Z");r.put("publisher",publisher);r.put("image","");r.put("category","News");
  String image=wordpressImage(p);if(https(image))r.put("image",image);
  JSONObject embedded=p.optJSONObject("_embedded");if(embedded!=null){
   JSONArray terms=embedded.optJSONArray("wp:term");if(terms!=null)for(int i=0;i<terms.length();i++){JSONArray group=terms.optJSONArray(i);if(group==null)continue;for(int j=0;j<group.length();j++){JSONObject t=group.optJSONObject(j);if(t!=null&&"category".equals(t.optString("taxonomy"))){r.put("category",t.optString("name","News"));break;}}}
  }return r;
 }
 private static String wordpressImage(JSONObject p){
  JSONObject embedded=p.optJSONObject("_embedded");
   if(embedded!=null){JSONArray media=embedded.optJSONArray("wp:featuredmedia");if(media!=null&&media.length()>0){String image=mediaImage(media.optJSONObject(0));if(!image.isEmpty())return image;}}
  String[] keys={"jetpack_featured_media_url","featured_image","featuredImage","thumbnail","image"};
  for(String key:keys){Object raw=p.opt(key);String image=raw instanceof JSONObject?mediaImage((JSONObject)raw):imageUrl(raw==null?"":String.valueOf(raw));if(!image.isEmpty())return image;}
  String[] markupKeys={"content","excerpt"};
  for(String key:markupKeys){JSONObject markup=p.optJSONObject(key);if(markup!=null){String image=imageFromMarkup(markup.optString("rendered"));if(!image.isEmpty())return image;}}
  return "";
 }
 private static String mediaImage(JSONObject item){
  if(item==null)return "";
  String[] keys={"source_url","sourceUrl","url","image"};
  for(String key:keys){String image=imageUrl(item.optString(key,""));if(!image.isEmpty())return image;}
  JSONObject details=item.optJSONObject("media_details");if(details!=null){JSONObject sizes=details.optJSONObject("sizes");if(sizes!=null){String[] preferred={"large","medium_large","medium","thumbnail"};for(String key:preferred){String image=mediaImage(sizes.optJSONObject(key));if(!image.isEmpty())return image;}}}
  return "";
 }
 public static JSONObject primary(Context c,int page,String category,String search)throws Exception {
  JSONObject cfg=config(c);String query="/posts?_embed&per_page=20&page="+page;
  if(!category.isEmpty())query+="&categories="+URLEncoder.encode(category,"UTF-8");if(!search.isEmpty())query+="&search="+URLEncoder.encode(search,"UTF-8");
  try {JSONArray data=new JSONArray(new String(fetch(endpoint(cfg.getString("api"),query)),StandardCharsets.UTF_8));JSONArray posts=new JSONArray();for(int i=0;i<data.length();i++){JSONObject r=post(data.getJSONObject(i),cfg.getString("name"));if(https(r.optString("url")))posts.put(r);}return new JSONObject().put("posts",posts).put("page",page).put("more",data.length()==20).put("method","WordPress API").put("cached",false);}
  catch(Exception e){if(page>1&&e.getMessage()!=null&&e.getMessage().contains("rest_post_invalid_page_number"))return new JSONObject().put("posts",new JSONArray()).put("page",page).put("more",false).put("method","WordPress API").put("cached",false);if(page>1||!category.isEmpty()||!search.isEmpty())throw e;return new JSONObject().put("posts",rss(cfg.getString("feed"),cfg.getString("name"))).put("page",1).put("more",false).put("method","RSS fallback").put("cached",false);}
 }
 public static JSONArray categories(Context c)throws Exception {JSONObject cfg=config(c);JSONArray raw=new JSONArray(new String(fetch(endpoint(cfg.getString("api"),"/categories?per_page=100&hide_empty=true")),StandardCharsets.UTF_8));JSONArray result=new JSONArray();for(int i=0;i<raw.length();i++){JSONObject r=raw.getJSONObject(i);result.put(new JSONObject().put("id",r.getInt("id")).put("name",plain(r.getString("name"))));}return result;}
 public static JSONObject trending(Context c,JSONArray sources)throws Exception {
  JSONObject cfg=config(c);LinkedHashMap<String,JSONObject> unique=new LinkedHashMap<>();
  String curatedUrl=cfg.optString("trendingApi","").trim();
  if(!curatedUrl.isEmpty())try{JSONArray curated=curated(c,curatedUrl,cfg.optString("name","FaceTV News"));if(curated.length()>0)return new JSONObject().put("posts",curated).put("method","FaceTVNews curated trends").put("curated",true).put("sourceCount",curated.length());}catch(Exception ignored){}
  try{JSONArray latest=primary(c,1,"","").getJSONArray("posts");for(int i=0;i<latest.length();i++)addUnique(unique,latest.getJSONObject(i));}catch(Exception ignored){}
  if(sources!=null)for(int i=0;i<sources.length();i++){JSONObject source=sources.optJSONObject(i);if(source==null)continue;try{JSONArray feed=rss(source.getString("url"),source.optString("name","Publisher feed"));for(int j=0;j<feed.length();j++)addUnique(unique,feed.getJSONObject(j));}catch(Exception ignored){}}
  if(unique.isEmpty())throw new IOException("No trend stories are available right now.");
  Map<String,Integer> terms=new HashMap<>();for(JSONObject item:unique.values())addTerms(terms,item.optString("title"));
  List<JSONObject> ranked=new ArrayList<>(unique.values());for(JSONObject item:ranked){int score=trendScore(item,terms);item.put("trendScore",score);item.put("trendReason",score>=90?"Fresh and widely mentioned":"Fresh from available feeds");}
  ranked.sort((a,b)->Integer.compare(b.optInt("trendScore"),a.optInt("trendScore")));
  JSONArray out=new JSONArray();for(int i=0;i<Math.min(12,ranked.size());i++)out.put(ranked.get(i));
  return new JSONObject().put("posts",out).put("method","Live feed trend ranking").put("curated",false).put("sourceCount",unique.size());
 }
 private static JSONArray curated(Context c,String url,String publisher)throws Exception {
  Object raw=new JSONTokener(new String(fetch(url),StandardCharsets.UTF_8)).nextValue();JSONArray data=raw instanceof JSONArray?(JSONArray)raw:null;
  if(data==null&&raw instanceof JSONObject){JSONObject object=(JSONObject)raw;data=object.optJSONArray("posts");if(data==null)data=object.optJSONArray("items");}
  if(data==null)return new JSONArray();JSONArray out=new JSONArray();for(int i=0;i<data.length();i++){JSONObject item=data.optJSONObject(i);if(item==null)continue;JSONObject normalized=item.optJSONObject("title")!=null?post(item,publisher):new JSONObject(item.toString());if(https(normalized.optString("url")))out.put(normalized);}return out;
 }
 private static void addUnique(Map<String,JSONObject> items,JSONObject item){String url=item.optString("url");if(https(url)&&!items.containsKey(url))items.put(url,item);}
 private static void addTerms(Map<String,Integer> terms,String title){for(String token:title.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{Nd}]+"," ").trim().split("\\s+"))if(token.length()>3&&!Arrays.asList("about","after","before","from","have","into","that","their","this","with","your").contains(token))terms.put(token,terms.getOrDefault(token,0)+1);}
 private static int trendScore(JSONObject item,Map<String,Integer> terms){int score=0;long stamp=when(item.optString("date"));if(stamp>0){long hours=Math.max(0,(System.currentTimeMillis()-stamp)/3600000);score=hours<24?80:hours<72?60:hours<168?35:15;}for(String token:item.optString("title").toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{Nd}]+"," ").trim().split("\\s+"))score+=Math.min(24,terms.getOrDefault(token,0)*4);if("FaceTV News".equalsIgnoreCase(item.optString("publisher")))score+=8;return score;}
 private static long when(String value){try{return Instant.parse(value).toEpochMilli();}catch(Exception ignored){}try{return ZonedDateTime.parse(value,DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli();}catch(Exception ignored){return 0;}}
 public static JSONArray rss(String url,String publisher)throws Exception {
  XmlPullParser p=Xml.newPullParser();p.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,true);byte[] xml=fetch(url);if(new String(xml,StandardCharsets.UTF_8).toUpperCase(Locale.ROOT).contains("<!DOCTYPE"))throw new IOException("DTD is not allowed");p.setInput(new ByteArrayInputStream(xml),null);JSONArray out=new JSONArray();JSONObject current=null;String capture=null;int depth=0,captureDepth=0;StringBuilder val=new StringBuilder();
  for(int event=p.getEventType();event!=XmlPullParser.END_DOCUMENT;event=p.next()){
   if(event==XmlPullParser.DOCDECL)throw new IOException("DTD is not allowed");
   if(event==XmlPullParser.START_TAG){depth++;String name=p.getName();if(name.equals("item")||name.equals("entry")){current=new JSONObject().put("publisher",publisher).put("category","External feed").put("image","");}
    else if(current!=null){if(name.equals("link")&&p.getAttributeValue(null,"href")!=null){String rel=p.getAttributeValue(null,"rel");if(rel==null||rel.equals("alternate"))current.put("url",p.getAttributeValue(null,"href"));}
     if(name.equals("img")){setImage(current,attribute(p,"src"));setImage(current,attribute(p,"data-src"));setImage(current,attribute(p,"data-lazy-src"));}
     if(name.equals("thumbnail")||name.equals("content")||name.equals("enclosure")){String im=attribute(p,"url");if(!imageUrl(im).isEmpty()&&("image".equals(attribute(p,"medium"))||name.equals("thumbnail")||attribute(p,"type").startsWith("image/")))setImage(current,im);}
     if(capture==null&&Arrays.asList("title","link","description","summary","encoded","pubDate","published","updated","guid","id").contains(name)){capture=name;captureDepth=depth;val.setLength(0);}
    }
   }else if((event==XmlPullParser.TEXT||event==XmlPullParser.CDSECT)&&capture!=null)val.append(p.getText());
   else if(event==XmlPullParser.END_TAG){String name=p.getName();if(current!=null&&capture!=null&&depth==captureDepth){String v=val.toString().trim();switch(capture){case "title":current.put("title",plain(v));break;case "link":if(!v.isEmpty())current.put("url",v);break;case "description":case "summary":case "encoded":setImage(current,imageFromMarkup(v));current.put("excerpt",plain(v));break;case "guid":case "id":current.put("id",v);break;default:current.put("date",v);}capture=null;}
    if(current!=null&&(name.equals("item")||name.equals("entry"))){if(https(current.optString("url"))){if(current.optString("id").isEmpty())current.put("id",current.optString("url"));out.put(current);}current=null;if(out.length()>=200)break;}depth--;
   }
   }return out;
  }
 private static String imageUrl(String raw){if(raw==null)return "";String image=raw.trim();if(image.startsWith("https://"))return image;if(image.startsWith("http://"))return "https://"+image.substring(7);return "";}
 private static void setImage(JSONObject item,String image){String safe=imageUrl(image);if(item!=null&&!safe.isEmpty()&&item.optString("image").isEmpty())try{item.put("image",safe);}catch(JSONException ignored){}}
 private static String attribute(XmlPullParser p,String wanted){for(int i=0;i<p.getAttributeCount();i++)if(wanted.equalsIgnoreCase(p.getAttributeName(i)))return p.getAttributeValue(i);return "";}
 private static String imageFromMarkup(String markup){if(markup==null||markup.isEmpty())return "";Matcher m=Pattern.compile("(?i)(?:src|data-src|data-lazy-src)\\s*=\\s*[\\\"'](https?://[^\\\"']+)[\\\"']").matcher(markup);return m.find()?imageUrl(m.group(1)):"";}
 }
