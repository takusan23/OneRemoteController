package io.github.takusan23.oneremotecontroller

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.preference.PreferenceManager
import kotlinx.android.synthetic.main.activity_instance_access_token.*
import okhttp3.*
import java.io.IOException

class InstanceAccessTokenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_instance_access_token)

        title = "アカウント設定"

        //登録
        activity_login_button.setOnClickListener {
            val pref_setting = PreferenceManager.getDefaultSharedPreferences(this)
            val editor = pref_setting.edit()
            editor.putString("instance", activity_login_instance.text.toString())
            editor.putString("access_token", activity_login_access_token.text.toString())
            editor.apply()
            //正しいか検証する
            getAccount()
        }
    }

    fun getAccount() {
        val request = Request.Builder()
            .url("https://${activity_login_instance.text.toString()}/api/v1/accounts/verify_credentials?access_token=${activity_login_access_token.text.toString()}")
            .get()
            .build()
        val okHttpClient = OkHttpClient()
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                showToast("問題が発生しました。")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    showToast("ログインに成功しました。")
                    startActivity(Intent(this@InstanceAccessTokenActivity, MainActivity::class.java))
                } else {
                    showToast("インスタンス名、アクセストークンをもう一度確認してみてください。\n${response.code}")
                }
            }
        })
    }

    fun showToast(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

}
