package com.lepu.sleep

import android.annotation.SuppressLint
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


object TimeUtils {

    const val YMD_FORMAT = "yyyy-MM-dd"

    /**
     * 获取小时（默认24小时制）
     */
    fun getDateForHours(time: Long): Int {
        val date = Date(time)
        val cal = Calendar.getInstance()
        cal.time = date
        return cal.get(Calendar.HOUR_OF_DAY)
    }


    /**
     * 年月日
     */
    fun getDateStrYmd(
        time: Long,
        pattern: String = YMD_FORMAT,
        isGmt0Enable: Boolean = true
    ): String {
        val format = SimpleDateFormat(pattern, getLocale())
        if (isGmt0Enable) {
            format.timeZone = TimeZone.getTimeZone("GMT+0")
        }
        return format.format(Date(time))
    }


    /**
     * 国家码
     */
    fun getLocale(): Locale {
        return Locale.getDefault()
    }


    /**
     * 日期格式字符串转换成时间戳
     * date 字符串日期（2020-05-02 15:30:00）
     * 结果：1588404600
     */
    @SuppressLint("SimpleDateFormat")
    fun timeTimeMillis(date: String, pattern: String): Long {
        val sdf = SimpleDateFormat(pattern)
        try {
            return sdf.parse(date)?.time ?: 0
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return 0
    }
}
