package com.example.yasu0000.opetimeasure;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Display;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;


//サービスの定義
public class MyService extends Service
{
  private Handler handler=new Handler();
  private boolean running=false;
  private String  message="Message";
  static int onTimes = 0;
  DisplayManager dm;
  static long start = 0;
  static long end = 0;
  public static int i = 0;
  public static String mToday;
  long tot;
  long dOnT = System.currentTimeMillis();
  long onTime =0;
  SQLiteDatabase db;      //データベースオブジェクト
  private final static String DB_NAME = "deviceinf.db";//DB名
  private final static String DB_TABLE = "deviceinf";   //テーブル名
  private final static int DB_VERSION = 1;      //バージョン
  int postCount = 0;
  int nowHour = 0;
  int nowSecond =0;

  //テキストファイルのURLの指定
  private boolean dispOnWas = false;


  //------------------------------------------------------------------
  //サービス生成時に呼ばれる
  //------------------------------------------------------------------
  @Override
  public void onCreate()
  {
    super.onCreate();
    DBHelper dbHelper = new DBHelper(this);
    db = dbHelper.getWritableDatabase();
  }

  //------------------------------------------------------------------
  //サービス開始時に呼ばれる
  //------------------------------------------------------------------
  public int onStartCommand(Intent intent, int flags, final int startId)
  {
    super.onStartCommand(intent,flags,startId);
    //ノティフィケーションの表示
   /* showNotification(this,R.mipmap.ic_launcher,
      "サービスを開始しました",
      "DDC",
      "自作サービスを操作します",
      "情報"); */
    Notification ng = new Notification();
    startForeground(startId,ng);

    //サービスの開始
    final Thread thread=new Thread(){public void run() {
      running=true;
      while (running)
      {
        // 24時に送信
        try
        {
          Calendar cl = Calendar.getInstance();
          nowHour = cl.get(cl.HOUR_OF_DAY);
          nowSecond = cl.get(cl.SECOND);
          if(nowHour!=0&&postCount!=0) postCount=0;
          if(nowHour+postCount ==0)
          {
            int mYear = cl.get(cl.YEAR);
            int mMonth = cl.get(cl.MONTH)+1;
            int mDate = cl.get(cl.DATE);
            int mDOW = cl.get(cl.DAY_OF_WEEK);
            mToday = String.valueOf(mYear)+(mMonth)+(mDate)+(mDOW);

            writeDB(DataCtrl.deviceName,mToday);
            DataCtrl.PostData(i,onTimes,mToday);
            i=0;
            onTimes=0;
            postCount+=1;
          }
        }
        catch (Exception e){String error = String.valueOf(e);}
        dm = (DisplayManager) MyService.this.getSystemService(Context.DISPLAY_SERVICE);
        handler.post(new Runnable(){public void run() {
          if(onTimes==0)
          {
            // 始めはゼロに
            start = System.currentTimeMillis();
            end   = System.currentTimeMillis();
            ++onTimes;
          }
          // ディスプレイがついている
          if(dm.getDisplay(Display.DEFAULT_DISPLAY).getState()==Display.STATE_ON)
          {
            // 直前までディスプレイが消灯していたら
            if(!dispOnWas)
            {
              if(i>=60)
              {
                // 分表示に対応
                int hun   = i/60;
                int amari = i%60;
                toast(MyService.this,"回数:"+onTimes+"   "+"時間:"+(hun+"分"+String.valueOf(amari)+"秒"));
              }
              else {toast(MyService.this,"回数："+onTimes+"   "+"時間:"+(i+"秒"));}
            }
            // テキストの表示
            dispOnWas=true;
            i+=1;
            if(i>=60)
            {
              int hun   = i/60;
              int amari = i%60;
              MeaSurvice.ontimeTv.setText(hun+"分"+String.valueOf(amari)+"秒");
            }
            else {
              MeaSurvice.Companion.getOntimeTv().setText(String.valueOf(i)+"秒");}
            MeaSurvice.Companion.getOntimesTv().setText("本日の表示回数："+onTimes+"回");
          }
          // ディスプレイが消えたら
          if (dm.getDisplay(Display.DEFAULT_DISPLAY).getState() == Display.STATE_OFF)
          {
            start = System.currentTimeMillis() - System.currentTimeMillis();
            if (dispOnWas)
            {
              ++onTimes;
              dispOnWas = false;
            }
          }
        }});
        try {Thread.sleep(1000);}
        catch (Exception e) { }
      }
    }};
    thread.start();
    return START_STICKY;
  }
  //------------------------------------------------------------------
  //サービス停止時に呼ばれる
  //------------------------------------------------------------------
  @Override
  public void onDestroy()
  {
    running=false;
    super.onDestroy();
  }
  //------------------------------------------------------------------
  //サービス接続時に呼ばれる
  //------------------------------------------------------------------
  @Override
  public IBinder onBind(Intent intent) {
    return IMyServiceBinder;
  }
  //------------------------------------------------------------------
  //ノティフィケーションの表示
  //------------------------------------------------------------------
  private static void showNotification(Context context, int iconId,String ticker,String title,String text,String info)
  {
    //ノティフィケーションオブジェクトの生成
    Notification.Builder builder=new Notification.Builder(context);
    builder.setWhen(System.currentTimeMillis());
    builder.setTicker(ticker);
    builder.setContentTitle(title);
    builder.setContentText(text);
    builder.setContentInfo(info);
    builder.setSmallIcon(iconId);
    Intent intent=new Intent(context, MeaSurvice.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    builder.setContentIntent(PendingIntent.getActivity(context,0,intent, PendingIntent.FLAG_ONE_SHOT));

    //ノティフィケーションマネージャの取得
    NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
    //ノティフィケーションのキャンセル
    nm.cancel(0);
    //ノティフィケーションの表示
    nm.notify(0,builder.getNotification());

  }

  // ---------------------------------------------------------------------------------------------------
  //トーストの表示
  // ---------------------------------------------------------------------------------------------------
  private static void toast(Context context,String text) { Toast.makeText(context,text,Toast.LENGTH_SHORT).show();}

  //---------------------------------------------------------
  //HTTP通信
  //---------------------------------------------------------
  public static void http(String path) throws Exception
  {
    byte[] w=new byte[1024];
    HttpURLConnection c=null;
    InputStream in=null;
    ByteArrayOutputStream out=null;
    try
    {
      //HTTP接続のオープン
      java.net.URL url=new URL(path);
      c=(HttpURLConnection)url.openConnection();
      c.setRequestMethod("GET");
      c.connect();
      in=c.getInputStream();

      //バイト配列の読み込み
      out=new ByteArrayOutputStream();
      while (true)
      {
        int size=in.read(w);
        if (size<=0) break;
        out.write(w,0,size);
      }
      out.close();

      //HTTP接続のクローズ
      in.close();
      c.disconnect();
    }
    catch (Exception e)
    {
      try
      {
        if (c!=null) c.disconnect();
        if (in!=null) in.close();
        if (out!=null) out.close();
      }
      catch (Exception e2) { }
      throw e;
    }
  }
  //============================================================================
  //データベースヘルパーの定義
  //============================================================================
  private static class DBHelper extends SQLiteOpenHelper {
    //データベースヘルパーのコンストラクタ
    public DBHelper(Context context) {
      super(context, DB_NAME, null, DB_VERSION);
    }

    //----------------------------------------------------------------------------
    //データベースの生成
    //----------------------------------------------------------------------------
    @Override
    public void onCreate(SQLiteDatabase db)
    {
      db.execSQL("create table if not exists " + DB_TABLE + "(id text primary key, device text, ontime text, ontimes text, date text)");
    }
    //------------------------------------------------------------------
    //データベースのアップグレード
    //------------------------------------------------------------------
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
    {
      db.execSQL("drop table if exists " + DB_TABLE);
      onCreate(db);
    }
  }
  //------------------------------------------------------------------------
  //データベースへの書き込み
  //------------------------------------------------------------------------
  public void writeDB(String deviceName,String today) throws Exception
  {
    ContentValues values = new ContentValues();
    values.put("id", "0");
    values.put("device",deviceName);
    values.put("ontime",String.valueOf(MyService.i));
    values.put("ontimes",String.valueOf(MyService.onTimes));
    values.put("date",today);
    int colNum = db.update(DB_TABLE, values, null, null);
    if (colNum == 0) db.insert(DB_TABLE, "", values);
  }
  //-------------------------------------------------------------------------
  // 計測した情報を送信
  //-------------------------------------------------------------------------
  static void PostData(int dailyOnTime,int dailyOnTimes,String today)
  {
    HttpURLConnection con = null;
    String ontime = String.valueOf(dailyOnTime);
    String ontimes = String.valueOf(dailyOnTimes);
    // 送信するパラメーターの整形
    String PostParameter = "&dailyontime=" + ontime + "&dailyontimes=" + ontimes + "&today=" + today;
    try
    {
      URL url = new URL("");
      con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("POST");
      con.setDoOutput(true);
      con.setFixedLengthStreamingMode(PostParameter.getBytes().length);
      con.setUseCaches(false);
      OutputStream os = con.getOutputStream();
      os.write(PostParameter.getBytes());
      os.flush();
      os.close();
      con.connect();
    }
    catch (Exception e) {String test = e.toString();}
    finally {if (con != null) {con.disconnect();}}
  }
  //------------------------------------------------------------------
  //バインダの生成
  //------------------------------------------------------------------
  private final IMyService.Stub IMyServiceBinder=new IMyService.Stub() {
    public void setMessage(String msg) throws RemoteException {
      message=msg;
    }
  };
}
