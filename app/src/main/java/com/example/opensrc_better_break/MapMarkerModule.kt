@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.example.opensrc_better_break

import androidx.compose.runtime.Composable
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState

object MapMarkerModule {
    @Composable
    @GoogleMapComposable
    fun ShowMarker(
        latitude: Double?,
        longitude: Double?,
        title: String?,
        onClick: () -> Unit // 🔥 클릭 이벤트를 MainActivity로 전달
    ) {
        if (latitude == null || longitude == null) return

        val position = LatLng(latitude, longitude)
        val markerState = rememberMarkerState(position = position)

        // 클릭 인식을 위해 가짜 타이틀 부여 (필수)

        Marker(
            state = markerState,
            title = title ?: "센서",
            onClick = {
                onClick() // 마커 클릭 시 전달받은 함수 실행
                false // false를 반환해야 카메라 이동 애니메이션이 작동함
            }
        )
    }
}