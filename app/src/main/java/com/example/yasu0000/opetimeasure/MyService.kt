package com.example.yasu0000.opetimeasure

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.hardware.display.DisplayManager
import android.icu.text.AlphabeticIndex
import android.os.Handler
import android.os.IBinder
import android.os.RemoteException
import android.provider.BaseColumns
import android.view.Display
import android.widget.Toast

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Connection



//サービスの定義
class MyService : Service() {
  private val handler = Handler()
  private var running = false
  private var message = "Message"
  internal lateinit var dm: DisplayManager
  internal var tot: Long = 0
  internal var dOnT = System.currentTimeMillis()
  internal var onTime: Long = 0
  internal lateinit var db: SQLiteDatabase      //データベースオブジェクト
  internal var postCount = 0
  internal var nowHour = 0
  internal var nowSecond = 0

  //テキストファイルのURLの指定
  private var dispOnWas = false
  //------------------------------------------------------------------
  //バインダの生成
  //------------------------------------------------------------------
  private val IMyServiceBinder = object : IMyService.Stub() {
    @Throws(RemoteException::class)
    override fun setMessage(msg: String) {
      message = msg
    }
  }
  //------------------------------------------------------------------
  //サービス生成時に呼ばれる
  //------------------------------------------------------------------
  override fun onCreate() {
    super.onCreate()
    val dbHelper = DBHelper(this)
    db = dbHelper.writableDatabase
  }
  fun hello()
  {

  }
  //------------------------------------------------------------------
  //サービス開始時に呼ばれる
  //------------------------------------------------------------------
  override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int
  {
    super.onStartCommand(intent, flags, startId)
    val ng = Notification()
    startForeground(startId, ng)
    //サービスの開始
    val thread = object : Thread()
    {
      override fun run()
      {
        running = true
        while (running)
        {
          try
          {
            val cl = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"),Locale.JAPAN)
            nowHour = cl.get(Calendar.HOUR_OF_DAY)
            nowSecond = cl.get(Calendar.SECOND)
            if(nowHour==24)
            {
              val mYear = cl.get(Calendar.YEAR)
              val mMonth = cl.get(Calendar.MONTH) + 1
              val mDate = cl.get(Calendar.DATE)
              val mDOW = cl.get(Calendar.DAY_OF_WEEK)
              mToday = mYear.toString() + mMonth + mDate + mDOW

              writeDB(DataCtrl().deviceName, mToday)
              DataCtrl().PostData(i, onTimes, mToday)
              i = 0
              onTimes = 0
              postCount += 1
            }
          }
          catch (e: Exception)
          {
            val error = e.toString()
          }
          dm = this@MyService.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
          handler.post()
          {
            if (onTimes == 0) ++onTimes
            if (dm.getDisplay(Display.DEFAULT_DISPLAY).state == Display.STATE_ON)
            {
              var hun = i / 60
              var amari = i % 60
              if (!dispOnWas)
              {
                if (i >= 60) toast(this@MyService, "回数:${onTimes}  時間:${hun}分${amari}秒")
                else toast(this@MyService, "回数：${onTimes}  時間:${i}秒")
              }

              // テキストの表示
              dispOnWas = true
              i += 1
              if (i >= 60)
              {
                hun = i / 60
                amari = i % 60
                MeaSurvice.ontimeTv.text = "${hun}分${amari}秒"
              }
              else
              {
                MeaSurvice.ontimeTv.text = "${i}秒"
              }
              MeaSurvice.ontimesTv.text = "本日の表示回数： ${onTimes}回"
            }
            // ディスプレイが消えたら
            if (dm.getDisplay(Display.DEFAULT_DISPLAY).state == Display.STATE_OFF&&dispOnWas)
            {
              // 最低でも１秒間付けてないとカウントしない
              ++onTimes
              dispOnWas = false
            }
          }
          try
          {
            Thread.sleep(1000)
          }
          catch (e: Exception)
          {
          }
        }
      }
    }
    thread.start()
    return Service.START_STICKY
  }

  //------------------------------------------------------------------
  //サービス停止時に呼ばれる
  //------------------------------------------------------------------
  override fun onDestroy()
  {
    running = false
    super.onDestroy()
  }

  //------------------------------------------------------------------
  //サービス接続時に呼ばれる
  //------------------------------------------------------------------
  override fun onBind(intent: Intent): IBinder?
  {
    return IMyServiceBinder
  }

