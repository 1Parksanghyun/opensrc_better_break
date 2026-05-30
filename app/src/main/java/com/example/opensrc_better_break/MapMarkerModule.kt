package com.example.opensrc_better_break

import androidx.compose.runtime.Composable
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

object MapMarkerModule {

    @Composable
    fun ShowMarker(
        latitude: Double?,
        longitude: Double?,
        title: String? = null,
        onClick: (() -> Unit)? = null
    ) {
        if (latitude == null || longitude == null) return

        val position = LatLng(latitude, longitude)

        Marker(
            state = MarkerState(position = position),
            title = title,
            onClick = {
                onClick?.invoke()
                true
            }
        )
    }
}

