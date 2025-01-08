package com.lepu.sleep

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SleepViewModel : ViewModel() {

    private val _sleepData: MutableLiveData<SleepData> = MutableLiveData()
    val mSleepData: LiveData<SleepData>
        get() = _sleepData

    fun getSleepData(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // 完整原始数据
                context.assets.open("spo2.dat").use { inputStream ->
                    _sleepData.postValue(SleepUtil.sleepAnalyze(inputStream.readBytes()))
                }
            }
        }
    }
}


