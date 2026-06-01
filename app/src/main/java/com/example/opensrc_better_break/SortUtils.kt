@file:Suppress("UNUSED") // 1. 뷰모델과 마찬가지로 미사용 경고(노란줄)를 미리 꺼둡니다.

package com.example.opensrc_better_break

// 휴식처 데이터를 담을 데이터 클래스
data class RestArea(
    val name: String,
    val distance: Double,
    val comfortStatus: CongestionAnalyzer.Status,
    val diValue: Int
)

// 2. 이 헬퍼 함수는 이 파일 안에서만 쓰이므로 private 키워드를 붙여 은닉화(캡슐화) 해줍니다.
private fun getComfortScore(status: CongestionAnalyzer.Status): Int {
    return when (status) {
        CongestionAnalyzer.Status.GOOD -> 3
        CongestionAnalyzer.Status.NORMAL -> 2
        CongestionAnalyzer.Status.BAD -> 1
    }
}

// 거리 우선 정렬 함수
// (1순위: 거리 가까운 순 -> 2순위: 쾌적도 좋은 순 -> 3순위: 불쾌지수 낮은 순)
fun sortRestAreasByDistance(restAreas: List<RestArea>): List<RestArea> {
    return restAreas.sortedWith(
        compareBy<RestArea> { it.distance }
            .thenByDescending { getComfortScore(it.comfortStatus) }
            .thenBy { it.diValue }
    )
}

// 쾌적도 우선 정렬 함수
fun sortRestAreasByComfort(restAreas: List<RestArea>): List<RestArea> {
    return restAreas.sortedWith(
        compareByDescending<RestArea> { getComfortScore(it.comfortStatus) }
            .thenBy { it.distance }
            .thenBy { it.diValue } // 3. 만약 쾌적도와 거리가 둘 다 똑같을 경우를 대비해 3순위(불쾌지수) 판별을 추가했습니다!
    )
}