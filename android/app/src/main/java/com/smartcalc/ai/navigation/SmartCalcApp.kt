package com.smartcalc.ai.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartcalc.ai.R
import com.smartcalc.ai.ui.calculator.CalculatorScreen
import com.smartcalc.ai.ui.history.HistoryScreen
import com.smartcalc.ai.ui.solver.SolverScreen

@Composable
fun SmartCalcApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute == Routes.CALCULATOR || currentRoute == Routes.HISTORY

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = showBottomBar) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Routes.CALCULATOR,
                        onClick = {
                            if (currentRoute != Routes.CALCULATOR) {
                                navController.navigate(Routes.CALCULATOR) {
                                    popUpTo(Routes.CALCULATOR) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Outlined.Calculate, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_calculator)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Routes.HISTORY,
                        onClick = {
                            if (currentRoute != Routes.HISTORY) {
                                navController.navigate(Routes.HISTORY) {
                                    launchSingleTop = true
                                }
                            }
                        },
                        icon = { Icon(Icons.Outlined.History, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_history)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = Routes.CALCULATOR
            ) {
                composable(Routes.CALCULATOR) {
                    CalculatorScreen(
                        onOpenCamera = { navController.navigate(Routes.solver(SolverSource.CAMERA)) },
                        onOpenGallery = { navController.navigate(Routes.solver(SolverSource.GALLERY)) }
                    )
                }
                composable(Routes.HISTORY) {
                    HistoryScreen(
                        onReuseExpression = {
                            navController.navigate(Routes.CALCULATOR) {
                                popUpTo(Routes.CALCULATOR) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
                composable(
                    route = Routes.SOLVER,
                    arguments = listOf(
                        navArgument("source") {
                            type = NavType.StringType
                            defaultValue = SolverSource.CAMERA.value
                        }
                    )
                ) { entry ->
                    val source = SolverSource.from(entry.arguments?.getString("source"))
                    SolverScreen(
                        source = source,
                        onClose = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
