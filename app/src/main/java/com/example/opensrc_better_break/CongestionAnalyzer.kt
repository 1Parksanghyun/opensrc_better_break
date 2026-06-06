@file:Suppress("UNUSED")

package com.example.opensrc_better_break

// 1. 기존 혼잡도 상태 Enum 유지 (totalPoint 반환 블록과 짝을 맞추기 위함)
enum class CongestionLevel(val title: String, val message: String, val colorCode: String) {
    RELAXED("쾌적", "쾌적하고 한산한 공간입니다.", "#4CAF50"),
    NORMAL("보통", "적당한 환경의 공간입니다.", "#FFEB3B"),
    CROWDED("불쾌", "다른 휴식처를 권장합니다.", "#FF9800"),
    VERY_CROWDED("매우 불쾌", "매우 불쾌한 곳 입니다. 피하세요.", "#F44336")
}

// 2. 불쾌지수 기반 분석기로 업데이트된 CongestionAnalyzer [cite: 51, 52]
object CongestionAnalyzer {

    /**
     * @param temp DB에서 가져온 야외 온도 (C) [cite: 52]
     * @param humidity DB에서 가져온 야외 습도 (%) [cite: 52]
     */
    fun analyze(temp: Double, humidity: Double): CongestionLevel {

        // 기상청 표준 불쾌지수(DI) 계산 [cite: 53]
        val di = 1.8 * temp - 0.55 * (1.0 - humidity / 100.0) * (1.8 * temp - 26.0) + 32.0

        // 불쾌지수를 totalPoint 로 매핑 (요청하신 반환 블록을 위해 포인트 부여)
        val totalPoint = when {
            di >= 83 -> 4 // 83 이상: 매우 불쾌 (else 조건으로 VERY_CROWDED 매핑)
            di >= 80 -> 3 // 80 이상: 불쾌 (CROWDED 매핑) [cite: 53]
            di >= 75 -> 1 // 75 이상: 보통 (1 또는 2 이므로 NORMAL 매핑) [cite: 53]
            else -> 0     // 75 미만: 쾌적 (RELAXED 매핑) [cite: 54]
        }

        // 🔥 요청하신 기존 반환 블록 완벽 유지 🔥
        return when (totalPoint) {
            0 -> CongestionLevel.RELAXED
            1, 2 -> CongestionLevel.NORMAL
            3 -> CongestionLevel.CROWDED
            else -> CongestionLevel.VERY_CROWDED
        }
    }
}

// -----------------------------------------------------
// 3. 함께 주신 정렬(Sort) 기능 및 데이터 클래스 연동 업데이트 [cite: 55]
// -----------------------------------------------------

// 휴식처 데이터를 담을 데이터 클래스 (Status 대신 CongestionLevel 사용) [cite: 55]
data class RestArea(
    val name: String,
    val distance: Double,
    val comfortStatus: CongestionLevel,
    val diValue: Int
)

// 정렬을 위한 점수 계산 헬퍼 함수 [cite: 56]
private fun getComfortScore(status: CongestionLevel): Int {
    return when (status) {
        CongestionLevel.RELAXED -> 4
        CongestionLevel.NORMAL -> 3
        CongestionLevel.CROWDED -> 2
        CongestionLevel.VERY_CROWDED -> 1
    }
}

// 거리 우선 정렬 함수 [cite: 56]
// (1순위: 거리 가까운 순 -> 2순위: 쾌적도 좋은 순 -> 3순위: 불쾌지수 낮은 순) [cite: 56]
fun sortRestAreasByDistance(restAreas: List<RestArea>): List<RestArea> {
    return restAreas.sortedWith(
        compareBy<RestArea> { it.distance }
            .thenByDescending { getComfortScore(it.comfortStatus) } // [cite: 56]
            .thenBy { it.diValue } // [cite: 57]
    )
}

// 쾌적도 우선 정렬 함수 [cite: 57]
fun sortRestAreasByComfort(restAreas: List<RestArea>): List<RestArea> {
    return restAreas.sortedWith(
        compareByDescending<RestArea> { getComfortScore(it.comfortStatus) }
            .thenBy { it.distance } // [cite: 57]
            .thenBy { it.diValue }  // [cite: 57]
    )
}