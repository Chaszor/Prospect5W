package com.yourname.prospect5w

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yourname.prospect5w.ui.EventsScreen
import com.yourname.prospect5w.ui.QuickAddScreen
import com.yourname.prospect5w.ui.TodayScreen
import com.yourname.prospect5w.ui.theme.AppTheme

class MainActivity : ComponentActivity() {
    private val vm: EventViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                AppScaffold(vm)
            }
        }
    }
}

private enum class Dest(val route: String, val label: String) {
    Today("today", "Today"),
    Events("events", "All"),
    Add("add", "Add")
}

@Composable
private fun AppScaffold(vm: EventViewModel) {
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val currentDest: NavDestination? = backStack?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                listOf(Dest.Today, Dest.Events, Dest.Add).forEach { d ->
                    val selected = currentDest?.route == d.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(d.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        label = { Text(d.label) },
                        // Simple bullet instead of icons dependency
                        icon = { Text(if (selected) "●" else "○") }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = nav,
            startDestination = Dest.Today.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Dest.Today.route) { TodayScreen(vm) }
            composable(Dest.Events.route) { EventsScreen(vm) }
            composable(Dest.Add.route) { QuickAddScreen(vm, onSaved = { nav.navigate(Dest.Events.route) }) }
        }
    }
}
