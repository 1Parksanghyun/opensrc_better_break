package com.example.osmapp

import android.os.Bundle
import android.widget.TextView

import androidx.appcompat.app.AppCompatActivity

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class MainActivity : AppCompatActivity() {

    private lateinit var map: MapView

    private lateinit var recyclerView: RecyclerView

    private lateinit var txtDetail: TextView

    private val markerMap =
        mutableMapOf<MapLocation, Marker>()

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        Configuration.getInstance().userAgentValue =
            packageName

        setContentView(R.layout.activity_main)

        map = findViewById(R.id.map)

        recyclerView =
            findViewById(R.id.locationList)

        txtDetail =
            findViewById(R.id.txtDetail)

        // 지도 스타일
        map.setTileSource(
            TileSourceFactory.MAPNIK
        )

        // 줌 활성화
        map.setMultiTouchControls(true)

        // 시작 위치
        val center =
            GeoPoint(37.5665, 126.9780)

        val controller = map.controller

        controller.setZoom(11.0)

        controller.setCenter(center)

        // 위치 데이터
        val locations = listOf(

            MapLocation(
                title = "북한산",

                position = GeoPoint(
                    37.6587,
                    126.9770
                ),

                restScore = 98,

                properties = mutableMapOf(
                    "이름" to "북한산",
                    "온도" to "21°C",
                    "습도" to "48%",
                    "혼잡도" to "쾌적",
                    "불쾌지수" to 55,
                    "공기질" to 1,
                    "AQI" to 18,
                    "CO2" to "350ppm"
                )
            ),

            MapLocation(
                title = "서울시청",

                position = GeoPoint(
                    37.5665,
                    126.9780
                ),

                restScore = 92,

                properties = mutableMapOf(
                    "이름" to "서울시청",
                    "온도" to "26°C",
                    "습도" to "58%",
                    "혼잡도" to "보통",
                    "불쾌지수" to 72,
                    "공기질" to 1,
                    "AQI" to 42,
                    "CO2" to "420ppm"
                )
            ),

            MapLocation(
                title = "강남역",

                position = GeoPoint(
                    37.4979,
                    127.0276
                ),

                restScore = 65,

                properties = mutableMapOf(
                    "이름" to "강남역",
                    "온도" to "28°C",
                    "습도" to "65%",
                    "혼잡도" to "혼잡",
                    "불쾌지수" to 81,
                    "공기질" to 2,
                    "AQI" to 76,
                    "CO2" to "680ppm"
                )
            )
        )

        // 휴식 점수 높은 순 정렬
        val sortedLocations =
            locations.sortedByDescending {
                it.restScore
            }

        // 마커 추가
        for (location in sortedLocations) {

            addMarker(location)
        }

        // RecyclerView
        recyclerView.layoutManager =
            LinearLayoutManager(this)

        recyclerView.adapter =
            LocationAdapter(

                sortedLocations

            ) { selectedLocation ->

                moveToLocation(selectedLocation)
            }
    }

    /**
     * 공기질 텍스트 변환
     */
    private fun getAirQualityText(
        value: Int
    ): String {

        return when(value) {

            1 -> "좋음"

            2 -> "보통"

            3 -> "나쁨"

            else -> "알수없음"
        }
    }

    /**
     * 지도 마커 추가
     */
    private fun addMarker(
        location: MapLocation
    ) {

        val marker = Marker(map)

        marker.position =
            location.position

        marker.setAnchor(
            Marker.ANCHOR_CENTER,
            Marker.ANCHOR_BOTTOM
        )

        val infoText = buildString {

            append("${location.title}\n\n")

            append(
                "휴식 점수 : " +
                        "${location.restScore}\n\n"
            )

            location.properties.forEach {

                    (key, value) ->

                if (key == "공기질") {

                    append(
                        "$key : " +
                                getAirQualityText(
                                    value as? Int ?: 1
                                )
                    )

                } else {

                    append("$key : $value")
                }

                append("\n")
            }
        }

        marker.title = infoText

        map.overlays.add(marker)

        markerMap[location] = marker
    }

    /**
     * 카드 클릭 시 이동
     */
    private fun moveToLocation(
        location: MapLocation
    ) {

        val controller = map.controller

        controller.animateTo(
            location.position
        )

        controller.setZoom(15.0)

        // 팝업 표시
        markerMap[location]
            ?.showInfoWindow()

        // 상세정보
        val detailText = buildString {

            append("${location.title}\n\n")

            append(
                "휴식 점수 : " +
                        "${location.restScore}\n\n"
            )

            location.properties.forEach {

                    (key, value) ->

                if (key == "공기질") {

                    append(
                        "$key : " +
                                getAirQualityText(
                                    value as? Int ?: 1
                                )
                    )

                } else {

                    append("$key : $value")
                }

                append("\n")
            }
        }

        txtDetail.text = detailText
    }

    override fun onResume() {

        super.onResume()

        map.onResume()
    }

    override fun onPause() {

        super.onPause()

        map.onPause()
    }

    override fun onDestroy() {

        super.onDestroy()

        map.onDetach()
    }
}