package com.aistudio.wanas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { WansApp() } }
}

@Composable
private fun WansApp() {
    MaterialTheme { Surface(Modifier.fillMaxSize()) { Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
        Text("Wans", style=MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(12.dp))
        Text("إعادة بناء Wans — الإصدار 3.1")
        Spacer(Modifier.height(20.dp))
        Text("تم تجهيز الأساس لاستعادة الغرف والمزامنة الحية.")
    } } }
}
