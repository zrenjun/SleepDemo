package com.lepu.sleep

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.Carewell.ecg700.port.LogUtil

class MainActivity : AppCompatActivity() {
    private val viewModel: SleepViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        viewModel.getSleepData(this)
        viewModel.mSleepData.observe(this) {
            LogUtil.e(it.toString())
        }
    }
}


