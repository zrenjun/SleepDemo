package com.lepu.sleep

import android.content.Context
import android.text.format.DateFormat
import com.Carewell.ecg700.port.LogUtil
import com.lepu.utilpro.AlgorithmUtil
import com.lepu.utilpro.data.ble.Bluetooth
import com.lepu.utilpro.data.output.SleepResult
import org.joda.time.DateTime
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.abs


/**
 *
 *  说明: 睡眠分析工具
 *  zrj 2025/1/7 11:43
 *
 */
object SleepUtil {

    //o2 不包含2代（s）
    private val exSize = 40
    private val eachSize = 5 // 每采样点字节数

    fun sleepAnalyze(rawData: ByteArray): SleepData? {
        if (rawData.isEmpty()) {
            LogUtil.e("sleepCheckStatus, 没有血氧原始数据")
            return null
        } else {
            //普通o2
            val sleepBuf = rawData.takeLast(rawData.size - exSize).toByteArray()
            val oxyBleFile = OxyBleFile(rawData)
            // 采样间隔s
            val interval = oxyBleFile.recordingTime / oxyBleFile.data.size
            // 去截取20: 00 - 10:00的数据
            return getSleepData(
                oxyBleFile.startTime * 1000,
                oxyBleFile.recordingTime,
                sleepBuf,
                interval
            )
        }
    }

    //截取
    private fun getSleepData(
        measureTime: Long,
        measureDuration: Int,
        sleepBuf: ByteArray,
        interval: Int
    ): SleepData? {
        //截取数据起始时间
        LogUtil.e("sleepBuf = ${sleepBuf.size}, measureTime = ${measureTime.toDateString()}, measureDuration =  $measureDuration")
        var startTime = measureTime
        var endTime = measureTime + measureDuration * 1000
        LogUtil.e("startTime = $startTime, endTime =  $endTime")
        //原始时间
        val dayStart = DateTime(startTime)
        val dayEnd = DateTime(endTime)
        //睡眠范围时间 晚8点到第二天10点

        val time0foStr = TimeUtils.getDateStrYmd(measureTime, TimeUtils.YMD_FORMAT, false)
        val time0f0 = TimeUtils.timeTimeMillis(time0foStr, TimeUtils.YMD_FORMAT)
        val time0f10 = time0f0 + 10 * 60 * 60 * 1000 // 当天10点
        val time0f20 = time0f0 + 20 * 60 * 60 * 1000  // 开始测量时间的当天晚8点
        val time0f10Plus = time0f0 + 34 * 60 * 60 * 1000 // 第二天10点

        //数据满足睡眠分析

        // 以下代码基于O2设备最长存储10小时
        if (dayStart.millis > time0f10 && dayEnd.millis < time0f20) {
            // 在10-20点之间测量完成的数据， 不生成数据
            LogUtil.e("sleepCheckStatus, 不满足，在10-20点之间测量完成的数据")
            return null
        } else if (dayStart.millis >= time0f20 && dayEnd.millis <= time0f10Plus) {
            // 测量在20-次日0开始， 次日10点之间结束, 就是原始数据的开始结束时间
        } else if (dayEnd.millis <= time0f10) {
            // 测量在0-10开始，10之前结束, 就是原始数据的开始结束时间
        } else {
            // 跨20点、跨次日10点的情况
            // 处理开始时间： 测量时间在10点-20点之间，截取从20点开始， 其他随自身
            if (dayStart.millis in (time0f10 + 1) until time0f20) {
                // 测量开始时间在10-20之间， 开始时间为20，结束时间为自身结束时间
                startTime = time0f20
            }
            // 处理10点后结束的数据，由于设备最对保存10小时数据，所以10点结束的数据不会跨天
            if (dayStart.millis <= time0f10 && dayEnd.millis > time0f10) {
                endTime = time0f10
            }
        }
        LogUtil.e("截取出来的符合需求规定的睡眠时间：startTime = ${startTime.toDateString()}, endTime =  ${endTime.toDateString()}")
        if ((endTime - startTime) < 3 * 60 * 60 * 1000) {
            // 如果截取时长不够3小时
            LogUtil.e("sleepCheckStatus, 时长不满足")
            return null
        } else {
            val startIndex = (startTime - measureTime) / 1000 / interval * eachSize
            val duration = (endTime - startTime) / 1000
            var endIndex = duration / interval * eachSize + startIndex
            if (endIndex > sleepBuf.size) {
                endIndex = sleepBuf.size.toLong()
            }
            LogUtil.e("sleepBuf size = ${sleepBuf.size}interval = $interval, startIndex = $startIndex, endIndex =$endIndex, duration = $duration")
            val data = sleepBuf.copyOfRange(startIndex.toInt(), endIndex.toInt())
            val prList: IntArray
            val spo2List: IntArray?
            val moveList: IntArray
            val dataList = mutableListOf<OxyBleFile.EachData>()
            val exSize = 5
            val len = data.size.div(exSize)
            for (i in 0 until len) {
                dataList.add(OxyBleFile.EachData(data.copyOfRange(i * exSize, (i + 1) * exSize)))
            }
            prList = dataList.map { it.pr }.toIntArray()
            moveList = dataList.map { it.vector }.toIntArray()
            spo2List = dataList.map { it.spo2 }.toIntArray()

            //type – 设备类型 0：腕带式 1：手指式
            val type = 0
            val motion = if (moveList.find { it > 0 } == null) 0 else 1
            val timeS = (startTime / 1000).toInt()
            AlgorithmUtil.sleepAlgInit(timeS)
            AlgorithmUtil.sleepAlgMain(prList, moveList, motion, interval, type)
            val sleepResult = AlgorithmUtil.sleepAlgGetResult()
            return createSleep(sleepResult, interval, spo2List, prList, startTime, data)
        }
    }

