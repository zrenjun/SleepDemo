package com.lepu.sleep


/**
 *
 *  说明:
 *  zrj 2025/1/7 11:39
 *
 */
data class SleepData(
    var measureDuration: Int = 0,  // 测量时长（s）
    var startSleep: Long = 0, // 入睡时间点  当作睡眠文件名称
    var endSleep: Long = 0, // 出睡时间点
    var wakingTimes: Int = 0, // 清醒次数
    var awakeTime: Int = 0, // 清醒时长
    var remTime: Int = 0, // 快速眼动时长
    var deepTime: Int = 0, // 深睡时长
    var lightTime: Int = 0, // 浅睡时长
    var sleepDuration: Int = 0, // 睡眠时长（s）
    var averageSpo2: Int = 0, //平均血氧
    var lowestSpo2: Int = 0, // 最低血氧
    var maxSpo2: Int = 0, // 最大血氧
    var averagePr: Int = 0, //平均脉率
    var minPr: Int = 0, // 最低脉率
    var maxPr: Int = 0, // 最大脉率
    var sleepScore: Int = 0, // 睡眠评分
    var odi: Float = 0f, // 呼吸暂停风险指数
    var sleepItems: List<SleepItem>,//SleepDetails
)

data class SleepItem(
    var timeStamp: Long, //时间戳
    var duration: Int, // 时长
    var status: Int //睡眠状态  0 深睡眠 1 浅睡眠 2 快速眼动 3 清醒
)