package com.example.osmapp

import org.osmdroid.util.GeoPoint

data class MapLocation(

    val title: String,

    val position: GeoPoint,

    val restScore: Int,

    val properties: MutableMap<String, Any>
)