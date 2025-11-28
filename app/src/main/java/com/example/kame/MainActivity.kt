@file:Suppress("NonAsciiCharacters")

package com.example.kame

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.lifecycleScope
import com.example.kame.data.SampleData
import com.example.kame.data.database.WorkoutDatabase
import com.example.kame.ui.theme.KAMETheme
import com.example.kame.screens.HomeScreen
import com.example.kame.screens.WorkoutsScreen
import com.example.kame.screens.DiagrammeScreen
import com.example.kame.screens.ProfilScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = WorkoutDatabase.getDatabase(this)
        val workoutDao = database.workoutDao()

        SampleData.insertSampleDataIfNeeded(this, workoutDao, lifecycleScope)

        setContent {
            KAMETheme {
                KAMEApp()
            }
        }
    }
}

@PreviewScreenSizes
@Composable
fun KAMEApp() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        if (it.iconVector != null) {
                            Icon(
                                imageVector = it.iconVector,
                                contentDescription = it.label
                            )
                        } else if (it.iconDrawable != null) {
                            Icon(
                                painter = painterResource(id = it.iconDrawable),
                                contentDescription = it.label
                            )
                        }
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            when (currentDestination) {
                AppDestinations.HOME -> HomeScreen(
                    modifier = Modifier.padding(innerPadding),
                    onNavigateToWorkouts = { currentDestination = AppDestinations.WORKOUTS } // FIX: Navigation übergeben
                )
                AppDestinations.WORKOUTS -> WorkoutsScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.DIAGRAMME -> DiagrammeScreen(modifier = Modifier.padding(innerPadding))
                AppDestinations.PROFIL -> ProfilScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val iconVector: ImageVector? = null,
    @DrawableRes val iconDrawable: Int? = null,
) {
    HOME("Home", iconVector = Icons.Default.Home),
    WORKOUTS("Workouts", iconDrawable = R.drawable.ic_workout),
    DIAGRAMME("Diagramme", iconDrawable = R.drawable.ic_bar_chart),
    PROFIL("Profil", iconVector = Icons.Default.AccountBox),
}