  //============================================================================
  //データベースヘルパーの定義
  //============================================================================
  private class DBHelper//データベースヘルパーのコンストラクタ
  (context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION)
  {
    override fun onCreate(db: SQLiteDatabase)
    {
      db.execSQL("create table if not exists $DB_TABLE("+BaseColumns._ID+" integer primary key, device text, ontime text, ontimes text, date text)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int)
    {
      if( oldVersion == 1 && newVersion == 2 )
      {
        db.execSQL("CREATE TABLE " + DB_TABLE + " ("
            + BaseColumns._ID + " INTEGER PRIMARY KEY,"
            + "device TEXT"
            + "ontime TEXT"
            + "ontimes TEXT"
            + "date TEXT"
            + ");" );
      }
      onCreate(db)
    }
  }

  //------------------------------------------------------------------------
  //データベースへの書き込み
  //------------------------------------------------------------------------
  @Throws(Exception::class)
  fun writeDB(deviceName: String, today: String)
  {
    val values = ContentValues()
    values.put("id", "0")
    values.put("device", deviceName)
    values.put("ontime", MyService.i.toString())
    values.put("ontimes", MyService.onTimes.toString())
    values.put("date", today)
    val colNum = db.update(DB_TABLE, values, null, null)
    if (colNum == 0) db.insert(DB_TABLE, "", values)
  }

  companion object
  {
    internal var onTimes = 0
    var i = 0
    lateinit var  mToday: String
    private val DB_NAME = "deviceinf.db"//DB名
    private val DB_TABLE = "deviceinf"   //テーブル名
    private val DB_VERSION = 1      //バージョン
    //------------------------------------------------------------------
    //ノティフィケーションの表示
    //------------------------------------------------------------------
    private fun showNotification(context: Context, iconId: Int, ticker: String, title: String, text: String, info: String)
    {
      //ノティフィケーションオブジェクトの生成
      val builder = Notification.Builder(context)
      builder.setWhen(System.currentTimeMillis())
      builder.setTicker(ticker)
      builder.setContentTitle(title)
      builder.setContentText(text)
      builder.setContentInfo(info)
      builder.setSmallIcon(iconId)
      val intent = Intent(context, MeaSurvice::class.java)
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      builder.setContentIntent(PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT))

      //ノティフィケーションマネージャの取得
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
      //ノティフィケーションのキャンセル
      nm.cancel(0)
      //ノティフィケーションの表示
      nm.notify(0, builder.notification)

    }

    // ---------------------------------------------------------------------------------------------------
    //トーストの表示
    // ---------------------------------------------------------------------------------------------------
    private fun toast(context: Context, text: String)
    {
      Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    //---------------------------------------------------------
    //HTTP通信
    //---------------------------------------------------------
    @Throws(Exception::class)
    fun http(path: String)
    {
      val w = ByteArray(1024)
      var c: HttpURLConnection? = null
      var `in`: InputStream? = null
      var out: ByteArrayOutputStream? = null
      try
      {
        //HTTP接続のオープン
        val url = URL(path)
        c = url.openConnection() as HttpURLConnection
        c.requestMethod = "GET"
        c.connect()
        `in` = c.inputStream

        //バイト配列の読み込み
        out = ByteArrayOutputStream()
        while (true)
        {
          val size = `in`!!.read(w)
          if (size <= 0) break
          out.write(w, 0, size)
        }
        out.close()

        //HTTP接続のクローズ
        `in`.close()
        c.disconnect()
      }
      catch (e: Exception)
      {
        try
        {
          if (c != null) c.disconnect()
          if (`in` != null) `in`.close()
          if (out != null) out.close()
        }
        catch (e2: Exception)
        {
        }
        throw e
      }
    }

    //-------------------------------------------------------------------------
    // 計測した情報を送信
    //-------------------------------------------------------------------------
    internal fun PostData(dailyOnTime: Int, dailyOnTimes: Int, today: String) {
      var con: HttpURLConnection? = null
      val ontime = dailyOnTime.toString()
      val ontimes = dailyOnTimes.toString()

      // 送信するパラメーターの整形
      val PostParameter = "&dailyontime=$ontime&dailyontimes=$ontimes&today=$today"
      try
      {
        val url = URL("")
        con = url.openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.doOutput = true
        con.setFixedLengthStreamingMode(PostParameter.toByteArray().size)
        con.useCaches = false
        val os = con.outputStream
        os.write(PostParameter.toByteArray())
        os.flush()
        os.close()
        con.connect()
      }
      catch (e: Exception)
      {
        val test = e.toString()
      }
      finally
      {
        if (con != null)
        {
          con.disconnect()
        }
      }
    }
  }
}
