package com.example.opensrc_better_break

// 1. 휴식처 데이터를 담을 데이터 클래스 (독립된 데이터 클래스로 확정)
data class RestArea(
    val name: String,
    val distance: Double,

    // RestAreaAnalyzer.kt에 정의된 정식 Status Enum 타입을 연결.
    val comfortStatus: CongestionAnalyzer.Status,
    val diValue: Int
)

// 2. 이미 정의된 Status Enum을 바탕으로 정렬 점수를 반환하는 헬퍼 함수
fun getComfortScore(status: CongestionAnalyzer.Status): Int {
    return when (status) {
        CongestionAnalyzer.Status.GOOD -> 3
        CongestionAnalyzer.Status.NORMAL -> 2
        CongestionAnalyzer.Status.BAD -> 1
    }
}

// 3. 거리 우선 정렬 함수
// (1순위: 거리 가까운 순 -> 2순위: 쾌적도 좋은 순 -> 3순위: 불쾌지수 낮은 순)
fun sortRestAreasByDistance(restAreas: List<RestArea>): List<RestArea> {
    return restAreas.sortedWith(
        compareBy<RestArea> { it.distance }
            .thenByDescending { getComfortScore(it.comfortStatus) }
            .thenBy { it.diValue }
    )
}

// 4. 쾌적도 우선 정렬 함수
// (1순위: 쾌적도 좋은 순 -> 2순위: 거리 가까운 순)
fun sortRestAreasByComfort(restAreas: List<RestArea>): List<RestArea> {
    return restAreas.sortedWith(
        compareByDescending<RestArea> { getComfortScore(it.comfortStatus) }
            .thenBy { it.distance }
    )
}