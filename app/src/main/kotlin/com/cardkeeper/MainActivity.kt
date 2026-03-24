package com.cardkeeper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Label
import androidx.compose.material.icons.rounded.Style
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cardkeeper.ui.navigation.AppNavHost
import com.cardkeeper.ui.navigation.CardListRoute
import com.cardkeeper.ui.navigation.ScanFlowRoute
import com.cardkeeper.ui.navigation.TagManagerRoute
import com.cardkeeper.ui.theme.CardKeeperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CardKeeperTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = currentRoute == CardListRoute::class.qualifiedName,
                                onClick = {
                                    navController.navigate(CardListRoute) {
                                        popUpTo(CardListRoute) { inclusive = true }
                                    }
                                },
                                icon = { Icon(Icons.Rounded.Style, contentDescription = "Cards") },
                                label = { Text("Cards") }
                            )
                            NavigationBarItem(
                                selected = navBackStackEntry?.destination?.hierarchy?.any {
                                    it.hasRoute<ScanFlowRoute>()
                                } == true,
                                onClick = {
                                    navController.navigate(ScanFlowRoute) {
                                        popUpTo(CardListRoute)
                                    }
                                },
                                icon = { Icon(Icons.Rounded.CameraAlt, contentDescription = "Scan") },
                                label = { Text("Scan") }
                            )
                            NavigationBarItem(
                                selected = currentRoute == TagManagerRoute::class.qualifiedName,
                                onClick = {
                                    navController.navigate(TagManagerRoute) {
                                        popUpTo(CardListRoute)
                                    }
                                },
                                icon = { Icon(Icons.Rounded.Label, contentDescription = "Tags") },
                                label = { Text("Tags") }
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        AppNavHost(navController = navController)
                    }
                }
            }
        }
    }
}
