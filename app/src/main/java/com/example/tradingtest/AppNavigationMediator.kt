package com.example.tradingtest

import android.content.Context
import com.example.tradingtest.dashboard.DashBoardController
import com.example.tradingtest.setting.SettingController


class AppNavigationMediator : IAppNavigationMediator {
    override fun navigateToSetting(context: Context) {
        val controller = SettingController.getInstance()
        controller.setNavMediator(this)
        controller.startUI(context)
    }

    override fun navigateToDashboard(context: Context) {
        val controller = DashBoardController.getInstance()
        controller.setNavMediator(this)
        controller.startUI(context)
    }
}
