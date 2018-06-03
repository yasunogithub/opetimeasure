package com.example.yasu0000.opetimeasure;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.sql.DriverManager;
import java.sql.Connection;

/**
 * Created by yasu0000 on 2016/11/23.
 *
 */

public  class DataCtrl {
  // DB
  private final static String DB_NAME = "deviceinf.db";//DB名
  private final static String DB_TABLE = "deviceinf";   //テーブル名
  private final static int DB_VERSION = 1;      //バージョン
  static SQLiteDatabase db;      //データベースオブジェクト
  public static final UUID u1 = UUID.randomUUID();
  final static String  deviceName = Build.BRAND + Build.DEVICE + "%=" + u1 + System.currentTimeMillis() + "=%";
  static Context context;
  static DBHelper dbHelper = new DBHelper(context);
  //データベースへの書き込み
  public static void writeDB(String deviceInfo,String today) throws Exception {
    db = dbHelper.getWritableDatabase();
    //データベースオブジェクトの取得
    ContentValues values = new ContentValues();
    values.put("id", "0");
    values.put("device",deviceInfo);
    values.put("ontime",String.valueOf(MyService.i));
    values.put("ontimes",String.valueOf(MyService.onTimes));
    values.put("date",today);
    int colNum = db.update(DB_TABLE, values, null, null);
    if (colNum == 0) db.insert(DB_TABLE, "", values);
  }


  // POST
  static void PostData(int dailyOnTime,int dailyOnTimes,String today) {
    HttpURLConnection con = null;

    String ontime = String.valueOf(dailyOnTime);
    String ontimes = String.valueOf(dailyOnTimes);
    String PostParameter = "devicename=" + deviceName + "&dailyontime=" + ontime + "&dailyontimes=" + ontimes + "&today=" + today;
    try {
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
    catch (Exception e) {
      String test = e.toString();
    }
    finally {
      if (con != null) {
        con.disconnect();
      }
    }
  }




    //データベースヘルパーの定義
  private static class DBHelper extends SQLiteOpenHelper {
    //データベースヘルパーのコンストラクタ
    public DBHelper(Context context) {
      super(context, DB_NAME, null, DB_VERSION);
    }

    //データベースの生成
    @Override
    public void onCreate(SQLiteDatabase db) {
      db.execSQL("create table if not exists " +
        DB_TABLE + "(id text primary key, device text, ontime text, ontimes text, date text)");


    }

    //データベースのアップグレード
    @Override
    public void onUpgrade(SQLiteDatabase db,
                          int oldVersion, int newVersion) {
      db.execSQL("drop talbe if exists " + DB_TABLE);
      onCreate(db);
    }

    }
}