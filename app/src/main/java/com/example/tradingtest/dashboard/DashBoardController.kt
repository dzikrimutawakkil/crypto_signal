package com.example.tradingtest.dashboard

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Parcelable
import com.example.tradingtest.IAppNavigationMediator
import kotlinx.parcelize.Parcelize

@Parcelize
class DashBoardController  : Parcelable {
    private lateinit var navigationMediator: IAppNavigationMediator

    fun startUI(context: Context) {
        val intent = Intent(context, getActivityClass())
        context.startActivity(intent)
    }

    fun setNavMediator(appMediator: IAppNavigationMediator){
        navigationMediator = appMediator
    }

    private fun getActivityClass(): Class<*> = DashboardActivity::class.java

    fun goToSetting(context: Context) {
        navigationMediator.navigateToSetting(context) // Uses mediator to instantiate RegisterController
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var instance: DashBoardController? = null

        fun getInstance(): DashBoardController {
            return instance ?: synchronized(this) {
                instance ?: DashBoardController().also { instance = it }
            }
        }
    }
}