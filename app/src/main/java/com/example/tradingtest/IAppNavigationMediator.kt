package com.example.tradingtest

import android.content.Context

interface IAppNavigationMediator {
    fun navigateToSetting(context: Context)
    fun navigateToDashboard(context: Context)
}
