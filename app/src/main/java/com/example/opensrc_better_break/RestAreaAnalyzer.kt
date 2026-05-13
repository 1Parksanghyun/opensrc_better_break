@file:Suppress("UNUSED")

package com.example.opensrc_better_break

/**
 * 라즈베리파이 -> DB를 거쳐 수신된 센서 데이터를 분석하는 클래스
 */
object RestAreaAnalyzer {

    // 화면에 뿌려줄 상태값 (텍스트와 색상만 심플하게 전달)
    enum class Status(val label: String, val colorCode: String) {
        GOOD("쾌적", "#4CAF50"),
        NORMAL("보통", "#FFEB3B"),
        BAD("혼잡/불쾌", "#F44336")
    }

    /**
     * @param temp DB에서 가져온 온도 (C)
     * @param humidity DB에서 가져온 습도 (%)
     * @param co2 DB에서 가져온 이산화탄소 (ppm)
     * @param isIndoor 실내 여부 (DB 장소 테이블에 실내/실외 구분 플래그가 있다고 가정)
     */
    fun analyze(temp: Double, humidity: Double, co2: Int, isIndoor: Boolean): Status {

        // 1. 불쾌지수(DI) 계산
        val di = 1.8 * temp - 0.55 * (1.0 - humidity / 100.0) * (1.8 * temp - 26.0) + 32.0

        // 2. 장소 특성에 따른 분기 처리 (조원 피드백 완벽 반영)
        return if (isIndoor) {
            // [실내] 밀폐 공간이므로 CO2(사람 수)와 불쾌지수(온습도)를 모두 적용
            when {
                co2 >= 1200 || di >= 80 -> Status.BAD
                co2 >= 800 || di >= 75 -> Status.NORMAL
                else -> Status.GOOD
            }
        } else {
            // [야외] 대기 중으로 CO2가 흩어지므로 센서값 무시. 오직 '불쾌지수'만으로 판별
            when {
                di >= 80 -> Status.BAD
                di >= 75 -> Status.NORMAL
                else -> Status.GOOD
            }
        }
    }
}