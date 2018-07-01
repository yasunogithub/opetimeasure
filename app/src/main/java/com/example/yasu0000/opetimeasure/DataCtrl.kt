package com.example.yasu0000.opetimeasure

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Build

import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Created by yasu0000 on 2016/11/23.
 *
 */

class  DataCtrl {
    // DB
    val DB_NAME = "deviceinf.db"//DB名
    val DB_TABLE = "deviceinf"   //テーブル名
    private val DB_VERSION = 1      //バージョン
    private  lateinit var db: SQLiteDatabase      //データベースオブジェクト
    val u1 = UUID.randomUUID()
    internal val deviceName = Build.BRAND + Build.DEVICE + "%=" + u1
    internal var context: Context? = null
    private lateinit var dbHelper:DBHelper
    //データベースへの書き込み
    @Throws(Exception::class)
    fun writeDB(deviceInfo: String, today: String)
    {
        dbHelper = DBHelper(context)
        db = dbHelper.writableDatabase
        //データベースオブジェクトの取得
        val values = ContentValues()
        values.put("device", deviceInfo)
        values.put("ontime", MyService.i.toString())
        values.put("ontimes", MyService.onTimes.toString())
        values.put("date", today)
        val colNum = db.update(DB_TABLE, values, null, null)
        if (colNum == 0) db.insert(DB_TABLE, "", values)
    }


    // POST
    internal fun PostData(dailyOnTime: Int, dailyOnTimes: Int, today: String) {
        var con: HttpURLConnection? = null

        val ontime = dailyOnTime.toString()
        val ontimes = dailyOnTimes.toString()
        val PostParameter = "devicename=$deviceName&dailyontime=$ontime&dailyontimes=$ontimes&today=$today"
        try {
            val url = URL("https://yasumelt.net/dronDispChart/_post.cgi")
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
        } catch (e: Exception) {
            val test = e.toString()
        } finally {
            if (con != null) {
                con.disconnect()
            }
        }
    }


    //データベースヘルパーの定義
    private class DBHelper//データベースヘルパーのコンストラクタ
    (context: Context?) : SQLiteOpenHelper(context, DataCtrl().DB_NAME, null, DataCtrl().DB_VERSION) {

        //データベースの生成
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("create table if not exists " +
                    DataCtrl().DB_TABLE+ "(id text primary key, device text, ontime text, ontimes text, date text)")


        }
        //データベースのアップグレード
        override fun onUpgrade(db: SQLiteDatabase,
                               oldVersion: Int, newVersion: Int) {
            //db.execSQL("drop exists if exists hello")
            onCreate(db)
        }
    }
}