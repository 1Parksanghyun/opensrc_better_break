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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.foundation.clickable
import androidx.compose.runtime.rememberCoroutineScope
import com.google.android.gms.maps.CameraUpdateFactory
import kotlinx.coroutines.launch

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
    private var mockJob: Job? = null // 타이머 역할을 할 코루틴 Job

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
                                    .sortedBy { sensor ->
                                        // 🔥 수정된 부분: CO2 대신 온도와 습도를 가져옵니다.
                                        val temp = sensor.temperature ?: 0.0
                                        val humidity = sensor.humidity ?: 0.0

                                        // 🔥 수정된 부분: calculate 대신 새로운 analyze 함수를 호출합니다.
                                        CongestionAnalyzer.analyze(temp, humidity).ordinal
                                    }

                                // 2. 필터링 및 정렬이 완료된 리스트를 바탕으로 RestPlaceCard 배치
                                displaySensors.forEach { sensor ->
                                    RestPlaceCard(
                                        sensor = sensor,
                                        onClick = {
                                            // 1. 마커를 클릭한 것과 동일하게 팝업창 상태 열기
                                            selectedSensor = sensor

                                            // 2. (선택 사항) 클릭한 카드의 센서 위치로 지도 카메라 자동 이동
                                            if (sensor.latitude != null && sensor.longitude != null) {
                                                cameraPositionState.position = CameraPosition.fromLatLngZoom(
                                                    LatLng(sensor.latitude, sensor.longitude),
                                                    15f
                                                )
                                            }
                                        }
                                    )
                                }
                            }

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
        val fixedLocations = listOf(
            Triple("sensor 01", 36.626906, 127.457722),
            Triple("sensor 02", 36.625851, 127.457487),
            Triple("sensor 03", 36.627369, 127.457839),
            Triple("sensor 04", 36.626878, 127.458745),
            Triple("sensor 05", 36.627389, 127.458433),
            Triple("sensor 06", 36.628191, 127.456860),
            Triple("sensor 07", 36.628895, 127.457839),
            Triple("sensor 08", 36.630404, 127.454361),
            Triple("sensor 09", 36.629398, 127.454361),
            Triple("sensor 10", 36.628384, 127.455681),
            Triple("sensor 11", 36.627823, 127.459118),
            Triple("sensor 12", 36.627453, 127.455976),
            Triple("sensor 13", 36.631825, 127.452925),
            Triple("sensor 14", 36.628885, 127.459296),
            Triple("sensor 15", 36.629740, 127.456406),
            Triple("sensor 16", 36.628021, 127.454927),
            Triple("sensor 17", 36.626295, 127.454117),
            Triple("sensor 18", 36.627520, 127.453162),
            Triple("sensor 19", 36.627597, 127.455668),
            Triple("sensor 20", 36.625360, 127.455642),
            Triple("sensor 21", 36.627443, 127.454431),
            Triple("sensor 22", 36.626023, 127.455356),
            Triple("sensor 23", 36.625433, 127.456917),
            Triple("sensor 24", 36.629449, 127.453645),
            Triple("sensor 25", 36.628000, 127.455000),
            Triple("sensor 26", 36.626035, 127.456245),
            Triple("sensor 27", 36.626333, 127.455760),
            Triple("sensor 28", 36.625672, 127.456219),
            Triple("sensor 29", 36.626717, 127.454132),
            Triple("sensor 30", 36.626042, 127.454686),
            Triple("sensor 31", 36.629400, 127.457200),
            Triple("sensor 32", 36.630958, 127.454715),
            Triple("sensor 33", 36.630931, 127.454222),
            Triple("sensor 34", 36.631461, 127.453407)
        )

        // 2. 초기 앱 구동 시: 기본 랜덤 값 할당
        val baseSensors = fixedLocations.map { (name, lat, lng) ->
            SensorData(
                sensor = name,
                latitude = lat,
                longitude = lng,
                temperature = Math.round(Random.nextDouble(20.0, 26.0) * 10.0) / 10.0,
                humidity = Math.round(Random.nextDouble(40.0, 60.0) * 10.0) / 10.0,
                co2 = Random.nextLong(400, 800)
            )
        }

        sensorList.clear()
        sensorList.addAll(baseSensors)

        var internalState = baseSensors.toList()

        val TEMP_THRESHOLD = 0.5
        val HUMIDITY_THRESHOLD = 1.0
        val CO2_THRESHOLD = 30

        // 🔥 흐른 시간을 기록할 변수 추가
        var elapsedSeconds = 0

        mockJob?.cancel()
        mockJob = lifecycleScope.launch {
            while (isActive) {
                delay(1000) // 1초 대기
                elapsedSeconds++ // 1초마다 카운트 증가

                // A. 기본 미세 변동 (기존과 동일)
                internalState = internalState.map { old ->
                    val newTemp = (old.temperature ?: 25.0) + Random.nextDouble(-3.0, 3.0)
                    val newHum = (old.humidity ?: 50.0) + Random.nextDouble(-10.0, 10.0)
                    old.copy(
                        temperature = newTemp,
                        humidity = newHum,
                        co2 = (old.co2 ?: 400) + Random.nextLong(-100, 100)
                    )
                }

                // B. 🔥 [시나리오 연출] 특정 시간에 특정 센서 값 강제 조작 🔥
                // toMutableList()를 통해 리스트를 수정 가능하게 연 뒤, 원하는 인덱스의 값을 덮어씌웁니다.
                internalState = internalState.toMutableList().apply {

                    // 시나리오 1: 앱 실행 20초 뒤, "sensor 03"의 센서의 값이 비상식적인 수치가 나타남
                    if (elapsedSeconds == 20) {
                        val idx = indexOfFirst { it.sensor == "sensor 03" }
                        if (idx != -1) {
                            this[idx] = this[idx].copy(
                                temperature = 3500.0, // 온도 35도
                                humidity = 90.0,    // 습도 90%
                                co2 = 30000          // CO2 3000 ppm
                            )
                        }
                    }


                    // 필요한 시나리오가 있다면 여기에 계속 if문을 추가하시면 됩니다.

                }.toList()

                // C. 내부 데이터와 화면 데이터 비교 후 업데이트 (임계값 렉 방지 로직)
                for (i in internalState.indices) {
                    val internal = internalState[i]
                    val displayed = sensorList[i]

                    val tempDiff = Math.abs((internal.temperature ?: 0.0) - (displayed.temperature ?: 0.0))
                    val humDiff = Math.abs((internal.humidity ?: 0.0) - (displayed.humidity ?: 0.0))
                    val co2Diff = Math.abs((internal.co2 ?: 0L) - (displayed.co2 ?: 0L))

                    if (tempDiff >= TEMP_THRESHOLD || humDiff >= HUMIDITY_THRESHOLD || co2Diff >= CO2_THRESHOLD) {
                        sensorList[i] = internal.copy(
                            temperature = Math.round((internal.temperature ?: 0.0) * 10.0) / 10.0,
                            humidity = Math.round((internal.humidity ?: 0.0) * 10.0) / 10.0
                        )
                    }
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
    fun RestPlaceCard(sensor: SensorData, onClick: () -> Unit) { // 🔥 onClick 매개변수 추가
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
                .clickable { onClick() } // 🔥 클릭 이벤트 추가 (padding 이전에 작성해야 전체 영역이 클릭됩니다)
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
    }}