package com.example.yasu0000.opetimeasure

import android.app.Activity
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.Window
import android.webkit.WebView
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Created by yasu on 2016/10/31.
 */

//HTTP通信
class MeaSurvice : Activity(){

  private enum class BUTAG
  {
    TAG_CHNG,TAG_START,TAG_STOP,TAG_WRITE
  }

  private lateinit var  serviceIntent: Intent
  private var binder: IMyService? = null
  private val handler = Handler()
  private  val isLogin:Boolean = true

  // レイアウト達
  internal lateinit var layout: LinearLayout


  companion object
  {
    private val WC = LinearLayout.LayoutParams.WRAP_CONTENT
    private var strtbtn: Button? = null
    private var stopbtn: Button? = null
    public var writeDBbtn: Button? =null
    //テキストファイルのURLの指定
    //--------------------------------------------------------
    // サービス操作の指定
    //--------------------------------------------------------
    fun setServiceUI(isStop: Boolean)
    {
      strtbtn!!.isEnabled = isStop
      stopbtn!!.isEnabled = !isStop
    }
    public lateinit var ontimeTv:TextView
    public lateinit var ontimesTv:TextView
  }
  //アクティビティ起動時に呼ばれる
  public override fun onCreate(bundle: Bundle?)
  {
    super.onCreate(bundle)
    requestWindowFeature(Window.FEATURE_NO_TITLE)
    // ボタンの作成
    stopbtn = makeButton("リセット",BUTAG.TAG_STOP)
    strtbtn = makeButton("計測開始",BUTAG.TAG_START)
    writeDBbtn = makeButton("DB登録",BUTAG.TAG_WRITE)
    serviceIntent = Intent(this,MyService::class.java)
    // レイアウトの生成
    layout = LinearLayout(this)
    layout.orientation
    setContentView(layout)
    layout.addView(strtbtn)
    layout.addView(stopbtn)


    ontimeTv = TextView(this)
    ontimesTv = TextView(this)
    ontimeTv.layoutParams= LinearLayout.LayoutParams(WC,WC)
    ontimesTv.layoutParams= LinearLayout.LayoutParams(WC,WC)
    ontimeTv.setText("")
    ontimesTv.setText("")
    layout.addView(ontimeTv)
    layout.addView(ontimesTv)


    // Loginかどうか
    if (isLogin) layout.addView(makeButton("一時停止", BUTAG.TAG_CHNG))
    else layout.addView(makeButton("一時停止", BUTAG.TAG_CHNG)) // 登録画面の表示
  }
  //----------------------------------------------------------------
  // サービス起動中かどうか判定
  //----------------------------------------------------------------
  private fun isServiceRunning(): Boolean
  {
    val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val serviceInfos = am.getRunningServices(Integer.MAX_VALUE)
    return serviceInfos.indices.any { serviceInfos[it].service.className == "com.example.yasu0000.opetimeasure.MyService" }
    // アクティブなサービスのなかで、このプロジェクト名があったら。
  }
  //--------------------------------------------------------
  //ボタンの生成メソッド
  //--------------------------------------------------------
  private fun makeButton(text: String, tag: Any): Button
  {
    val button = Button(this)
    button.setText(text)
    // イベントの設定
    when(tag)
    {
      BUTAG.TAG_CHNG  ->button.setOnClickListener { if(isLogin)logout()else login() }
      BUTAG.TAG_START ->button.setOnClickListener { startService() }  // サービスの開始
      BUTAG.TAG_STOP  ->button.setOnClickListener { stopService() }   // サービスの停止
      BUTAG.TAG_WRITE -> button.setOnClickListener{DataCtrl().writeDB()}
      else            -> ""
    }
    button.setTag(tag)
    button.layoutParams = LinearLayout.LayoutParams(WC, WC)
    return button
  }

  //---------------------------------------------------------------------------
  // ログアウト
  //---------------------------------------------------------------------------
  private fun logout()
  {
    if (isServiceRunning())
    {
      setServiceUI(true)
      unbindService(connection)
      stopService(serviceIntent)

    }
  }
  //----------------------------------------------------------------------
  // ログイン
  //----------------------------------------------------------------------
  fun login()
  {
    // webビューを起動する
    var web: WebView = WebView(this)
    web.loadUrl("")
    setContentView(web)
    web.settings.javaScriptEnabled
    web.settings.supportZoom()
  }
  //----------------------------------------------------------------------
  //サービスの開始
  //----------------------------------------------------------------------
  fun startService()
  {
    setServiceUI(false)
    startService(serviceIntent)
    bindService(serviceIntent, connection, BIND_AUTO_CREATE)

  }
  //----------------------------------------------------------------------
  // サービスの停止
  //----------------------------------------------------------------------
  fun stopService()
  {
    setServiceUI(true)
    unbindService(connection)
    stopService(serviceIntent)

    // 計測結果を0に
    ontimeTv.setText("0")
    ontimesTv.setText("0")
    MyService.i =0
    MyService.onTimes=0
  }
  //--------------------------------------------------------
  // サービスとの接続
  //--------------------------------------------------------
  private val connection = object : ServiceConnection
  {
    override fun onServiceConnected(name: ComponentName, service: IBinder) { binder = IMyService.Stub.asInterface(service) }
    override fun onServiceDisconnected(name: ComponentName) { binder = null }
  }
}