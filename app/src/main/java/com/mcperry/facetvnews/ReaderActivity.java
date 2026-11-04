package com.mcperry.facetvnews;
import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.os.*;
import android.webkit.*;
import android.widget.*;
import android.view.*;
public class ReaderActivity extends Activity {
 private WebView web;private TextView status;private String url;
 @Override public void onCreate(Bundle b){super.onCreate(b);url=getIntent().getStringExtra("url");if(!FeedRepository.https(url)){finish();return;}
  LinearLayout layout=new LinearLayout(this);layout.setOrientation(LinearLayout.VERTICAL);layout.setBackgroundColor(Color.WHITE);layout.setPadding(0,0,0,0);
  layout.setOnApplyWindowInsetsListener((v,insets)->{v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());return insets;});
  LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);Button back=new Button(this);back.setText("‹ Back");back.setOnClickListener(v->finish());bar.addView(back);
  status=new TextView(this);status.setText(getIntent().getStringExtra("title"));status.setMaxLines(2);status.setPadding(8,4,8,4);bar.addView(status,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1));Button share=new Button(this);share.setText("Share");share.setOnClickListener(v->{Intent intent=new Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT,url);startActivity(Intent.createChooser(intent,"Share story"));});bar.addView(share);layout.addView(bar);
  web=new WebView(this);layout.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(layout);
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setSafeBrowsingEnabled(true);s.setMediaPlaybackRequiresUserGesture(true);
  web.setWebViewClient(new WebViewClient(){@Override public boolean shouldOverrideUrlLoading(WebView v,WebResourceRequest r){String next=r.getUrl().toString();if(FeedRepository.https(next)){url=next;return false;}if("mailto".equals(r.getUrl().getScheme())||"tel".equals(r.getUrl().getScheme())){new AlertDialog.Builder(ReaderActivity.this).setMessage("Open another app for this link?").setNegativeButton("Cancel",null).setPositiveButton("Open",(d,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW,r.getUrl()));}catch(Exception ignored){}}).show();}return true;}
   @Override public void onReceivedError(WebView v,WebResourceRequest req,WebResourceError e){if(req.isForMainFrame())status.setText("Unable to load this page. Check your connection or publisher access.");}
  });web.loadUrl(url);if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->{if(web.canGoBack())web.goBack();else finish();});
 }
 @Override public void onBackPressed(){if(web.canGoBack())web.goBack();else super.onBackPressed();}
 @Override protected void onDestroy(){if(web!=null){web.stopLoading();web.destroy();}super.onDestroy();}
}
