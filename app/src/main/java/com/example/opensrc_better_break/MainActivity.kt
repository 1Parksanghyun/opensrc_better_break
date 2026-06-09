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
import androidx.compose.runtime.rememberCoroutineScope
import com.google.android.gms.maps.CameraUpdateFactory
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

// ── 기능 2·3: 센서 상태를 나타내는 Enum ───────────────────────────────────────────
enum class SensorStatus {
    NORMAL,         // 정상
    DATA_ERROR,     // 이상 데이터 (범위 초과)
    CONNECT_ERROR   // 연결 이상 (null 수신)
}

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
    val baseCo2: Long? = 400,
    val bleCount: Int? = 0,

    // ── 기능 2·3: 센서 상태 필드 ────────────────────────────────────────────────
    val status: SensorStatus = SensorStatus.NORMAL,

    // ── 기능 2: 습도 100% 이상 지속 시간 카운터 (초) ────────────────────────────
    val humidity100Seconds: Int = 0,

    // ── 기능 4·5: 이번 갱신이 미세 변경인지 여부 ────────────────────────────────
    val isMinorChange: Boolean = false
)

class MainActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val TAG = "firebase"
    private val sensorList = mutableStateListOf<SensorData>()
    private var mockJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        load_sensordata()

        setContent {
            var selectedSensor by remember { mutableStateOf<SensorData?>(null) }

            Opensrc_better_breakTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    val cbnu = LatLng(36.6300, 127.4551)
                    val scrollState = rememberScrollState()

                    // 기능 1: cameraPositionState를 MapMarkerModule.ShowMarker 에 전달합니다
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(cbnu, 15f)
                    }
                    // 마커 클릭 전 줌 레벨 저장 (빈 곳 클릭 시 이 값으로 원복)
                    var savedZoom by remember { mutableStateOf(15f) }
                    val mapScope = rememberCoroutineScope()

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
                                onMapClick = {
                                    // 빈 곳 클릭 → 팝업 닫기 + 저장된 줌으로 원복
                                    selectedSensor = null
                                    mapScope.launch {
                                        cameraPositionState.animate(
                                            CameraUpdateFactory.zoomTo(savedZoom),
                                            durationMs = 500
                                        )
                                    }
                                }
                            ) {
                                sensorList.forEach { sensor ->
                                    key(sensor.sensor) {
                                        // ── 기능 1: cameraPositionState 전달 ──────
                                        MapMarkerModule.ShowMarker(
                                            latitude = sensor.latitude,
                                            longitude = sensor.longitude,
                                            title = sensor.sensor,
                                            cameraPositionState = cameraPositionState,
                                            onMarkerClick = { zoom ->
                                                savedZoom = zoom   // 클릭 전 줌 저장
                                                selectedSensor = sensor
                                            }
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(scrollState)
                            ) {
                                val displaySensors = sensorList
                                    .sortedBy { sensor ->
                                        val temp = sensor.temperature ?: 0.0
                                        val humidity = sensor.humidity ?: 0.0
                                        CongestionAnalyzer.analyze(temp, humidity).ordinal
                                    }

                                displaySensors.forEach { sensor ->
                                    RestPlaceCard(
                                        sensor = sensor,
                                        onClick = {
                                            if (sensor.latitude != null && sensor.longitude != null) {
                                                // 마커 클릭과 동일한 효과: 현재 줌 저장 → 중심 이동 + 2단계 줌인
                                                savedZoom = cameraPositionState.position.zoom
                                                selectedSensor = sensor
                                                val target = LatLng(sensor.latitude, sensor.longitude)
                                                mapScope.launch {
                                                    cameraPositionState.animate(
                                                        CameraUpdateFactory.newLatLngZoom(target, savedZoom + 2f),
                                                        durationMs = 400
                                                    )
                                                    cameraPositionState.animate(
                                                        CameraUpdateFactory.zoomBy(2f),
                                                        durationMs = 400
                                                    )
                                                }
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
                                .padding(bottom = 32.dp)
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

    // ─────────────────────────────────────────────────────────────────────────────
    // 데이터 시뮬레이션 + 기능 2·3·4·5·6 핵심 로직
    // ─────────────────────────────────────────────────────────────────────────────
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

        // ── 기능 5: 미세 변경 임계값 (이 값 미만이면 화면 업데이트 안 함) ─────────
        val TEMP_THRESHOLD    = 0.5
        val HUMIDITY_THRESHOLD = 1.0
        val CO2_THRESHOLD     = 30L

        var elapsedSeconds = 0

        mockJob?.cancel()
        mockJob = lifecycleScope.launch {
            while (isActive) {
                delay(1000)
                elapsedSeconds++

                // ── A. 기능 4: 미세 변경 7 : 일반 변경 3 비율로 데이터 생성 ──────────
                // 이상 상태인 센서(시나리오 주입 포함)는 랜덤 변동을 건너뜀 → 시나리오 값 보존
                internalState = internalState.map { old ->
                    if (old.status == SensorStatus.DATA_ERROR || old.status == SensorStatus.CONNECT_ERROR) {
                        return@map old
                    }

                    val isMinor = Random.nextFloat() < 0.7f

                    val rawTemp: Double
                    val rawHum:  Double
                    val rawCo2:  Long

                    if (isMinor) {
                        rawTemp = (old.temperature ?: 25.0) + Random.nextDouble(-0.3, 0.3)
                        rawHum  = (old.humidity    ?: 50.0) + Random.nextDouble(-0.5, 0.5)
                        rawCo2  = (old.co2         ?: 400L) + Random.nextLong(-10, 10)
                    } else {
                        rawTemp = (old.temperature ?: 25.0) + Random.nextDouble(-3.0, 3.0)
                        rawHum  = (old.humidity    ?: 50.0) + Random.nextDouble(-10.0, 10.0)
                        rawCo2  = (old.co2         ?: 400L) + Random.nextLong(-100, 100)
                    }

                    // ★ 핵심 수정: 정상 운용 범위로 클램핑
                    // → 랜덤 누적으로 이상값 범위에 빠지지 않음
                    // → 이상 감지 조건(15/45°C, 15%/100%, 380/550ppm)보다 넉넉한 내부 범위 사용
                    old.copy(
                        temperature   = rawTemp.coerceIn(18.0, 32.0),
                        humidity      = rawHum.coerceIn(30.0, 80.0),
                        co2           = rawCo2.coerceIn(390L, 540L),
                        isMinorChange = isMinor
                    )
                }

                // ── B. 기능 6: 시나리오 연출 ─────────────────────────────────────────
                internalState = internalState.toMutableList().apply {

                    // ─ 시나리오 2-1 (기능 2 테스트): 15초 뒤 sensor 03 온도 이상값 주입
                    //   기대 결과: 카드에 "온도 이상" 표시
                    if (elapsedSeconds == 15) {
                        val idx = indexOfFirst { it.sensor == "sensor 03" }
                        if (idx != -1) this[idx] = this[idx].copy(
                            temperature   = 50.0,   // 45도 초과 → 이상
                            humidity      = 55.0,
                            co2           = 450,
                            status        = SensorStatus.DATA_ERROR, // 섹션A 랜덤변동 차단
                            isMinorChange = false
                        )
                        Log.d(TAG, "[시나리오] 15s: sensor 03 온도 이상값 주입 (50°C)")
                    }

                    // ─ 시나리오 2-2 (기능 2 테스트): 25초 뒤 sensor 03 정상값 복구
                    //   기대 결과: 카드가 정상 데이터로 업데이트됨
                    if (elapsedSeconds == 25) {
                        val idx = indexOfFirst { it.sensor == "sensor 03" }
                        if (idx != -1) this[idx] = this[idx].copy(
                            temperature   = 24.0,
                            humidity      = 52.0,
                            co2           = 430,
                            status        = SensorStatus.NORMAL, // 이상 해제
                            isMinorChange = false
                        )
                        Log.d(TAG, "[시나리오] 25s: sensor 03 정상값 복구")
                    }

                    // ─ 시나리오 2-3 (기능 2 테스트): 35초 뒤 sensor 05 CO2 이상값 주입
                    //   기대 결과: 카드에 "데이터 이상" 표시
                    if (elapsedSeconds == 35) {
                        val idx = indexOfFirst { it.sensor == "sensor 05" }
                        if (idx != -1) this[idx] = this[idx].copy(
                            temperature   = 23.0,
                            humidity      = 50.0,
                            co2           = 600,    // 550 초과 → 이상
                            status        = SensorStatus.DATA_ERROR, // 섹션A 랜덤변동 차단
                            isMinorChange = false
                        )
                        Log.d(TAG, "[시나리오] 35s: sensor 05 CO2 이상값 주입 (600 ppm)")
                    }

                    // ─ 시나리오 2-4 (기능 2 테스트): 45초 뒤 sensor 05 CO2 정상 복구
                    if (elapsedSeconds == 45) {
                        val idx = indexOfFirst { it.sensor == "sensor 05" }
                        if (idx != -1) this[idx] = this[idx].copy(
                            co2           = 430,
                            status        = SensorStatus.NORMAL, // 이상 해제
                            isMinorChange = false
                        )
                        Log.d(TAG, "[시나리오] 45s: sensor 05 CO2 정상 복구")
                    }

                    // ─ 시나리오 3-1 (기능 3 테스트): 55초 뒤 sensor 07 null 주입
                    //   기대 결과: 카드에 "연결 이상" 표시
                    if (elapsedSeconds == 55) {
                        val idx = indexOfFirst { it.sensor == "sensor 07" }
                        if (idx != -1) this[idx] = this[idx].copy(
                            temperature   = null,  // null → 연결 이상
                            humidity      = null,
                            co2           = null,
                            status        = SensorStatus.CONNECT_ERROR, // 섹션A 랜덤변동 차단
                            isMinorChange = false
                        )
                        Log.d(TAG, "[시나리오] 55s: sensor 07 null 주입 (연결 이상)")
                    }

                    // ─ 시나리오 3-2 (기능 3 테스트): 65초 뒤 sensor 07 정상 복구
                    if (elapsedSeconds == 65) {
                        val idx = indexOfFirst { it.sensor == "sensor 07" }
                        if (idx != -1) this[idx] = this[idx].copy(
                            temperature   = 22.0,
                            humidity      = 48.0,
                            co2           = 420,
                            status        = SensorStatus.NORMAL, // 연결 이상 해제
                            isMinorChange = false
                        )
                        Log.d(TAG, "[시나리오] 65s: sensor 07 정상 복구")
                    }

                    // ─ 시나리오 5-1 (기능 5 테스트): 75초 뒤 sensor 01에 미세 변경 강제 적용
                    //   기대 결과: internalState는 변하지만 화면(sensorList)은 갱신되지 않음
                    if (elapsedSeconds == 75) {
                        val idx = indexOfFirst { it.sensor == "sensor 01" }
                        if (idx != -1) this[idx] = this[idx].copy(
                            temperature   = (this[idx].temperature ?: 25.0) + 0.1, // TEMP_THRESHOLD(0.5) 미만
                            isMinorChange = true
                        )
                        Log.d(TAG, "[시나리오] 75s: sensor 01 미세 변경 강제 (화면 갱신 없어야 함)")
                    }

                    // ─ 시나리오 5-2 (기능 5 테스트): 80초 뒤 sensor 01에 일반 변경 적용
                    //   기대 결과: 화면이 갱신됨
                    if (elapsedSeconds == 80) {
                        val idx = indexOfFirst { it.sensor == "sensor 01" }
                        if (idx != -1) this[idx] = this[idx].copy(
                            temperature   = (this[idx].temperature ?: 25.0) + 3.0,
                            isMinorChange = false
                        )
                        Log.d(TAG, "[시나리오] 80s: sensor 01 일반 변경 (화면 갱신 있어야 함)")
                    }

                }.toList()

                // ── C. 기능 2·3·4·5: 상태 판별 및 화면 업데이트 ─────────────────────
                for (i in internalState.indices) {
                    val raw       = internalState[i]
                    val displayed = sensorList[i]

                    // ── STEP 1: 기능 3 - null 체크로 현재 상태 결정 ──────────────────
                    val hasNull = raw.temperature == null || raw.humidity == null || raw.co2 == null
                    if (hasNull) {
                        // null 수신 → 연결 이상 표시 (화면 값은 마지막 정상값 유지)
                        if (displayed.status != SensorStatus.CONNECT_ERROR) {
                            sensorList[i] = displayed.copy(status = SensorStatus.CONNECT_ERROR)
                        }
                        continue // 이번 사이클은 값 갱신 없이 상태만 유지
                    }

                    // ── STEP 2: null 아님 → 실제 값 추출 ────────────────────────────
                    val tempVal = raw.temperature!! // hasNull 통과했으므로 non-null 보장
                    val humVal  = raw.humidity!!
                    val co2Val  = raw.co2!!

                    // ── STEP 3: 기능 2 - 범위 이상 여부 판별 ────────────────────────
                    // 습도 100% 지속 카운터: internalState 기준으로 누적
                    val prevHum100Sec = if (humVal >= 100.0) raw.humidity100Seconds else 0
                    val newHum100Sec  = if (humVal >= 100.0) prevHum100Sec + 1 else 0

                    val isDataError =
                        tempVal < 15.0 || tempVal > 45.0 ||
                                humVal  < 15.0 ||
                                newHum100Sec >= 5 ||
                                co2Val  < 380L || co2Val > 550L

                    // internalState에 최신 카운터 반영 (상태 판별용)
                    internalState = internalState.toMutableList().also { list ->
                        list[i] = raw.copy(humidity100Seconds = newHum100Sec)
                    }

                    if (isDataError) {
                        // 이상 데이터 → 화면 값은 마지막 정상값 유지, 상태 배너만 변경
                        if (displayed.status != SensorStatus.DATA_ERROR) {
                            sensorList[i] = displayed.copy(status = SensorStatus.DATA_ERROR)
                        }
                        continue // 이번 사이클은 값 갱신 없이 상태만 유지
                    }

                    // ── STEP 4: 여기까지 왔으면 현재 데이터는 "정상 범위" ────────────
                    // 기능 5: 미세 변경이면 값은 갱신하지 않되, 혹시 이전에 이상 상태였다면
                    //         정상 상태로만 복구해 준다.
                    if (raw.isMinorChange) {
                        if (displayed.status != SensorStatus.NORMAL) {
                            // 이상 → 정상 복구: 현재 정상 값으로 화면 업데이트
                            sensorList[i] = displayed.copy(
                                temperature        = Math.round(tempVal * 10.0) / 10.0,
                                humidity           = Math.round(humVal  * 10.0) / 10.0,
                                co2                = co2Val,
                                status             = SensorStatus.NORMAL,
                                humidity100Seconds = newHum100Sec
                            )
                        }
                        // 이미 정상 상태면 미세 변경이므로 화면 갱신 없이 그냥 통과
                        continue
                    }

                    // ── STEP 5: 일반 변경 + 정상 범위 → 임계값 초과 시 화면 갱신 ────
                    val tempDiff = Math.abs(tempVal - (displayed.temperature ?: 0.0))
                    val humDiff  = Math.abs(humVal  - (displayed.humidity    ?: 0.0))
                    val co2Diff  = Math.abs(co2Val  - (displayed.co2         ?: 0L).toDouble())

                    if (tempDiff >= TEMP_THRESHOLD || humDiff >= HUMIDITY_THRESHOLD || co2Diff >= CO2_THRESHOLD
                        || displayed.status != SensorStatus.NORMAL // 이상 상태에서 정상 복구 시 무조건 갱신
                    ) {
                        sensorList[i] = internalState[i].copy(
                            temperature        = Math.round(tempVal * 10.0) / 10.0,
                            humidity           = Math.round(humVal  * 10.0) / 10.0,
                            co2                = co2Val,
                            status             = SensorStatus.NORMAL,
                            humidity100Seconds = newHum100Sec
                        )
                    }
                }
            }
        }
    }


    // ─────────────────────────────────────────────────────────────────────────────
    // UI 컴포저블
    // ─────────────────────────────────────────────────────────────────────────────

    @Composable
    fun SensorDetailPopup(sensor: SensorData, onClose: () -> Unit) {

        // 기능 2·3: 상태에 따른 배너 색상 및 문구
        val (bannerColor, bannerText) = when (sensor.status) {
            SensorStatus.CONNECT_ERROR -> Color(0xFFEF5350) to "⚠️ 연결 이상"
            SensorStatus.DATA_ERROR    -> Color(0xFFFF9800) to "⚠️ 온도 이상"  // 넓게 "데이터 이상"으로 바꿔도 됩니다
            SensorStatus.NORMAL        -> null to null
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // 상태 배너 (이상 시만 표시)
                if (bannerColor != null && bannerText != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(bannerColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = bannerText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }

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
                            text = if (sensor.status == SensorStatus.CONNECT_ERROR) "- °C"
                            else "${sensor.temperature ?: "- "}°C",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "습도", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = if (sensor.status == SensorStatus.CONNECT_ERROR) "- %"
                            else "${sensor.humidity ?: "- "}%",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "CO2", fontSize = 12.sp, color = Color.Gray)
                        Text(
                            text = if (sensor.status == SensorStatus.CONNECT_ERROR) "- ppm"
                            else "${sensor.co2 ?: "- "} ppm",
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
    fun RestPlaceCard(sensor: SensorData, onClick: () -> Unit) {
        val temp     = sensor.temperature ?: 0.0
        val humidity = sensor.humidity ?: 0.0
        val congestionLevel = CongestionAnalyzer.analyze(temp, humidity)

        // 기능 2·3: 상태 텍스트 및 색상 결정
        val (statusText, statusColor) = when (sensor.status) {
            SensorStatus.CONNECT_ERROR -> "연결 이상" to Color(0xFFEF5350)
            SensorStatus.DATA_ERROR    -> "데이터 이상" to Color(0xFFFF9800)
            SensorStatus.NORMAL        -> null to null
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .border(1.dp, Color.Gray)
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("센서: ${sensor.sensor}")
                if (statusText != null) {
                    // 이상 상태: 기존 값 대신 상태 문구 표시
                    Text(
                        text = statusText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor ?: Color.Red
                    )
                } else {
                    Text("온도: ${sensor.temperature ?: "- "}°C")
                    Text("습도: ${sensor.humidity ?: "- "}%")
                }
            }

            // 혼잡도 (이상 상태여도 마지막으로 정상이었을 때 값 유지)
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
        Text(text = "Hello $name!", modifier = modifier)
    }

    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        Opensrc_better_breakTheme {
            Greeting("Android")
        }
    }
}