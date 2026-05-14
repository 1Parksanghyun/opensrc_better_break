package com.example.opensrc_better_break

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.opensrc_better_break.ui.theme.Opensrc_better_breakTheme
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = FirebaseFirestore.getInstance()

        val testSensorData = hashMapOf(
            "location" to "테스트 구역",
            "temperature" to 22.5,
            "humidity" to 55.0,
            "message" to "파이어베이스 연결 성공!"
        )

        db.collection("test_sensors")
            .add(testSensorData)
            .addOnSuccessListener { documentReference ->
                // 성공하면 하단 Logcat에 초록색으로 출력됨
                Log.d("FirebaseTest", "데이터 저장 성공! 문서 ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                // 실패하면 빨간색으로 출력됨
                Log.w("FirebaseTest", "데이터 저장 실패...", e)
            }

        setContent {
            Opensrc_better_breakTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
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