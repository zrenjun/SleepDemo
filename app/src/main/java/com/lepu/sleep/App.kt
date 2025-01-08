package com.lepu.sleep

import android.app.Application
import com.Carewell.ecg700.port.LogUtil


/**
 *
 *  说明:
 *  zrj 2025/1/8 11:22
 *
 */
class App : Application() {
    override fun onCreate() {
        super.onCreate()
        LogUtil.isSaveLog(applicationContext)
    }
}