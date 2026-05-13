@file:Suppress("UNUSED") // 이 파일 전체의 '미사용' 경고 강제 미표시

package com.example.opensrc_better_break // ※ 주의: 이 부분은 프로젝트의 실제 패키지명과 똑같이 맞출 것.

// 1. 혼잡도 상태를 명확하게 분류해두는 Enum 클래스
enum class CongestionLevel(val title: String, val message: String, val colorCode: String) {
    RELAXED("여유", "쾌적하고 한산한 공간입니다.", "#4CAF50"),       // 초록색
    NORMAL("보통", "적당한 인원이 있는 공간입니다.", "#FFEB3B"),        // 노란색
    CROWDED("혼잡", "다소 붐빕니다. 다른 휴식처를 권장합니다.", "#FF9800"),  // 주황색
    VERY_CROWDED("매우 혼잡", "공기가 탁하고 혼잡합니다. 피하세요.", "#F44336") // 빨간색
}

// 2. 혼잡도 계산기 객체
object CongestionAnalyzer {

    /**
     * @param currentCo2 현재 CO2 수치
     * @param baseCo2 평상시 기준 CO2 수치
     * @param bleCount 1분간 스캔된 블루투스 기기 수
     * @return CongestionLevel (혼잡도 상태 객체)
     */
    fun calculate(currentCo2: Int, baseCo2: Int, bleCount: Int): CongestionLevel {

        // 1. CO2 증가량 계산
        val co2Spike = currentCo2 - baseCo2

        // 2. CO2 가중치 포인트
        val co2Point = when {
            co2Spike >= 300 -> 2 // 300 이상 급증하면 위험
            co2Spike >= 100 -> 1 // 100 이상 증가면 주의
            else -> 0
        }

        // 3. BLE 기기 가중치 포인트
        val blePoint = when {
            bleCount >= 15 -> 2 // 15대 이상 스캔되면 밀집
            bleCount >= 6 -> 1  // 6대 이상이면 보통
            else -> 0
        }

        // 4. 합산 및 최종 판정
        val totalPoint = co2Point + blePoint

        return when (totalPoint) {
            0 -> CongestionLevel.RELAXED
            1, 2 -> CongestionLevel.NORMAL
            3 -> CongestionLevel.CROWDED
            else -> CongestionLevel.VERY_CROWDED
        }
    }
}