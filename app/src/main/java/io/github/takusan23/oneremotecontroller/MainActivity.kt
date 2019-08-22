package io.github.takusan23.oneremotecontroller

import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.widget.Toast
import androidx.core.net.toUri
import androidx.preference.PreferenceManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONObject
import java.io.IOException
import java.lang.Exception
import java.net.URI

class MainActivity : AppCompatActivity() {

    lateinit var pref_setting: SharedPreferences
    var instance = ""
    var access_token = ""
    lateinit var webSocketClient: WebSocketClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pref_setting = PreferenceManager.getDefaultSharedPreferences(this)

        instance = pref_setting.getString("instance", "") ?: ""
        access_token = pref_setting.getString("access_token", "") ?: ""

        //WebSocket接続
        if (instance.isNotEmpty()) {
            connectionStreamingAPI()
        }

        //設定、ライセンス画面
        account_activity_button.setOnClickListener {
            startActivity(Intent(this, InstanceAccessTokenActivity::class.java))
        }
        license_activity_button.setOnClickListener {
            startActivity(Intent(this, LicenceActivity::class.java))
        }

        //照明ON/OFF
        light_on_button.setOnClickListener {
            postStatus("照明ON")
        }
        light_off_button.setOnClickListener {
            postStatus("照明OFF")
        }
        light_night_button.setOnClickListener {
            postStatus("照明常夜灯")
        }//エアコンON/OFF
        air_on_button.setOnClickListener {
            postStatus("エアコンON")
        }
        air_off_button.setOnClickListener {
            postStatus("エアコンOFF")
        }
        //扇風機ON/OFF
        fan_on_button.setOnClickListener {
            postStatus("扇風機ON")
        }
        fan_off_button.setOnClickListener {
            postStatus("扇風機OFF")
        }

    }

    fun connectionStreamingAPI() {
        val address =
            "wss://${instance}/api/v1/streaming/?stream=direct&access_token=${access_token}"
        val uri = URI(address)
        webSocketClient = object : WebSocketClient(uri) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                showSnackbar("接続しました。😀")
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                showSnackbar("閉じました。😴")
            }

            override fun onMessage(message: String?) {
                val jsonObject = JSONObject(message)
                val payload = jsonObject.getString("payload")
                val toot_jsonObject = JSONObject(payload)
                var content = toot_jsonObject.getJSONObject("last_status").getString("content")
                //Pタグ邪魔。Html.fromHtmlは改行されるので
                content = content.replace("<p>", "")
                content = content.replace("</p>", "")
                runOnUiThread {
                    Snackbar.make(
                        account_activity_button,
                        content,
                        Snackbar.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onError(ex: Exception?) {
                showSnackbar("エラーです。😰")
            }
        }
        webSocketClient.connect()
    }

    fun postStatus(commandName: String) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("access_token", access_token)
            .addFormDataPart("status", commandName)
            .addFormDataPart("visibility", "direct")
            .build()
        val request = Request.Builder()
            .url("https://${instance}/api/v1/statuses")
            .post(requestBody)
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showSnackbar("問題が発生しました。")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    showSnackbar("送信しました。Raspberry Pi側の応答をお待ち下さい・・・")
                } else {
                    showSnackbar("問題が発生しました。\n${response.code}")
                }
            }
        })
    }

    fun showSnackbar(message: String) {
        runOnUiThread {
            Snackbar.make(account_activity_button, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        webSocketClient.close()
    }

}
