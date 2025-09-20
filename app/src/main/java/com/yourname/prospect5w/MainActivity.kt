package com.yourname.prospect5w

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.material3.icons.Icons
import androidx.compose.material3.icons.filled.Add
import androidx.compose.material3.icons.filled.Home
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.*
import com.yourname.prospect5w.data.ProspectDb
import com.yourname.prospect5w.domain.ProspectRepo
import com.yourname.prospect5w.notify.ReminderScheduler
import com.yourname.prospect5w.notify.ensureNotificationChannel
import com.yourname.prospect5w.ui.QuickAddScreen
import com.yourname.prospect5w.ui.TodayScreen

class MainActivity : ComponentActivity() {

    companion object {
        fun intent(ctx: android.content.Context) = Intent(ctx, MainActivity::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensureNotificationChannel(this)

        if (Build.VERSION.SDK_INT >= 33) {
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ).launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        val db = (application as App).db
        val repo = ProspectRepo(db.dao())
        val scheduler = ReminderScheduler(this)

        setContent {
            MaterialTheme {
                val nav = rememberNavController()
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = nav.currentDestination?.route == "today",
                                onClick = { nav.navigate("today") },
                                label = { Text("Today") },
                                icon = { Icon(Icons.Default.Home, null) }
                            )
                            NavigationBarItem(
                                selected = nav.currentDestination?.route == "add",
                                onClick = { nav.navigate("add") },
                                label = { Text("Quick Add") },
                                icon = { Icon(Icons.Default.Add, null) }
                            )
                        }
                    }
                ) { pad ->
                    NavHost(nav, startDestination = "today", modifier = Modifier.padding(pad)) {
                        composable("today") { TodayScreen(repo) }
                        composable("add") { QuickAddScreen(repo, scheduler) }
                    }
                }
            }
        }
    }
}
