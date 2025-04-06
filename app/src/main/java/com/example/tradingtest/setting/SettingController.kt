package com.example.tradingtest.setting

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.example.tradingtest.IAppNavigationMediator
import kotlinx.parcelize.Parcelize

@Parcelize
class SettingController  : Parcelable {
    private lateinit var navigationMediator: IAppNavigationMediator

    fun startUI(context: Context) {
        val intent = Intent(context, getActivityClass())
        context.startActivity(intent)
    }

    fun setNavMediator(appMediator: IAppNavigationMediator){
        navigationMediator = appMediator
    }

    private fun getActivityClass(): Class<*> = SettingActivity::class.java

    fun goToDashboard(context: Context) {
//        Log.d("TAG", "goToRegister: ")
        navigationMediator.navigateToDashboard(context) // Uses mediator to instantiate RegisterController
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: SettingController? = null

        fun getInstance(): SettingController {
            return instance ?: synchronized(this) {
                instance ?: SettingController().also { instance = it }
            }
        }
    }
}
