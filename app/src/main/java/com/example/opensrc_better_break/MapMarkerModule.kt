@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.example.opensrc_better_break

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.CameraPositionState
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import kotlinx.coroutines.launch

object MapMarkerModule {

    @Composable
    @GoogleMapComposable
    fun ShowMarker(
        latitude: Double?,
        longitude: Double?,
        title: String?,
        cameraPositionState: CameraPositionState,
        // 마커 클릭 시 원래 줌을 저장할 콜백 (MainActivity에서 remember로 관리)
        onMarkerClick: (savedZoom: Float) -> Unit
    ) {
        if (latitude == null || longitude == null) return

        val position = LatLng(latitude, longitude)
        val markerState = rememberMarkerState(position = position)
        val scope = rememberCoroutineScope()

        Marker(
            state = markerState,
            title = title ?: "센서",
            onClick = {
                scope.launch {
                    // ① 클릭 전 줌 저장 → MainActivity의 savedZoom 상태에 기록
                    val originalZoom = cameraPositionState.position.zoom
                    onMarkerClick(originalZoom)

                    // ② 마커 위치를 중심으로 이동하면서 1단계 줌인 (+2)
                    cameraPositionState.animate(
                        CameraUpdateFactory.newLatLngZoom(position, originalZoom + 2f),
                        durationMs = 400
                    )

                    // ③ 2단계 줌인 (+2 추가, 총 +4) — 위치는 그대로 유지
                    cameraPositionState.animate(
                        CameraUpdateFactory.zoomBy(2f),
                        durationMs = 400
                    )
                    // ※ 원복은 빈 곳 클릭(onMapClick) 시 MainActivity에서 처리
                }
                false
            }
        )
    }
}