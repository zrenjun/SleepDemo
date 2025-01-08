package com.viatom.common.util.time

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import kotlin.math.abs


object TimeUtils {

    /**
     * HH：24小时制
     * hh：12小时制
     */

    private val YMDHMS_FORMAT = "yyyy-MM-dd HH:mm:ss"
    const val YMD_FORMAT = "yyyy-MM-dd"
    private val YMDHMS_UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    const val HMS_FORMAT = "HH:mm:ss"

    const val HM_FORMAT = "HH:mm"

    const val H_FORMAT = "HH"

    //国外12小时制【年月日时分秒】
    private val YMDHMS_FOREIGN = "yyyy/MM/dd aa hh:mm:ss"//"hh:mm:ss aa MMM dd, yyyy"

    //国内24h小时制【年月日时分秒】
    private val YMDHMS_DOMESTIC = "yyyy/MM/dd aa HH:mm:ss"

    //国外12小时制【年月日】
    private val YMD_FOREIGN = "MMM dd, yyyy"

    //国内24h小时制【年月日】
    private val YMD_DOMESTIC = "yyyy年MMMdd日"

    //国外12小时制【时分秒】
    private val HMS_FOREIGN = "hh:mm:ss aa"

    //国内24h小时制【时分秒】
    private val HMS_DOMESTIC = "HH:mm:ss"

    /**
     * F5体脂称开始
     */
    //国外12小时制【年月日时分秒】
    private val F5_FOREIGN = "HH:mm:ss  yyyy/MM/dd"


    private val PDF_DATE = "yyyyMMddHHmmss"

    /**
     * 将字符串转换成date
     */
    fun parseTime(time: String, formatStr: String): Date? {
        time?.let {
            val sdf = SimpleDateFormat(formatStr)
            try {
                return sdf.parse(time)
            } catch (e: java.lang.Exception) {
            }
        }
        return null
    }


    /**
     * 根据Date时间生成UTC时间函数  yyyy-MM-dd'T'HH:mm:ss.SS'Z'
     * @param time
     * @return
     */
    fun getUTCDate(time: String): String? {
        parseTime(time, YMDHMS_FORMAT)?.let {
            val formatZ = SimpleDateFormat(YMDHMS_UTC_FORMAT)
            return formatZ.format(it)
        } ?: return null
    }


    /**
     * HTC 转  年月日
     * yyyy-MM-dd'T'HH:mm:ss'Z'  >>> yyyy-MM-dd
     * 2011-01-01 00:00:00  >>> yyyy-MM-dd
     */
    fun getYmdDate(time: String): String? {

        if (time.isEmpty()) return ""
        var str = YMDHMS_FORMAT
        if (time.contains("T")) {
            str = YMDHMS_UTC_FORMAT
        }
        val parseTime = parseTime(time, str)
        val formatZ = SimpleDateFormat(YMD_FORMAT)
        return formatZ.format(parseTime)
    }