    private fun createSleep(
        sleepResult: SleepResult,
        interval: Int,
        spo2List: IntArray,
        prList: IntArray,
        startTime: Long,
        data: ByteArray
    ): SleepData? {
        if (sleepResult.sleepTime < 3 * 60 * 60) {
            LogUtil.e("sleepCheckStatus, 时长不满足")
            return null
        } else {
            val sleepData = sleepResConvertToSleepData(sleepResult, interval)
            sleepData.measureDuration = sleepData.sleepDuration + sleepData.awakeTime
            with(prList.map { it }.filter { !InvalidValue.PR.contains(it) }) {
                sleepData.averagePr = this.average().toInt()
                sleepData.minPr = this.minOrNull() ?: 0
                sleepData.maxPr = this.maxOrNull() ?: 0
            }
            with(spo2List.map { it }.filter { !InvalidValue.SPO2.contains(it) }) {
                sleepData.averageSpo2 = this.average().toInt()
                sleepData.lowestSpo2 = this.minOrNull() ?: 0
                sleepData.maxSpo2 = this.maxOrNull() ?: 0
            }

            val model = Bluetooth.MODEL_O2RING
            val sleepByte = splitSleepStartToSleepEnd(sleepData, startTime, interval, data)
            sleepByte?.let {
                // 再送血氧算法检查
                LogUtil.e("再送血氧算法 model = $model interval = $interval, sleepByte size = ${it.size}")
                val odiRes = AlgorithmUtil.calculateOdi(model, interval, it)
                LogUtil.e("odiRes = ${odiRes.odi4Index}")
                odiRes?.let { odi ->
                    sleepData.odi = odi.odi4Index
                    val startH = TimeUtils.getDateForHours(sleepData.startSleep)
                    val timeOf0Str = TimeUtils.getDateStrYmd(
                        sleepData.startSleep,
                        pattern = TimeUtils.YMD_FORMAT,
                        isGmt0Enable = false
                    )
                    val timeOf0 = TimeUtils.timeTimeMillis(timeOf0Str, TimeUtils.YMD_FORMAT)
                    // 睡眠评分：入睡评分+睡眠总时间评分+深睡时间评分
                    // 入睡评分
                    //i.当入睡时间早于00:00时，入睡评分为20分;
                    //ii.当入睡时间晚于00:00，入睡评分(1-(入睡时间距离00:00的分钟数/480))*20
                    val socre1 =
                        if (startH in 20..23) 20f else (1 - Math.toIntExact((sleepData.startSleep - timeOf0) / 1000) / 60 / 480f) * 20

                    // 睡眠总时间评分：(1- abs((睡眠总分钟数-480) /480)))*60
                    val socre2 =
                        (1 - abs((sleepData.sleepDuration / 60 - 480) / 480f)) * 60
                    // c.深睡评分：(1-abs(((深睡分钟数/总睡眠分钟数)100-25)/25))*20
                    val socre3 =
                        (1 - abs(((sleepData.deepTime / 60) / (sleepData.sleepDuration / 60).toFloat()) * 100 - 25) / 25f) * 20

                    //睡眠质量评分*50%+ODI评分*50%
                    val socre4 = when {
                        sleepData.odi < 5f -> 100
                        sleepData.odi in 5f..14.9f -> 60
                        sleepData.odi in 15f..29.9f -> 30
                        sleepData.odi >= 30 -> 10
                        else -> 0
                    }
                    sleepData.sleepScore =
                        ((socre1 + socre2 + socre3) * 0.5f + socre4 * 0.5f).toInt()
                    LogUtil.e("入睡评分 = $socre1, 睡眠总时间评分 = $socre2, 深睡评分 = $socre3, socre4 = $socre4")
                    LogUtil.e("sleepScore = ${sleepData.sleepScore}")

                }
            }
            LogUtil.e("sleepCheckStatus, 合格")
            return sleepData
        }
    }

