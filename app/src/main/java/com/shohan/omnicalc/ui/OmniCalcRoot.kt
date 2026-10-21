package com.shohan.omnicalc.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.shohan.omnicalc.core.common.AppSettings
import com.shohan.omnicalc.core.common.LanguageMode
import com.shohan.omnicalc.feature.calculator.CalculatorScreen
import com.shohan.omnicalc.feature.library.LibraryScreen
import com.shohan.omnicalc.feature.scientific.ScientificScreen
import com.shohan.omnicalc.R

/**
 * App shell: five persistent bottom-navigation destinations (spec §2.5) plus
 * global search and About routes.
 */
private data class Tab(val route: String, val labelRes: Int, val icon: @Composable () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OmniCalcRoot(settings: AppSettings, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val tabs = listOf(
        Tab("calculate", R.string.nav_calculate) { Icon(Icons.Filled.Calculate, contentDescription = null) },
        Tab("scientific", R.string.nav_scientific) { Icon(Icons.Filled.Science, contentDescription = null) },
        Tab("formulas", R.string.nav_formulas) { Icon(Icons.Filled.Functions, contentDescription = null) },
        Tab("tools", R.string.nav_tools) { Icon(Icons.Filled.Construction, contentDescription = null) },
        Tab("library", R.string.nav_library) { Icon(Icons.Filled.MenuBook, contentDescription = null) },
    )

    // Expression handed Library → Calculator (shared at shell level so tab
    // switches never lose it).
    var pendingExpression by rememberSaveable { mutableStateOf<String?>(null) }

    val onReuse: (String, String?) -> Unit = { expr, _ ->
        pendingExpression = expr
        navController.navigate("calculate") {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            val title = when (currentRoute) {
                "calculate" -> stringResource(R.string.nav_calculate)
                "scientific" -> stringResource(R.string.nav_scientific)
                "formulas" -> stringResource(R.string.nav_formulas)
                "tools" -> stringResource(R.string.nav_tools)
                "library" -> stringResource(R.string.nav_library)
                "search" -> stringResource(R.string.search_title)
                "about" -> stringResource(R.string.about_title)
                else -> stringResource(R.string.app_name)
            }
            TopAppBar(
                title = { Text(title, style = MaterialTheme.typography.titleLarge) },
                actions = {
                    if (currentRoute != "search") {
                        IconButton(onClick = { navController.navigate("search") }) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(R.string.search_title),
                            )
                        }
                    }
                },
            )
        },
        bottomBar = {
            if (currentRoute in tabs.map { it.route }) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    restoreState = true
                                    launchSingleTop = true
                                }
                            },
                            icon = tab.icon,
                            label = { Text(stringResource(tab.labelRes)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "calculate",
            modifier = Modifier.padding(padding),
        ) {
            composable("calculate") {
                CalculatorScreen(
                    pendingExpression = pendingExpression,
                    onExpressionConsumed = { pendingExpression = null },
                )
            }
            composable("scientific") {
                ScientificScreen(
                    pendingExpression = pendingExpression,
                    onExpressionConsumed = { pendingExpression = null },
                )
            }
            composable("formulas") { PlaceholderFormulas() }
            composable("tools") { PlaceholderTools() }
            composable("library") {
                LibraryScreen(
                    onOpenAbout = { navController.navigate("about") },
                    onReuseExpression = onReuse,
                    onLanguageChanged = { _: LanguageMode -> /* applied via settings effect in MainActivity */ },
                )
            }
            composable("search") {
                SearchScreen(
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            restoreState = true
                            launchSingleTop = true
                        }
                    },
                    onBack = { navController.popBackStack() },
                )
            }
            composable("about") {
                AboutScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
