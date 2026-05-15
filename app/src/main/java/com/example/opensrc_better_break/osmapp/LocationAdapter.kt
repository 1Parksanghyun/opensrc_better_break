package com.example.osmapp

import android.location.Location

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.widget.ImageView
import android.widget.TextView

import androidx.recyclerview.widget.RecyclerView

class LocationAdapter(

    private val locations: List<MapLocation>,

    private val onItemClick:
        (MapLocation) -> Unit

) : RecyclerView.Adapter<LocationAdapter.ViewHolder>() {

    class ViewHolder(view: View)
        : RecyclerView.ViewHolder(view) {

        val imgAirQuality: ImageView =
            view.findViewById(R.id.imgAirQuality)

        val txtName: TextView =
            view.findViewById(R.id.txtName)

        val txtDistance: TextView =
            view.findViewById(R.id.txtDistance)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {

        val view = LayoutInflater
            .from(parent.context)

            .inflate(
                R.layout.item_location,
                parent,
                false
            )

        return ViewHolder(view)
    }

    override fun getItemCount(): Int {

        return locations.size
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int
    ) {

        val location = locations[position]

        // 이름
        holder.txtName.text =
            "이름 : ${location.title}"

        // 현재 위치
        val currentLat = 37.5665
        val currentLng = 126.9780

        val results = FloatArray(1)

        // 거리 계산
        Location.distanceBetween(

            currentLat,
            currentLng,

            location.position.latitude,
            location.position.longitude,

            results
        )

        val distanceKm =
            results[0] / 1000.0

        holder.txtDistance.text =
            "거리 : %.1fkm"
                .format(distanceKm)

        // 공기질 안전 처리
        val airQuality =
            location.properties["공기질"]
                    as? Int ?: 1

        // 이미지 선택
        val imageRes = when(airQuality) {

            1 -> R.drawable.air_quality_1

            2 -> R.drawable.air_quality_2

            3 -> R.drawable.air_quality_3

            else -> R.drawable.air_quality_1
        }

        holder.imgAirQuality
            .setImageResource(imageRes)

        // 카드 클릭
        holder.itemView.setOnClickListener {

            onItemClick(location)
        }
    }
}