    /**
     * 满足睡眠总长3小时，才合格
     * 将睡眠算法结果封装到SleepData
     */
    private fun sleepResConvertToSleepData(sleepResult: SleepResult, interval: Int): SleepData {
        val fallingAsleep = sleepResult.fallingAsleep //入睡时间点 (时间戳)s
        val awakeTime = sleepResult.awakeTime  //出睡时间点(时间戳)s

        val items = mutableListOf<SleepItem>()
        var status = sleepResult.sleepState[0]
        var index = 0
        for (i in sleepResult.sleepState.indices) {
            val algStatus = sleepResult.sleepState[i] // 睡眠状态
            if (status != algStatus || i == sleepResult.sleepState.size - 1) {
                if (algStatus == 4 || algStatus == 5) { //4 准备睡眠阶段  5 未得出结果
                    continue
                }
                items.add(
                    SleepItem(
                        (fallingAsleep + index * interval) * 1000L,
                        (i - index) * interval,
                        status
                    )//算法间隔4秒
                )
                status = algStatus
                index = i
            }
        }
        val sleepTime = sleepResult.sleepTime
        val sleepData = SleepData(
            sleepDuration = sleepTime, //总睡眠时间(单位: s)
            deepTime = sleepResult.deepTime, //深睡时间 (单位: s)
            lightTime = sleepResult.lightTime, //浅睡时间(单位:s)
            remTime = sleepResult.remTime, //快速眼动时间(单位: s)
            wakingTimes = sleepResult.awakeCnt, //清醒次数
            awakeTime = awakeTime - fallingAsleep - sleepTime, // 清醒时长(s): 睡眠周期-睡眠总时长
            startSleep = fallingAsleep * 1000L, //入睡时间点 (时间戳)
            endSleep = awakeTime * 1000L, //出睡时间点(时间戳)
            sleepItems = items
        )
        LogUtil.e(
            "总睡眠时间(单位: s)：${sleepData.sleepDuration} " +
                    "深睡时间 (单位: s)：${sleepData.deepTime} " +
                    "浅睡时间(单位:s)：${sleepData.lightTime} " +
                    "快速眼动时间(单位: s)：${sleepData.remTime} " +
                    "入睡时间点 (时间戳)：${sleepData.startSleep} " +
                    "出睡时间点(时间戳)：${sleepData.endSleep} " +
                    "清醒次数：${sleepData.wakingTimes} " +
                    "清醒时长：${sleepData.awakeTime} " +
                    "睡眠评分：${sleepData.sleepScore} " +
                    "item size：${items.size} "
        )
        return sleepData
    }


    /**
     * 截取从入睡到出睡的数组
     */
    private fun splitSleepStartToSleepEnd(
        sleepData: SleepData,
        startTime: Long,
        interval: Int,
        splitData: ByteArray
    ): ByteArray? {
        val startIndex = ((sleepData.startSleep - startTime) / 1000).toInt() / interval * eachSize
        val duration = ((sleepData.endSleep - sleepData.startSleep) / 1000).toInt()
        val endIndex = startIndex + duration / interval * eachSize
        LogUtil.e("startTime = $startTime, sleepSize：${splitData.size}")
        LogUtil.e("入睡时间：${sleepData.startSleep}, 出睡时间：${sleepData.endSleep}, duration = $duration, interval = $interval")
        LogUtil.e("startIndex = $startIndex, endIndex = $endIndex")
        return try {
            return splitData.copyOfRange(startIndex, endIndex)
        } catch (e: Exception) {
            LogUtil.e("截取入睡时间到出睡时间数据数值异常")
            e.printStackTrace()
            null
        }
    }
}

interface InvalidValue {
    companion object {
        val SPO2 = intArrayOf(-1, 0, 127, 255)
        val PR = intArrayOf(0, 255, 511, 65535)
    }
}


fun Long.toDateString(format: String = "HH:mm:ss dd/MM/yyyy", context: Context? = null): String {
    var pattern = format
    context?.let {
        pattern = format.is24HourFormat(context)
    }
    return SimpleDateFormat(pattern, TimeUtils.getLocale()).format(Date(this))
}


fun String.is24HourFormat(context: Context): String {
    if (DateFormat.is24HourFormat(context)) {
        return this
    }
    if (!this.contains("HH")) return this
    return this.replace("HH", "hh") + " aa"
}
