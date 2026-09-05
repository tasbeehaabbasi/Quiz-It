package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.QuizDatabase
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.PastQuizReviewScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.LibraryViewModel
import com.example.ui.viewmodel.QuizViewModel
import com.example.ui.viewmodel.ThemeViewModel

@Composable
fun AppNavGraph(
    authViewModel: AuthViewModel = viewModel(),
    quizViewModel: QuizViewModel = viewModel(),
    libraryViewModel: LibraryViewModel = viewModel(),
    themeViewModel: ThemeViewModel = viewModel()
) {
    val navController = rememberNavController()
    val user by authViewModel.currentUser.collectAsState()

    NavHost(
        navController = navController,
        startDestination = if (user != null) "home" else "auth"
    ) {
        composable("auth") {
            AuthScreen(
                authViewModel = authViewModel,
                onSignInSuccess = {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            )
        }

        composable("home") {
            HomeScreen(
                quizViewModel = quizViewModel,
                authViewModel = authViewModel,
                themeViewModel = themeViewModel,
                onNavigateToQuiz = { navController.navigate("quiz") },
                onNavigateToLibrary = { navController.navigate("library") }
            )
        }
        
        composable("quiz") {
            QuizScreen(
                quizViewModel = quizViewModel,
                onNavigateToResults = { navController.navigate("results") {
                    popUpTo("quiz") { inclusive = true }
                } },
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable("results") {
            ResultsScreen(
                quizViewModel = quizViewModel,
                userId = user?.uid ?: "",
                onNavigateHome = {
                    quizViewModel.reset()
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = true }
                    }
                },
                onRetakeQuiz = {
                    navController.navigate("quiz") {
                        popUpTo("home")
                    }
                }
            )
        }
        
        composable("library") {
            LibraryScreen(
                libraryViewModel = libraryViewModel,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReview = { quizId -> navController.navigate("past_quiz_review/$quizId") }
            )
        }

        composable(
            "past_quiz_review/{quizId}",
            arguments = listOf(navArgument("quizId") { type = NavType.IntType })
        ) { backStackEntry ->
            val quizId = backStackEntry.arguments?.getInt("quizId") ?: 0
            val context = androidx.compose.ui.platform.LocalContext.current
            val database = QuizDatabase.getDatabase(context)
            PastQuizReviewScreen(
                quizId = quizId,
                database = database,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
