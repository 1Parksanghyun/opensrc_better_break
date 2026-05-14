@file:Suppress("UNUSED")

package com.example.opensrc_better_break

/**
 * 실외 전용 센서(온도, 습도) 데이터를 바탕으로 야외 휴식처의 쾌적도를 분석.
 */
object CongestionAnalyzer {

    // 화면에 띄울 상태 라벨
    enum class Status(val label: String, val colorCode: String) {
        GOOD("쾌적", "#4CAF50"),
        NORMAL("보통", "#FFEB3B"),
        BAD("불쾌", "#F44336")
    }

    /**
     * @param temp DB에서 가져온 야외 온도 (C)
     * @param humidity DB에서 가져온 야외 습도 (%)
     */
    fun analyze(temp: Double, humidity: Double): Status {

        // 1. 기상청 표준 불쾌지수(DI) 계산
        val di = 1.8 * temp - 0.55 * (1.0 - humidity / 100.0) * (1.8 * temp - 26.0) + 32.0

        // 2. 야외 환경이므로 CO2는 제외하고 불쾌지수만으로 쾌적도 판별
        return when {
            di >= 80 -> Status.BAD
            di >= 75 -> Status.NORMAL
            else -> Status.GOOD
        }
    }
}