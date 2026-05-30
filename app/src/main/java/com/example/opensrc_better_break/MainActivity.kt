package com.example.opensrc_better_break

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.opensrc_better_break.ui.theme.Opensrc_better_breakTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.EventListener
import com.google.firebase.Timestamp
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember

data class SensorData(
    val sensor: String? = "",
    val co2: Long? = 0,
    val temperature: Double? = 0.0,
    val humidity: Double? = 0.0,
    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0,
    val fresh: Boolean? = false,
    val time: String? = "",
    val savedAt: Timestamp? = null
)
class MainActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "firebase"
    private val sensorList = mutableStateListOf<SensorData>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        load_sensordata()
        setContent {
            val selectedSensor = androidx.compose.runtime.remember {
                androidx.compose.runtime.mutableStateOf<SensorData?>(null)
            }
            Opensrc_better_breakTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val cbnu = LatLng(36.6300, 127.4551)
                    val scrollState = rememberScrollState()
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(cbnu, 15f)
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(innerPadding),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GoogleMap(
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .fillMaxHeight(0.7f),
                            cameraPositionState = cameraPositionState
                        ) {

                            sensorList.forEach { sensor ->

                                MapMarkerModule.ShowMarker(
                                    latitude = sensor.latitude,
                                    longitude = sensor.longitude,
                                    title = sensor.sensor,
                                    onClick = {
                                        selectedSensor.value = sensor
                                    }
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                        ) {
                            sensorList.forEach { sensor ->
                                RestPlaceCard(sensor)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun load_sensordata() {
        db.collection("sensor_status")
            .addSnapshotListener { value, e ->

                if (e != null) {
                    Log.w(TAG, "연결 실패", e)
                    return@addSnapshotListener
                }

                sensorList.clear()

                value?.forEach { doc ->
                    val sensor = doc.toObject(SensorData::class.java)
                    sensorList.add(sensor)

                    Log.d(TAG, "센서: ${sensor.sensor}, 온도: ${sensor.temperature}, 습도: ${sensor.humidity}")
                }
            }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    Opensrc_better_breakTheme {
        Greeting("Android")
    }
}

@Composable
fun RestPlaceCard(sensor: SensorData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .border(1.dp, androidx.compose.ui.graphics.Color.Gray)
    ) {
        Column {
            Text("센서: ${sensor.sensor}")
            Text("온도: ${sensor.temperature}")
            Text("CO2: ${sensor.co2}")
        }
    }
}