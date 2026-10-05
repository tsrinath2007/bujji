package com.pupil.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pupil.app.ui.components.PupilBottomNavBar
import com.pupil.app.ui.components.PupilTab
import com.pupil.app.ui.screens.graph.GraphScreen
import com.pupil.app.ui.screens.graph.GraphViewModel
import com.pupil.app.ui.screens.home.HomeScreen
import com.pupil.app.ui.screens.home.HomeViewModel
import com.pupil.app.ui.screens.import_material.ImportScreen
import com.pupil.app.ui.screens.import_material.ImportViewModel
import com.pupil.app.ui.screens.library.LibraryScreen
import com.pupil.app.ui.screens.library.LibraryViewModel
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.remember
import com.pupil.app.core.prefs.OnboardingPreferences
import com.pupil.app.ui.screens.onboarding.OnboardingScreen
import com.pupil.app.ui.screens.result.ResultScreen
import com.pupil.app.ui.screens.result.ResultViewModel
import com.pupil.app.ui.screens.selection.ConceptSelectionScreen
import com.pupil.app.ui.screens.selection.ConceptSelectionViewModel
import com.pupil.app.ui.screens.settings.SettingsScreen
import com.pupil.app.ui.screens.settings.SettingsViewModel
import com.pupil.app.ui.screens.teach.TeachScreen
import com.pupil.app.ui.screens.teach.TeachViewModel

object Routes {
    const val LOGIN = "login"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val LIBRARY = "library"
    const val SETTINGS = "settings"
    const val IMPORT = "import"

    // Subject-based Clean Notebook routes
    const val SELECTION = "selection/{subjectId}"
    const val TEACH = "teach/{subjectId}/{topicId}?gaps={gaps}"
    const val RESULT = "result/{subjectId}/{topicId}"
    const val GRAPH = "graph/{subjectId}?conceptId={conceptId}"

    fun selection(subjectId: String) = "selection/${encode(subjectId)}"
    fun teach(subjectId: String, topicId: String = "ALL", gaps: Boolean = false) =
        "teach/${encode(subjectId)}/${encode(topicId)}?gaps=$gaps"
    fun result(subjectId: String, topicId: String = "ALL") =
        "result/${encode(subjectId)}/${encode(topicId)}"
    fun graph(subjectId: String, conceptId: String? = null) =
        if (conceptId != null) "graph/${encode(subjectId)}?conceptId=${encode(conceptId)}"
        else "graph/${encode(subjectId)}"

    private fun encode(s: String) = java.net.URLEncoder.encode(s, "UTF-8")
}

