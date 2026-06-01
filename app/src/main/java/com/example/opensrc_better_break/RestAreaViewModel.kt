@file:Suppress("UNUSED")
package com.example.opensrc_better_break

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// UI(화면)와 정렬 로직을 분리해주는 뷰모델(ViewModel) 클래스.
class RestAreaViewModel : ViewModel() {

    // 현재 화면에 보여질 휴식처 리스트 데이터를 보관하는 곳 (컴포즈 전용 상태 관리)
    private val _restAreaList = MutableStateFlow<List<RestArea>>(emptyList())
    val restAreaList: StateFlow<List<RestArea>> = _restAreaList.asStateFlow()

    // 1. [거리 우선] 버튼이 눌렸을 때 UI 담당이 호출할 함수
    fun sortByDistance() {
        val currentList = _restAreaList.value
        // SortUtils.kt에 만들어둔 함수를 써서 정렬 후 화면 업데이트
        _restAreaList.value = sortRestAreasByDistance(currentList)
    }

    // 2. [쾌적도 우선] 버튼이 눌렸을 때 UI 담당이 호출할 함수
    fun sortByComfort() {
        val currentList = _restAreaList.value
        // SortUtils.kt에 만들어둔 함수를 써서 정렬 후 화면 업데이트
        _restAreaList.value = sortRestAreasByComfort(currentList)
    }

    // (참고) 나중에 DB에서 데이터를 받아오면 이 함수를 통해 리스트에 집어넣게 됨.
    fun setInitialData(data: List<RestArea>) {
        _restAreaList.value = data
    }
}