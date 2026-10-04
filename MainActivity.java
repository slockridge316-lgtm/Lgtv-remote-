package com.example.lgtvremote;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.*;

public class MainActivity extends Activity {
    static final String TV_IP="192.168.1.243";
    static final String PREF="lgtv";
    EditText keyBox; TextView status; Remote remote;
    int padBg=Color.rgb(20,20,20);
    @Override public void onCreate(Bundle b){super.onCreate(b); showSetup();}
    TextView tv(String s,int size){ TextView v=new TextView(this); v.setText(s); v.setTextColor(Color.WHITE); v.setTextSize(size); v.setGravity(Gravity.CENTER); return v; }
    Button btn(String s){ Button b=new Button(this); b.setText(s); b.setTextColor(Color.WHITE); b.setTextSize(13); b.setAllCaps(false); b.setBackgroundColor(Color.rgb(45,45,45)); return b; }
    void showSetup(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,45,28,28); root.setBackgroundColor(Color.BLACK);
        TextView title=tv("LG TV Remote",28); root.addView(title,new LinearLayout.LayoutParams(-1,70));
        TextView info=tv("Your TV: 192.168.1.243\n\nEnter the LG pairing key from your existing Termux setup.\nIt is saved only on this device.",16); root.addView(info,new LinearLayout.LayoutParams(-1,0,1));
        keyBox=new EditText(this); keyBox.setHint("LG pairing key"); keyBox.setTextColor(Color.WHITE); keyBox.setHintTextColor(Color.GRAY); keyBox.setSingleLine(true); keyBox.setInputType(0x00000081); root.addView(keyBox,new LinearLayout.LayoutParams(-1,60));
        Button connect=btn("Connect to TV"); root.addView(connect,new LinearLayout.LayoutParams(-1,64));
        status=tv("",14); root.addView(status,new LinearLayout.LayoutParams(-1,55)); setContentView(root);
        String saved=getPreferences(0).getString("key",""); if(!saved.isEmpty()) { keyBox.setText(saved); new Handler().postDelayed(()->connect(saved),250); }
        connect.setOnClickListener(v->connect(keyBox.getText().toString().trim()));
    }
    void connect(String key){ if(key.length()<16){status.setText("That key looks too short.");return;} status.setText("Connecting…"); remote=new Remote(key,()->runOnUiThread(this::showRemote),e->runOnUiThread(()->status.setText("Connection failed: "+e))); remote.start(); }
    void showRemote(){
        getPreferences(0).edit().putString("key",remote.key).apply();
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.BLACK);
        LinearLayout top=new LinearLayout(this); top.setPadding(8,4,8,4); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView st=tv("●  Connected",14); st.setTextColor(Color.LTGRAY); top.addView(st,new LinearLayout.LayoutParams(0,48,1));
        Button home=btn("⌂"); home.setOnClickListener(v->remote.button("HOME")); top.addView(home,new LinearLayout.LayoutParams(55,48));
        Button back=btn("‹"); back.setOnClickListener(v->remote.button("BACK")); top.addView(back,new LinearLayout.LayoutParams(55,48));
        root.addView(top);
        FrameLayout pad=new FrameLayout(this); pad.setBackgroundColor(padBg);
        TextView hint=tv("LG TV TOUCHPAD\n\nDrag to move  •  Tap to click",17); hint.setAlpha(.38f); pad.addView(hint,new FrameLayout.LayoutParams(-1,-1));
        TouchView touch=new TouchView(this,remote); pad.addView(touch,new FrameLayout.LayoutParams(-1,-1)); root.addView(pad,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout controls=new LinearLayout(this); controls.setGravity(Gravity.CENTER); String[] labels={"−","Mute","+","Back","Home"}; String[] cmds={"VOLDOWN","MUTE","VOLUP","BACK","HOME"};
        for(int i=0;i<labels.length;i++){Button x=btn(labels[i]); final String c=cmds[i]; x.setOnClickListener(v->remote.button(c)); controls.addView(x,new LinearLayout.LayoutParams(0,60,1));} root.addView(controls);
        setContentView(root);
    }
    static class TouchView extends View { Remote r; float x,y; long down; boolean moved;
        TouchView(Context c,Remote rr){super(c);r=rr;setBackgroundColor(Color.TRANSPARENT);setClickable(true);}
        public boolean onTouchEvent(android.view.MotionEvent e){ switch(e.getActionMasked()){
            case MotionEvent.ACTION_DOWN:x=e.getX();y=e.getY();down=System.currentTimeMillis();moved=false;return true;
            case MotionEvent.ACTION_MOVE: float dx=e.getX()-x,dy=e.getY()-y;x=e.getX();y=e.getY();if(Math.abs(dx)+Math.abs(dy)>1){moved=true;r.move(Math.round(dx*2.2f),Math.round(dy*2.2f));}return true;
            case MotionEvent.ACTION_UP: if(!moved && System.currentTimeMillis()-down<450)r.click();return true; }
            return true; }
    }
    static class Remote {
        final String key; final OkHttpClient client=new OkHttpClient.Builder().retryOnConnectionFailure(true).build(); WebSocket ssap,pointer; AtomicInteger ids=new AtomicInteger(); Runnable ok; java.util.function.Consumer<String> fail;
        Remote(String k,Runnable o,java.util.function.Consumer<String> f){key=k;ok=o;fail=f;}
        void start(){ Request q=new Request.Builder().url("ws://"+TV_IP+":3000").header("Origin","null").build(); ssap=client.newWebSocket(q,new WebSocketListener(){
            public void onOpen(WebSocket w,Response r){ register(w); }
            public void onMessage(WebSocket w,String s){try{JSONObject j=new JSONObject(s); String type=j.optString("type"),id=j.optString("id"); if("registered".equals(type)){getSocket(w);return;} if("response".equals(type)&&"pointer_socket".equals(id)){String p=j.getJSONObject("payload").getString("socketPath"); connectPointer(p);}}catch(Exception e){fail.accept(e.getMessage());}}
            public void onFailure(WebSocket w,Throwable t){fail.accept(t.getMessage()==null?"WebSocket error":t.getMessage());}
        }); }
        void register(WebSocket w){try{JSONObject m=new JSONObject();m.put("manifestVersion",1);m.put("appVersion","1.0");JSONArray p=new JSONArray();for(String x:new String[]{"LAUNCH","CONTROL_APP_LAUNCHER","CONTROL_AUDIO","CONTROL_MOUSE_AND_KEYBOARD","READ_INSTALLED_APPS","READ_RUNNING_APPS","READ_TV_STATE","READ_INPUTS"})p.put(x);m.put("permissions",p);JSONObject a=new JSONObject();a.put("client-key",key);a.put("forcePairing",false);a.put("manifest",m);JSONObject z=new JSONObject();z.put("type","register");z.put("payload",a);w.send(z.toString());}catch(Exception e){fail.accept(e.getMessage());}}
        void getSocket(WebSocket w){request("ssap://com.webos.service.networkinput/getPointerInputSocket",new JSONObject(),"pointer_socket");}
        void request(String uri,JSONObject payload){request(uri,payload,"r"+ids.incrementAndGet());}
        void request(String uri,JSONObject payload,String id){try{JSONObject z=new JSONObject();z.put("type","request");z.put("id",id);z.put("uri",uri);z.put("payload",payload);ssap.send(z.toString());}catch(Exception e){}}
        void connectPointer(String path){pointer=client.newWebSocket(new Request.Builder().url(path).header("Origin","null").build(),new WebSocketListener(){public void onOpen(WebSocket w,Response r){ok.run();}public void onFailure(WebSocket w,Throwable t){fail.accept("Pointer connection failed");}});}
        synchronized void raw(String s){if(pointer!=null)pointer.send(s);}
        void move(int dx,int dy){raw("type:move\ndx:"+dx+"\ndy:"+dy+"\n\n");}
        void click(){raw("type:click\n\n");}
        void button(String b){String key=null; if(b.equals("BACK"))key="BACK"; else if(b.equals("HOME"))key="HOME"; else if(b.equals("VOLUP"))key="VOLUMEUP"; else if(b.equals("VOLDOWN"))key="VOLUMEDOWN"; else if(b.equals("MUTE"))key="MUTE"; if(key!=null){try{JSONObject p=new JSONObject();p.put("button",key);request("ssap://com.webos.service.networkinput/sendKey",p);}catch(Exception e){}}}
    }
}