    /**
     * 年月日时分秒
     * 国内国外时间和UI显示不一致
     * 国内：2021年10月18 上午09:31:47
     * 国外：09:31:47 AM Oct 18,2021
     * HH：24小时制
     * hh：12小时制
     */
    fun getDateStrYmdHms(time: Long, isGmt0Enable: Boolean = true): String {
        var pattern = YMDHMS_FOREIGN//默认国外12小时制
        if (getLocale().language == "zh") {
            pattern = YMDHMS_DOMESTIC//国内24h制
        }
        val format = SimpleDateFormat(pattern, getLocale())
        if (isGmt0Enable) {
            format.timeZone = TimeZone.getTimeZone("GMT+0")
        }
        return format.format(Date(time))
    }


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
    fun getDateStrYmd(time: Long, isGmt0Enable: Boolean = true): String {
        var pattern = YMD_FOREIGN//默认国外12小时制
        if (getLocale().language == "zh") {
            pattern = YMD_DOMESTIC//国内24h制
        }
        val format = SimpleDateFormat(pattern, getLocale())
        if (isGmt0Enable) {
            format.timeZone = TimeZone.getTimeZone("GMT+0")
        }
        return format.format(Date(time))
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
     * 时分秒
     */
    fun getDateStrHms(time: Long, isGmt0Enable: Boolean = true): String {
        var pattern = HMS_FOREIGN//默认国外12小时制
        if (getLocale().language == "zh") {
            pattern = HMS_DOMESTIC//国内24h制
        }
        val format = SimpleDateFormat(pattern, getLocale())
        if (isGmt0Enable) {
            format.timeZone = TimeZone.getTimeZone("GMT+0")
        }
        return format.format(Date(time))
    }

    /**
     * 时分秒
     */
    fun getDateStrHms(
        time: Long,
        pattern: String = HMS_FORMAT,
        isGmt0Enable: Boolean = false
    ): String {
        val format = SimpleDateFormat(pattern, getLocale())
        if (isGmt0Enable) {
            format.timeZone = TimeZone.getTimeZone("GMT+0")
        }
        return format.format(Date(time))
    }


    fun getDateStr(date: Date, pattern: String, locale: Locale = Locale.getDefault()) =
        SimpleDateFormat(pattern, locale).format(date)


    /**
     * 测量时间 不带上下午以及年份
     */
    fun getBFMeasuringTime(time: Long, isGmt0Enable: Boolean = true): String {
        val pattern = YMDHMS_FORMAT//默认国外12小时制
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
        return try {
            if (!AppCompatDelegate.getApplicationLocales().isEmpty) {
                AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
            } else {
                val langList =
                    listOf("en", "cs", "de", "es", "fr", "hu", "it", "ja", "nl", "pt", "ro", "zh")
                if (langList.contains(Locale.getDefault().language)) {
                    Locale.getDefault()
                } else {
                    Locale.ENGLISH
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Locale.ENGLISH
        }
    }


    /**
     * 日期格式字符串转换成时间戳
     * date 字符串日期（2020-05-02 15:30:00）
     * 结果：1588404600
     */
    fun timeTimeMillis(date: String): Long {
        val sdf = SimpleDateFormat(YMDHMS_FORMAT)
        try {
            return sdf.parse(date).time
        } catch (e: ParseException) {
        }
        return 0
    }

    /**
     * 日期格式字符串转换成时间戳
     * date 字符串日期（2020-05-02 15:30:00）
     * 结果：1588404600
     */
    fun timeTimeMillis(date: String, pattern: String): Long {
        val sdf = SimpleDateFormat(pattern)
        try {
            return sdf.parse(date).time
        } catch (e: ParseException) {
        }
        return 0
    }


    /**
     * F5体脂称年月日时分秒显示
     */
    fun getF5BodyFatTime(time: Long, isGmt0Enable: Boolean = false): String {
        var pattern = F5_FOREIGN//默认国外12小时制
        if (getLocale().language == "zh") {
            pattern = YMDHMS_DOMESTIC//国内24h制
        }
        val format = SimpleDateFormat(pattern, getLocale())
        if (isGmt0Enable) {
            format.timeZone = TimeZone.getTimeZone("GMT+0")
        }
        return format.format(Date(time))
    }

    fun getF8PdfTime(time: Long, isGmt0Enable: Boolean = false): String {
        val format = SimpleDateFormat(YMDHMS_FORMAT, getLocale())
        if (isGmt0Enable) {
            format.timeZone = TimeZone.getTimeZone("GMT+0")
        }
        return format.format(Date(time))
    }


    /**
     * 时间戳 >>> 转年月日
     */
    fun getPdfShareTime(isGmt0Enable: Boolean = false): String {
        val pattern = PDF_DATE//默认国外12小时制
        val format = SimpleDateFormat(pattern, getLocale())
        if (isGmt0Enable) {
            format.timeZone = TimeZone.getTimeZone("GMT+0")
        }
        return format.format(Date(System.currentTimeMillis()))
    }

    /**
     * 时间戳 >>> 转年月日
     */
    fun getPdfShareTime(time: Long, isGmt0Enable: Boolean = false): String {
        val pattern = PDF_DATE//默认国外12小时制
        val format = SimpleDateFormat(pattern, getLocale())
        if (isGmt0Enable) {
            format.timeZone = TimeZone.getTimeZone("GMT+0")
        }
        return format.format(Date(time))
    }

    fun getRecordTime(recordingTime: Int, sep: String, ignoreHour: Boolean): String {
        if (recordingTime < 0) {
            return "00" + sep + "00" + sep + "00"
        }
        val recordHour = recordingTime / 3600
        val recordMinute = recordingTime % 3600 / 60
        val recordSecond = recordingTime % 3600 % 60
        var recordHourStr = recordHour.toString()
        if (recordHour < 10) {
            recordHourStr = "0$recordHourStr"
        }
        var recordMinuteStr = recordMinute.toString()
        if (recordMinute < 10) {
            recordMinuteStr = "0$recordMinuteStr"
        }
        var recordSecondStr = recordSecond.toString()
        if (recordSecond < 10) {
            recordSecondStr = "0$recordSecondStr"
        }
        var recordTime = ""
        if (recordHour > 0 || !ignoreHour) {
            recordTime = recordTime + recordHourStr + sep
        }
        recordTime = recordTime + recordMinuteStr + sep
        recordTime += recordSecondStr
        return recordTime
    }


    fun getYearMonthDay(millis: Long): String? {
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(Date(millis))
    }

    fun getTimeHourMinSecond(millis: Long): String? {
        val dateFormat: DateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        return dateFormat.format(Date(millis))
    }


    /**
     * 根据时间字符串和格式转换成long类型
     *
     * @param dateStr String
     * @param format String
     * @return Long?
     */
    @JvmStatic
    fun string2Time(dateStr: String, format: String): Long? {
        try {
            return SimpleDateFormat(format, Locale.US).parse(dateStr)?.time
        } catch (e: Exception) {

        }
        return null
    }

    fun getTimeToSpecificTime(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int = 0
    ): Date {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.YEAR, year)
        calendar.set(Calendar.MONTH, month - 1)
        calendar.set(Calendar.DAY_OF_MONTH, day)
        calendar.set(Calendar.HOUR_OF_DAY, hour)
        calendar.set(Calendar.MINUTE, minute)
        calendar.set(Calendar.SECOND, second)
        return calendar.time
    }

    /**
     * 返回两个时间相隔的天数
     * @param one 以毫秒计时间1
     * @param another 以毫秒计时间2
     * @return
     */
    fun getDaysBetween(one: Long, another: Long): Long {
        return abs(one - another) / (1000 * 60 * 60 * 24)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun convertUtcToLocal(utcString: String): String {
        // 解析UTC时间字符串
        val utcDateTime = LocalDateTime.parse(utcString, DateTimeFormatter.ISO_DATE_TIME)

        // 将LocalDateTime转换为ZonedDateTime，使用UTC时区
        val zonedUtcDateTime = utcDateTime.atZone(ZoneOffset.UTC)

        // 获取设备当前的时区
        val currentZoneId = ZoneId.systemDefault()

        // 转换为本地时区的时间
        val localDateTime = zonedUtcDateTime.withZoneSameInstant(currentZoneId)

        // 格式化输出（如果需要）
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return localDateTime.format(formatter)

    }

    fun getTimeForZone0(date: String): Long{
        val pattern = "yyyy-MM-dd HH:mm:ss"
        val sdf = SimpleDateFormat(pattern)
        sdf.timeZone = TimeZone.getTimeZone("GMT")

        val zeroTime = sdf.format(date)
        val zeroDate = sdf.parse(zeroTime)

        return zeroDate.time
    }

}
