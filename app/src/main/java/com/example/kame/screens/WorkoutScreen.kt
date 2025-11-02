package com.example.kame.screens

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.kame.ui.theme.KAMETheme

@Composable
fun WorkoutsScreen(modifier: Modifier = Modifier) {
    Text(
        text = "Hier sind deine Workouts! 💪",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun WorkoutsScreenPreview() {
    KAMETheme {
        WorkoutsScreen()
    }
}