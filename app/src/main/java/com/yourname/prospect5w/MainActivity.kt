package com.yourname.prospect5w

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yourname.prospect5w.domain.ProspectRepo
import com.yourname.prospect5w.notify.ReminderScheduler
import com.yourname.prospect5w.notify.ensureNotificationChannel
import com.yourname.prospect5w.ui.EventDetailScreen
import com.yourname.prospect5w.ui.EventsScreen
import com.yourname.prospect5w.ui.QuickAddScreen
import com.yourname.prospect5w.ui.TodayScreen

class MainActivity : ComponentActivity() {

    companion object {
        fun intent(ctx: android.content.Context) = Intent(ctx, MainActivity::class.java)
    }

    private lateinit var notifPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureNotificationChannel(this)

        notifPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* handle result if needed */ }

        if (Build.VERSION.SDK_INT >= 33) {
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val db = (application as App).db
        val repo = ProspectRepo(db.dao())
        val scheduler = ReminderScheduler(this)

        setContent {
            AppScaffold(repo = repo, scheduler = scheduler)
        }
    }
}

@Composable
private fun AppScaffold(repo: ProspectRepo, scheduler: ReminderScheduler) {
    MaterialTheme {
        val nav = rememberNavController()
        val backStackEntry by nav.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route

        Scaffold(
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == "today",
                        onClick = { nav.navigate("today") },
                        label = { Text("Today") },
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Today") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "add",
                        onClick = { nav.navigate("add") },
                        label = { Text("Quick Add") },
                        icon = { Icon(Icons.Filled.Add, contentDescription = "Quick Add") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "events",
                        onClick = { nav.navigate("events") },
                        label = { Text("Events") },
                        icon = { Icon(Icons.Filled.List, contentDescription = "Events") }
                    )
                }
            }
        ) { pad ->
            NavHost(navController = nav, startDestination = "today", modifier = Modifier.padding(pad)) {
                composable("today") { TodayScreen(repo) }
                composable("add") { QuickAddScreen(repo, scheduler) }
                composable("events") { EventsScreen(repo, onOpen = { id -> nav.navigate("event/$id") }) }
                composable("event/{id}") { backStackEntry ->
                    val id = backStackEntry.arguments?.getString("id")?.toLongOrNull() ?: return@composable
                    EventDetailScreen(id = id, repo = repo, onDeleted = { nav.popBackStack() })
                }
            }
        }
    }
}