@Composable
fun PupilNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val onboardingPrefs = remember { OnboardingPreferences(context) }
    val startDestination = when {
        !onboardingPrefs.hasCompletedOnboarding -> Routes.ONBOARDING
        !onboardingPrefs.isAuthenticated -> Routes.LOGIN
        else -> Routes.HOME
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Show bottom bar ONLY on top-level tabs: Home, Library, Settings
    val showBottomBar = currentRoute in listOf(Routes.HOME, Routes.LIBRARY, Routes.SETTINGS)

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                PupilBottomNavBar(
                    currentRoute = currentRoute,
                    onTabSelected = { tab ->
                        val destinationRoute = when (tab) {
                            PupilTab.HOME -> Routes.HOME
                            PupilTab.LIBRARY -> Routes.LIBRARY
                            PupilTab.SETTINGS -> Routes.SETTINGS
                        }

                        navController.navigate(destinationRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = modifier.padding(innerPadding)
        ) {
            // Screen 0: Onboarding
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinishOnboarding = {
                        onboardingPrefs.hasCompletedOnboarding = true
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            // Screen 1: Google Authentication / Login
            composable(Routes.LOGIN) {
                com.pupil.app.ui.screens.auth.LoginScreen(
                    onLoginSuccess = { name, email ->
                        onboardingPrefs.userName = name
                        onboardingPrefs.userEmail = email
                        onboardingPrefs.isAuthenticated = true
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onSkip = {
                        onboardingPrefs.isAuthenticated = true
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }

            // Screen 2: Home
            composable(Routes.HOME) {
                val homeViewModel: HomeViewModel = viewModel()
                HomeScreen(
                    viewModel = homeViewModel,
                    onTeachNowClick = { subjectId ->
                        if (subjectId != null) {
                            navController.navigate(Routes.selection(subjectId))
                        } else {
                            navController.navigate(Routes.LIBRARY)
                        }
                    },
                    onOpenGraphClick = { subjectId ->
                        if (subjectId != null) {
                            navController.navigate(Routes.graph(subjectId))
                        } else {
                            navController.navigate(Routes.LIBRARY)
                        }
                    },
                    onOpenLibraryClick = {
                        navController.navigate(Routes.LIBRARY)
                    },
                    onImportClick = {
                        navController.navigate(Routes.IMPORT)
                    },
                    onOpenSettingsClick = {
                        navController.navigate(Routes.SETTINGS)
                    }
                )
            }

            // Screen 3: Library (My Subjects)
            composable(Routes.LIBRARY) {
                val libraryViewModel: LibraryViewModel = viewModel()
                LibraryScreen(
                    viewModel = libraryViewModel,
                    onOpenSubjectClick = { subjectId ->
                        navController.navigate(Routes.selection(subjectId))
                    },
                    onOpenGraphClick = { subjectId ->
                        navController.navigate(Routes.graph(subjectId))
                    },
                    onImportNewClick = {
                        navController.navigate(Routes.IMPORT)
                    }
                )
            }

            // Screen 4: Subject Screen (Sources, Topics, Brain Map tabs)
            composable(
                route = Routes.SELECTION,
                arguments = listOf(
                    navArgument("subjectId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val subjectId = decode(backStackEntry.arguments?.getString("subjectId") ?: "")
                val selectionViewModel: ConceptSelectionViewModel = viewModel()

                ConceptSelectionScreen(
                    subjectId = subjectId,
                    viewModel = selectionViewModel,
                    onTopicSelected = { topicId ->
                        navController.navigate(Routes.teach(subjectId, topicId))
                    },
                    onTeachAll = {
                        navController.navigate(Routes.teach(subjectId, "ALL"))
                    },
                    onOpenGraphClick = {
                        navController.navigate(Routes.graph(subjectId))
                    },
                    onAddSourceClick = {
                        navController.navigate(Routes.IMPORT)
                    },
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            }

            // Add/Import Material
            composable(Routes.IMPORT) {
                val importViewModel: ImportViewModel = viewModel()
                ImportScreen(
                    viewModel = importViewModel,
                    onSubjectReady = { subjectId ->
                        navController.navigate(Routes.selection(subjectId)) {
                            popUpTo(Routes.HOME)
                        }
                    }
                )
            }

            // Screen 5: Teaching
            composable(
                route = Routes.TEACH,
                arguments = listOf(
                    navArgument("subjectId") { type = NavType.StringType },
                    navArgument("topicId") { type = NavType.StringType },
                    navArgument("gaps") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val subjectId = decode(backStackEntry.arguments?.getString("subjectId") ?: "")
                val topicId = decode(backStackEntry.arguments?.getString("topicId") ?: "ALL")
                val onlyGaps = backStackEntry.arguments?.getBoolean("gaps") ?: false
                val teachViewModel: TeachViewModel = viewModel()

                TeachScreen(
                    subjectId = subjectId,
                    topicId = if (topicId == "ALL") null else topicId,
                    viewModel = teachViewModel,
                    onlyGaps = onlyGaps,
                    onGradingFinished = { sId, tId ->
                        navController.navigate(Routes.result(sId, tId ?: "ALL")) {
                            popUpTo(Routes.teach(sId, topicId, onlyGaps)) { inclusive = true }
                        }
                    }
                )
            }

            // Screen 6: Summary / Result
            composable(
                route = Routes.RESULT,
                arguments = listOf(
                    navArgument("subjectId") { type = NavType.StringType },
                    navArgument("topicId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val subjectId = decode(backStackEntry.arguments?.getString("subjectId") ?: "")
                val topicId = decode(backStackEntry.arguments?.getString("topicId") ?: "ALL")
                val resultViewModel: ResultViewModel = viewModel()

                ResultScreen(
                    subjectId = subjectId,
                    topicId = if (topicId == "ALL") null else topicId,
                    viewModel = resultViewModel,
                    onReteachClick = {
                        navController.navigate(Routes.teach(subjectId, topicId)) {
                            popUpTo(Routes.result(subjectId, topicId)) { inclusive = true }
                        }
                    },
                    onBackToTopicsClick = {
                        navController.navigate(Routes.selection(subjectId)) {
                            popUpTo(Routes.selection(subjectId)) { inclusive = true }
                        }
                    },
                    onTeachGapsClick = {
                        navController.navigate(Routes.teach(subjectId, topicId, gaps = true))
                    },
                    onOpenGraphClick = { cid ->
                        navController.navigate(Routes.graph(subjectId, cid))
                    }
                )
            }

            // Screen 7: Brain Map
            composable(
                route = Routes.GRAPH,
                arguments = listOf(
                    navArgument("subjectId") { type = NavType.StringType },
                    navArgument("conceptId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val subjectId = decode(backStackEntry.arguments?.getString("subjectId") ?: "")
                val conceptId = backStackEntry.arguments?.getString("conceptId")?.let { decode(it) }
                val graphViewModel: GraphViewModel = viewModel()

                GraphScreen(
                    subjectId = subjectId,
                    initialSelectedConceptId = conceptId,
                    viewModel = graphViewModel,
                    onBackClick = { navController.popBackStack() },
                    onTeachGapsClick = { sId ->
                        navController.navigate(Routes.teach(sId, "ALL", gaps = true))
                    }
                )
            }

            // Screen 8: Settings
            composable(Routes.SETTINGS) {
                val settingsViewModel: SettingsViewModel = viewModel()
                SettingsScreen(
                    viewModel = settingsViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onSignOut = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

private fun decode(s: String): String = try {
    java.net.URLDecoder.decode(s, "UTF-8")
} catch (_: Exception) { s }
