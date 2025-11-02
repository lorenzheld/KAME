package com.example.kame.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.kame.ui.theme.KAMETheme

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    Text(
        text = "Willkommen auf der Home-Seite! 🏠",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    KAMETheme {
        HomeScreen()
    }
}