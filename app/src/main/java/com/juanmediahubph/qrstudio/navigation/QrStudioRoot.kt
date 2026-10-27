package com.juanmediahubph.qrstudio.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.juanmediahubph.qrstudio.R
import com.juanmediahubph.qrstudio.feature.create.CreateScreen
import com.juanmediahubph.qrstudio.feature.generator.GeneratorScreen
import com.juanmediahubph.qrstudio.feature.history.HistoryScreen
import com.juanmediahubph.qrstudio.feature.settings.SettingsScreen

private data class TopLevelDestination(
    val route: String,
    @StringRes val labelRes: Int,
    @DrawableRes val iconRes: Int,
)

private val topLevelDestinations = listOf(
    TopLevelDestination(Destinations.CREATE, R.string.nav_create, R.drawable.ic_action_create),
    TopLevelDestination(Destinations.HISTORY, R.string.nav_history, R.drawable.ic_action_history),
    TopLevelDestination(Destinations.SETTINGS, R.string.nav_settings, R.drawable.ic_action_settings),
)

@Composable
fun QrStudioRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = topLevelDestinations.any { it.route == currentRoute }

    BoxWithConstraints {
        val wide = maxWidth >= 840.dp
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isTopLevel && !wide) {
                    NavigationBar {
                        topLevelDestinations.forEach { destination ->
                            val selected = currentRoute == destination.route
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateTopLevel(navController, destination.route) },
                                icon = {
                                    Icon(
                                        painter = painterResource(destination.iconRes),
                                        contentDescription = null,
                                    )
                                },
                                label = { Text(stringResource(destination.labelRes)) },
                            )
                        }
                    }
                }
            },
        ) { padding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                if (isTopLevel && wide) {
                    NavigationRail(
                        modifier = Modifier.statusBarsPadding(),
                        header = {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .padding(bottom = 8.dp)
                                    .size(40.dp),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_launcher_foreground),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(9.dp),
                                )
                            }
                        },
                    ) {
                        topLevelDestinations.forEach { destination ->
                            val selected = currentRoute == destination.route
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTopLevel(navController, destination.route) },
                                icon = {
                                    Icon(
                                        painter = painterResource(destination.iconRes),
                                        contentDescription = null,
                                    )
                                },
                                label = { Text(stringResource(destination.labelRes)) },
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = Destinations.CREATE,
                    modifier = Modifier
                        .fillMaxSize()
                        .then(if (wide) Modifier.navigationBarsPadding() else Modifier),
                ) {
                    composable(Destinations.CREATE) {
                        CreateScreen(
                            onTypeSelected = { type ->
                                navController.navigate(Destinations.generator(type))
                            },
                        )
                    }
                    composable(Destinations.HISTORY) {
                        HistoryScreen(
                            onOpen = { type, id ->
                                navController.navigate(Destinations.generator(type, id))
                            },
                            onCreate = {
                                navigateTopLevel(navController, Destinations.CREATE)
                            },
                        )
                    }
                    composable(Destinations.SETTINGS) {
                        SettingsScreen()
                    }
                    composable(
                        route = Destinations.GENERATOR_ROUTE,
                        arguments = listOf(
                            navArgument(Destinations.TYPE_ARG) { type = NavType.StringType },
                            navArgument(Destinations.ID_ARG) {
                                type = NavType.LongType
                                defaultValue = 0L
                            },
                        ),
                    ) {
                        GeneratorScreen(onBack = { navController.popBackStack() })
                    }
                }
            }
        }
    }
}

private fun navigateTopLevel(navController: NavHostController, route: String) {
    navController.navigate(route) {
        popUpTo(navController.graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
