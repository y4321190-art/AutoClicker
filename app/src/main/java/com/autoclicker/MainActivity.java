package com.autoclicker;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;
public class MainActivity extends AppCompatActivity {
    private int[] px={0,0},py={0,0};
    private long[] interval={100,100},duration={0,0};
    private boolean[] active={false,false};
    private EditText[] etInterval=new EditText[2],etDuration=new EditText[2];
    private TextView[] tvCoord=new TextView[2];
    private Button[] btnToggle=new Button[2];
    private Button btnGlobal;
    private Switch swSeqMode;
    private WindowManager wm;
    private View overlayView;
    private int pickingIdx=-1;
    protected void onCreate(Bundle s){
        super.onCreate(s);setContentView(R.layout.activity_main);
        wm=(WindowManager)getSystemService(WINDOW_SERVICE);
        swSeqMode=findViewById(R.id.swSeqMode);
        btnGlobal=findViewById(R.id.btnGlobal);
        for(int i=0;i<2;i++){
            int idx=i;
            etInterval[i]=findViewById(i==0?R.id.etInterval1:R.id.etInterval2);
            etDuration[i]=findViewById(i==0?R.id.etDuration1:R.id.etDuration2);
            tvCoord[i]=findViewById(i==0?R.id.tvCoord1:R.id.tvCoord2);
            btnToggle[i]=findViewById(i==0?R.id.btnToggle1:R.id.btnToggle2);
            Button bp=findViewById(i==0?R.id.btnPick1:R.id.btnPick2);
            bp.setOnClickListener(v->pick(idx));
            btnToggle[i].setOnClickListener(v->togglePoint(idx));
        }
        btnGlobal.setOnClickListener(v->toggleAll());
        if(!Settings.canDrawOverlays(this))startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())),1001);
        else if(!accEnabled())startActivityForResult(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS),1002);
    }
    private boolean accEnabled(){
        try{String e=Settings.Secure.getString(getContentResolver(),Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);return e!=null&&e.contains(getPackageName());}catch(Exception e){return false;}
    }
    private void pick(int idx){
        if(!Settings.canDrawOverlays(this)){Toast.makeText(this,"Overlay izni ver",Toast.LENGTH_SHORT).show();return;}
        pickingIdx=idx;
        FrameLayout fl=new FrameLayout(this);
        fl.setBackgroundColor(0x55000033);
        TextView tv=new TextView(this);tv.setText("Nokta "+(idx+1)+" icin dokun");
        tv.setTextColor(0xFFFFFFFF);tv.setTextSize(22);
        fl.addView(tv,new FrameLayout.LayoutParams(-2,-2,Gravity.CENTER));
        overlayView=fl;
        WindowManager.LayoutParams lp=new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT);
        lp.gravity=Gravity.TOP|Gravity.START;
        overlayView.setOnTouchListener((v,e)->{
            if(e.getAction()==MotionEvent.ACTION_DOWN){
                px[pickingIdx]=(int)e.getRawX();py[pickingIdx]=(int)e.getRawY();
                tvCoord[pickingIdx].setText("X:"+px[pickingIdx]+" Y:"+py[pickingIdx]);
                removePicker();
            }return true;
        });
        wm.addView(overlayView,lp);
    }
    private void removePicker(){if(overlayView!=null){try{wm.removeView(overlayView);}catch(Exception ignored){}overlayView=null;}}
    private void togglePoint(int idx){
        readSettings(idx);active[idx]=!active[idx];
        btnToggle[idx].setText(active[idx]?"Kapat":"Ac");
        if(ClickService.isRunning){Intent i=new Intent(idx==0?ClickService.ACTION_TOGGLE1:ClickService.ACTION_TOGGLE2);i.setPackage(getPackageName());sendBroadcast(i);}
    }
    private void toggleAll(){
        if(ClickService.isRunning){
            sendBroadcast(new Intent(ClickService.ACTION_STOP).setPackage(getPackageName()));
            ClickService.isRunning=false;btnGlobal.setText("Baslat");
            for(int i=0;i<2;i++){active[i]=false;btnToggle[i].setText("Ac");}
        }else{
            for(int i=0;i<2;i++)readSettings(i);
            try{
                JSONArray arr=new JSONArray();
                for(int i=0;i<2;i++){if(px[i]==0&&py[i]==0)continue;JSONObject o=new JSONObject();o.put("x",px[i]);o.put("y",py[i]);o.put("interval",interval[i]);o.put("duration",duration[i]*1000L);o.put("active",active[i]);arr.put(o);}
                if(arr.length()==0){Toast.makeText(this,"Once nokta sec",Toast.LENGTH_SHORT).show();return;}
                Intent i=new Intent(ClickService.ACTION_START).setPackage(getPackageName()).putExtra(ClickService.EXTRA_POINTS,arr.toString()).putExtra(ClickService.EXTRA_SEQ_MODE,swSeqMode.isChecked());
                sendBroadcast(i);btnGlobal.setText("Durdur");
            }catch(Exception e){Toast.makeText(this,"Hata:"+e.getMessage(),Toast.LENGTH_SHORT).show();}
        }
    }
    private void readSettings(int idx){
        try{interval[idx]=Math.max(1,Long.parseLong(etInterval[idx].getText().toString()));}catch(Exception ignored){interval[idx]=100;}
        try{duration[idx]=Math.max(0,Long.parseLong(etDuration[idx].getText().toString()));}catch(Exception ignored){duration[idx]=0;}
    }
    protected void onDestroy(){super.onDestroy();removePicker();}
}
