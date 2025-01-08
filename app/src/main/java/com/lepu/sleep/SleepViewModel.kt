@file:OptIn(ExperimentalTime::class)

package com.lepu.ble

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.AndroidAdvertisement
import com.juul.kable.AndroidPeripheral
import com.juul.kable.Bluetooth
import com.juul.kable.Bluetooth.Availability.Available
import com.juul.kable.Bluetooth.Availability.Unavailable
import com.juul.kable.State
import com.juul.kable.peripheral
import com.lepu.ble.device.BiolandBGMDevice
import com.lepu.ble.utils.LogUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.ExperimentalTime

private val reconnectDelay = 1.seconds

sealed class ViewState {

    data object BluetoothUnavailable : ViewState()

    data object Connecting : ViewState()

    data class Connected(val txt: String) : ViewState()

    data object Disconnecting : ViewState()

    data object Disconnected : ViewState()
}


val ViewState.label: String
    get() = when (this) {
        ViewState.BluetoothUnavailable -> "蓝牙不可用"
        ViewState.Connecting -> "连接中"
        is ViewState.Connected -> "已连接 $txt"
        ViewState.Disconnecting -> "断开连接"
        ViewState.Disconnected -> "连接断开"
    }

val peripheralScope = CoroutineScope(Job())

class SensorViewModel(application: Application, advertisement: AndroidAdvertisement) : AndroidViewModel(application) {

    private val autoConnect = MutableStateFlow(false)

    // 需要的中介范围 https://github.com/JuulLabs/kable/issues/577 已解决.
    private val scope =
        CoroutineScope(peripheralScope.coroutineContext + Job(peripheralScope.coroutineContext.job))

    private val peripheral = scope.peripheral(advertisement) { autoConnectIf(autoConnect::value) }
    private val biolandBGMDevice = BiolandBGMDevice(peripheral)
    private val state = combine(Bluetooth.availability, peripheral.state, ::Pair)

    init {
        viewModelScope.enableAutoReconnect()
    }

    private fun CoroutineScope.enableAutoReconnect() {
        state.filter { (bluetoothAvailability, connectionState) ->
            bluetoothAvailability == Available && connectionState is State.Disconnected
        }.onEach {
            ensureActive()
            LogUtil.e("Waiting $reconnectDelay to reconnect...")
            delay(reconnectDelay)
            connect()
        }.launchIn(this)
    }

    private fun CoroutineScope.connect() {
        launch {
            LogUtil.e("Connecting")
            try {
                peripheral.connect()
                (peripheral as AndroidPeripheral).requestMtu(247)
                autoConnect.value = true
            } catch (e: Exception) {
                autoConnect.value = false
                LogUtil.e("Connection attempt failed")
            }
        }
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    var viewState: Flow<ViewState> = state
        .flatMapLatest { (bluetoothAvailability, state) ->
            if (bluetoothAvailability is Unavailable) {
                return@flatMapLatest flowOf(ViewState.BluetoothUnavailable)
            }
            when (state) {
                is State.Connecting -> flowOf(ViewState.Connecting)
                State.Connected -> {
                    getData()
                    flowOf(ViewState.Connected(""))
                }

                State.Disconnecting -> flowOf(ViewState.Disconnecting)
                is State.Disconnected -> flowOf(ViewState.Disconnected)
            }
        }


    private fun getData() {
        viewModelScope.launch {
            getInfo().collectLatest { cmd ->
                LogUtil.e(cmd)
            }
        }
        viewModelScope.launch {
            biolandBGMDevice.notify.collectLatest { data ->
                LogUtil.e("notify: ${bytesToHexString(data)}")
                _wave.value = data
            }
        }
    }


    private val _wave = MutableStateFlow(byteArrayOf())

    val wave: Flow<ByteArray> = _wave


    private fun getInfo(): Flow<String> = flow {
        val byteArray = byteArrayOf(
            0x5A.toByte(),
            0x0A,
            0x00,
            0x18,
            0x09,
            0x17,
            0x0E,
            0x24,
            0x33,
            0x03
        )
        biolandBGMDevice.write(byteArray)
        emit("获取设备信息: ${bytesToHexString(byteArray)}")
    }

    override fun onCleared() {
        peripheralScope.launch {
            viewModelScope.coroutineContext.job.join()
            peripheral.disconnect()
            scope.cancel()
        }
    }
}


fun bytesToHexString(src: ByteArray): String {
    val stringBuilder = StringBuilder("")
    for (i in src.indices) {
        val v: Int = src[i].toInt() and 0xFF
        val hv = Integer.toHexString(v)
        if (hv.length < 2) {
            stringBuilder.append(0)
        }
        stringBuilder.append(hv)
        stringBuilder.append(", ")
    }
    return stringBuilder.toString()
}
