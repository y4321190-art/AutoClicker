package com.autoclicker;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
public class ClickService extends AccessibilityService {
    public static final String ACTION_START="com.autoclicker.START";
    public static final String ACTION_STOP="com.autoclicker.STOP";
    public static final String ACTION_TOGGLE1="com.autoclicker.TOGGLE1";
    public static final String ACTION_TOGGLE2="com.autoclicker.TOGGLE2";
    public static final String EXTRA_POINTS="points";
    public static final String EXTRA_SEQ_MODE="seq_mode";
    public static boolean isRunning=false;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private static class CP {
        int x,y;long iv,dv;boolean active;long st;
        CP(int x,int y,long iv,long dv,boolean a){this.x=x;this.y=y;this.iv=iv;this.dv=dv;this.active=a;this.st=System.currentTimeMillis();}
    }
    private final List<CP> points=new ArrayList<>();
    private boolean seqMode=false;
    private int seqIdx=0;
    private boolean clicking=false;
    private final Runnable parallelTick=new Runnable(){public void run(){
        if(!clicking)return;
        long now=System.currentTimeMillis();boolean any=false;
        for(CP p:points){if(!p.active)continue;if(p.dv>0&&(now-p.st)>=p.dv){p.active=false;continue;}any=true;tap(p.x,p.y);}
        if(any){long min=Long.MAX_VALUE;for(CP p:points)if(p.active)min=Math.min(min,p.iv);handler.postDelayed(this,min==Long.MAX_VALUE?100:min);}
        else{clicking=false;isRunning=false;}
    }};
    private final Runnable seqTick=new Runnable(){public void run(){
        if(!clicking)return;
        List<CP> a=new ArrayList<>();long now=System.currentTimeMillis();
        for(CP p:points)if(p.active){if(p.dv>0&&(now-p.st)>=p.dv)p.active=false;else a.add(p);}
        if(a.isEmpty()){clicking=false;isRunning=false;return;}
        seqIdx%=a.size();CP c=a.get(seqIdx);tap(c.x,c.y);long d=c.iv;seqIdx=(seqIdx+1)%a.size();handler.postDelayed(this,d);
    }};
    private final BroadcastReceiver recv=new BroadcastReceiver(){public void onReceive(Context ctx,Intent i){
        String a=i.getAction();if(a==null)return;
        if(a.equals(ACTION_START))start(i);
        else if(a.equals(ACTION_STOP))stop();
        else if(a.equals(ACTION_TOGGLE1))toggle(0);
        else if(a.equals(ACTION_TOGGLE2))toggle(1);
    }};
    public void onServiceConnected(){
        IntentFilter f=new IntentFilter();
        f.addAction(ACTION_START);f.addAction(ACTION_STOP);f.addAction(ACTION_TOGGLE1);f.addAction(ACTION_TOGGLE2);
        registerReceiver(recv,f,Context.RECEIVER_NOT_EXPORTED);isRunning=false;
    }
    public void onAccessibilityEvent(AccessibilityEvent e){}
    public void onInterrupt(){stop();}
    public void onDestroy(){super.onDestroy();unregisterReceiver(recv);stop();}
    private void start(Intent i){
        points.clear();seqMode=i.getBooleanExtra(EXTRA_SEQ_MODE,false);seqIdx=0;
        String json=i.getStringExtra(EXTRA_POINTS);
        if(json!=null)try{JSONArray arr=new JSONArray(json);for(int j=0;j<arr.length();j++){JSONObject o=arr.getJSONObject(j);points.add(new CP(o.getInt("x"),o.getInt("y"),o.getLong("interval"),o.getLong("duration"),o.getBoolean("active")));}}catch(Exception ignored){}
        clicking=true;isRunning=true;handler.removeCallbacks(parallelTick);handler.removeCallbacks(seqTick);handler.post(seqMode?seqTick:parallelTick);
    }
    private void stop(){clicking=false;isRunning=false;handler.removeCallbacks(parallelTick);handler.removeCallbacks(seqTick);}
    private void toggle(int idx){if(idx<points.size()){CP p=points.get(idx);p.active=!p.active;p.st=System.currentTimeMillis();}}
    private void tap(int x,int y){
        Path path=new Path();path.moveTo(x,y);
        GestureDescription g=new GestureDescription.Builder().addStroke(new GestureDescription.StrokeDescription(path,0,1)).build();
        dispatchGesture(g,null,null);
    }
}
