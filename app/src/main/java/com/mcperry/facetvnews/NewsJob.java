package com.mcperry.facetvnews;
import android.app.*;
import android.app.job.*;
import android.content.*;
import android.os.Build;
import android.Manifest;
import android.content.pm.PackageManager;
import org.json.*;
import java.util.*;

public class NewsJob extends JobService {
 public static final int JOB=9301;private volatile boolean stopped;private Thread worker;
 public static void schedule(Context c){
  JobScheduler js=(JobScheduler)c.getSystemService(JOB_SCHEDULER_SERVICE);
  if(!c.getSharedPreferences("news",0).getBoolean("notifications",false)){js.cancel(JOB);js.cancel(JOB+1);return;}
  long minutes=30;try{minutes=Math.max(15,FeedRepository.config(c).optLong("pollMinutes",30));}catch(Exception ignored){}
  js.schedule(new JobInfo.Builder(JOB,new ComponentName(c,NewsJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setPeriodic(minutes*60_000).setPersisted(true).build());
  if(!c.getSharedPreferences("news",0).getBoolean("baseline",false))js.schedule(new JobInfo.Builder(JOB+1,new ComponentName(c,NewsJob.class)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY).setMinimumLatency(1000).build());
 }
 @Override public boolean onStartJob(JobParameters params){stopped=false;worker=new Thread(()->{boolean retry=false;try{check();}catch(Exception e){retry=true;}if(!stopped)jobFinished(params,retry);},"news-notifications");worker.start();return true;}
 @Override public boolean onStopJob(JobParameters params){stopped=true;if(worker!=null)worker.interrupt();return true;}
 private void check()throws Exception {
  android.content.SharedPreferences p=getSharedPreferences("news",0);if(!p.getBoolean("notifications",false))return;
  Set<String> previous=new HashSet<>(p.getStringSet("seen",Collections.emptySet()));boolean baseline=p.getBoolean("baseline",false);Set<String> next=new HashSet<>(previous);List<JSONObject> fresh=new ArrayList<>();
  for(int page=1;page<=5&&!stopped;page++){
   JSONObject result=FeedRepository.primary(this,page,"","");JSONArray posts=result.getJSONArray("posts");boolean allNew=posts.length()>0;
   for(int i=0;i<posts.length();i++){JSONObject r=posts.getJSONObject(i);String key=r.optString("id",r.getString("url"));next.add(key);if(previous.contains(key))allNew=false;else if(baseline)fresh.add(r);}
   if(!baseline||!allNew||!result.optBoolean("more"))break;
  }
  if(stopped)return;
  // Retain a bounded set. If oversized, rebaseline rather than risk historical re-notification.
  if(next.size()>2000){next.clear();JSONObject first=FeedRepository.primary(this,1,"","");JSONArray posts=first.getJSONArray("posts");for(int i=0;i<posts.length();i++)next.add(posts.getJSONObject(i).optString("id"));}
  p.edit().putStringSet("seen",next).putBoolean("baseline",true).apply();
  if(fresh.isEmpty()||Build.VERSION.SDK_INT>=33&&checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)return;
  NotificationManager manager=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);manager.createNotificationChannel(new NotificationChannel("news","FaceTVNews headlines",NotificationManager.IMPORTANCE_DEFAULT));
  for(int i=0;i<Math.min(5,fresh.size());i++){
   JSONObject r=fresh.get(i);Intent intent=new Intent(this,ReaderActivity.class).putExtra("url",r.getString("url")).putExtra("title",r.getString("title"));
   PendingIntent open=PendingIntent.getActivity(this,r.getString("url").hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
   Notification n=new Notification.Builder(this,"news").setSmallIcon(R.drawable.ic_logo).setContentTitle(r.getString("title")).setContentText("New on FaceTVNews").setStyle(new Notification.BigTextStyle().bigText(r.optString("excerpt"))).setAutoCancel(true).setContentIntent(open).setGroup("facetvnews").build();manager.notify(r.getString("url").hashCode(),n);
  }
  if(fresh.size()>5){Intent intent=new Intent(this,MainActivity.class);manager.notify(9000,new Notification.Builder(this,"news").setSmallIcon(R.drawable.ic_logo).setContentTitle(fresh.size()+" new FaceTVNews stories").setContentText("Open the app for every headline.").setGroup("facetvnews").setGroupSummary(true).setContentIntent(PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE)).setAutoCancel(true).build());}
 }
}
