package com.example.opensrc_better_break

import androidx.compose.runtime.Composable
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

/**
 * Google Maps Compose Marker 모듈
 * - 위도/경도 기반 마커 표시
 * - title optional
 * - MVVM에서도 재사용 가능
 */
object MapMarkerModule {

    @Composable
    fun ShowMarker(
        latitude: Double?,
        longitude: Double?,
        title: String? = null
    ) {
        if (latitude == null || longitude == null) return

        val position = LatLng(latitude, longitude)

        Marker(
            state = MarkerState(position = position),
            title = title
        )
    }
}

