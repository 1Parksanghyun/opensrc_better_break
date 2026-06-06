package com.example.opensrc_better_break

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.opensrc_better_break.ui.theme.Opensrc_better_breakTheme
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.rememberCameraPositionState

data class SensorData(
    val sensor: String? = "",
    val co2: Long? = 0,
    val temperature: Double? = 0.0,
    val humidity: Double? = 0.0,
    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0,
    val fresh: Boolean? = false,
    val time: String? = "",
    val savedAt: Timestamp? = null,
    // 아래 두 변수 추가 (Firebase 필드명과 일치해야 함)
    val baseCo2: Long? = 400,
    val bleCount: Int? = 0
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
            // 🔥 선택된 센서를 기억하는 상태 변수
            var selectedSensor by remember { mutableStateOf<SensorData?>(null) }

            Opensrc_better_breakTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val cbnu = LatLng(36.6300, 127.4551)
                    val scrollState = rememberScrollState()
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(cbnu, 15f)
                    }

                    // 🔥 Box로 전체를 감싸서 지도 위에 팝업이 뜰 수 있게 만듭니다.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // [레이어 1] 지도 및 리스트
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            GoogleMap(
                                modifier = Modifier
                                    .fillMaxWidth(0.9f)
                                    .fillMaxHeight(0.7f),
                                cameraPositionState = cameraPositionState,
                                onMapClick = { selectedSensor = null } // 지도 빈 곳 클릭 시 팝업 닫기
                            ) {
                                sensorList.forEach { sensor ->
                                    // 센서 리스트 갱신 시 마커가 깜빡이는 것을 방지하기 위해 key 부여
                                    key(sensor.sensor) {
                                        MapMarkerModule.ShowMarker(
                                            latitude = sensor.latitude,
                                            longitude = sensor.longitude,
                                            title = sensor.sensor,
                                            onClick = {
                                                selectedSensor = sensor // 마커 클릭 시 팝업 열기
                                            }
                                        )
                                    }
                                }
                            }

                            // ... (위쪽 코드 생략) ...
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                            ) {
                                // 1. "pws 01"과 "pws 02" 센서 제외하고, 혼잡도 레벨 기준으로 정렬
                                val displaySensors = sensorList
                                    .filter { sensor ->
                                        // 센서 이름이 "pws 01"이 아니고 "pws 02"도 아닌 것만 필터링
                                        sensor.sensor != "pws 01" && sensor.sensor != "pws 02"
                                    }
                                    .sortedBy { sensor ->
                                        // 🔥 수정된 부분: CO2 대신 온도와 습도를 가져옵니다.
                                        val temp = sensor.temperature ?: 0.0
                                        val humidity = sensor.humidity ?: 0.0

                                        // 🔥 수정된 부분: calculate 대신 새로운 analyze 함수를 호출합니다.
                                        CongestionAnalyzer.analyze(temp, humidity).ordinal
                                    }

                                // 2. 필터링 및 정렬이 완료된 리스트를 바탕으로 RestPlaceCard 배치
                                displaySensors.forEach { sensor ->
                                    RestPlaceCard(sensor)
                                }
                            }
// ... (아래쪽 코드 생략) ...

// ... (아래쪽 코드 생략) ...

                        }

                        // [레이어 2] 마커 클릭 시 나타나는 하단 팝업창
                        AnimatedVisibility(
                            visible = selectedSensor != null,
                            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp)
                                .padding(bottom = 32.dp) // 리스트와 겹치지 않게 여백 추가
                        ) {
                            selectedSensor?.let { sensor ->
                                SensorDetailPopup(
                                    sensor = sensor,
                                    onClose = { selectedSensor = null }
                                )
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

// -----------------------------------------------------
// UI 컴포저블 모음
// -----------------------------------------------------

@Composable
fun SensorDetailPopup(sensor: SensorData, onClose: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "📍 ${sensor.sensor ?: "알 수 없는 센서"}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color.Gray
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "온도", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = "${sensor.temperature ?: "- "}°C",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "습도", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = "${sensor.humidity ?: "- "}%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "CO2", fontSize = 12.sp, color = Color.Gray)
                    Text(
                        text = "${sensor.co2 ?: "- "} ppm",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if ((sensor.co2 ?: 0) > 1000) Color.Red else Color.Black
                    )
                }
            }
        }
    }
}

@Composable
fun RestPlaceCard(sensor: SensorData) {
    // 🔥 추가된 부분: "vs 01" 또는 "vs 02" 센서인 경우 카드 자체를 그리지 않고 제외합니다.
    if (sensor.sensor == "vs 01" || sensor.sensor == "vs 02") {
        return
    }

    // 1. 필요한 데이터를 안전하게 가져오기 (null 처리)
    val temp = sensor.temperature ?: 0.0
    val humidity = sensor.humidity ?: 0.0

    // 2. 바뀐 불쾌지수 분석기 호출
    val congestionLevel = CongestionAnalyzer.analyze(temp, humidity)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .border(1.dp, Color.Gray)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("센서: ${sensor.sensor}")
            Text("온도: ${sensor.temperature ?: "- "}°C")
            Text("습도: ${sensor.humidity ?: "- "}%")
        }

        // 3. 계산된 혼잡도 상태 표시
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = congestionLevel.title,
                fontWeight = FontWeight.Bold,
                color = Color(android.graphics.Color.parseColor(congestionLevel.colorCode))
            )
            Text(
                text = congestionLevel.message,
                fontSize = 12.sp,
                color = Color.Gray
            )
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