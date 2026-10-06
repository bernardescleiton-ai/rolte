package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.viewmodel.RoletaViewModel
import com.example.ui.AdminScreen
import com.example.ui.RoletaPublicScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: RoletaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "roleta/outubro") {
                        composable(
                            route = "roleta/{slug}",
                            arguments = listOf(navArgument("slug") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val slug = backStackEntry.arguments?.getString("slug") ?: "outubro"
                            RoletaPublicScreen(
                                viewModel = viewModel,
                                slug = slug,
                                onNavigateAdmin = {
                                    navController.navigate("admin")
                                }
                            )
                        }
                        composable("admin") {
                            AdminScreen(
                                viewModel = viewModel,
                                onNavigatePublic = { slug ->
                                    navController.navigate("roleta/$slug")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
