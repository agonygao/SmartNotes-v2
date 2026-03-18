package com.smartnotes.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.smartnotes.ui.screens.auth.LoginScreen
import com.smartnotes.ui.screens.auth.RegisterScreen
import com.smartnotes.ui.screens.notes.NoteEditScreen
import com.smartnotes.ui.screens.notes.NoteListScreen
import com.smartnotes.ui.screens.vocabulary.WordBookDetailScreen
import com.smartnotes.ui.screens.vocabulary.WordBookListScreen
import com.smartnotes.ui.screens.vocabulary.WordReviewScreen
import com.smartnotes.ui.screens.vocabulary.DictationScreen
import com.smartnotes.ui.screens.documents.DocumentListScreen
import com.smartnotes.ui.screens.documents.DocumentPreviewScreen
import com.smartnotes.ui.screens.documents.DocumentUploadScreen
import com.smartnotes.ui.screens.settings.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Notes.route
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Notes.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                },
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Notes.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Notes.route) {
            NoteListScreen(
                onNavigateToNoteEdit = { noteId ->
                    if (noteId != null) {
                        navController.navigate(Screen.NoteEdit.createRoute(noteId))
                    } else {
                        navController.navigate(Screen.NoteEdit.createRoute())
                    }
                },
            )
        }

        composable(
            route = Screen.NoteEdit.route,
            arguments = listOf(
                navArgument("noteId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val noteId = backStackEntry.arguments?.getLong("noteId")
            NoteEditScreen(
                noteId = noteId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Vocabulary.route) {
            WordBookListScreen(
                onNavigateToWordBookDetail = { bookId ->
                    navController.navigate(Screen.WordBook.createRoute(bookId))
                },
            )
        }

        composable(
            route = Screen.WordBook.route,
            arguments = listOf(
                navArgument("bookId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
            WordBookDetailScreen(
                bookId = bookId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToReview = { id ->
                    navController.navigate("review/$id")
                },
                onNavigateToDictation = { id ->
                    navController.navigate("dictation/$id")
                },
            )
        }

        composable(
            route = "review/{bookId}",
            arguments = listOf(
                navArgument("bookId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
            WordReviewScreen(
                bookId = bookId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = "dictation/{bookId}",
            arguments = listOf(
                navArgument("bookId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val bookId = backStackEntry.arguments?.getLong("bookId") ?: 0L
            DictationScreen(
                bookId = bookId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Documents.route) {
            DocumentListScreen(
                onNavigateToUpload = {
                    navController.navigate("document_upload")
                },
                onNavigateToPreview = { docId ->
                    navController.navigate(Screen.DocumentPreview.createRoute(docId))
                },
            )
        }

        composable("document_upload") {
            DocumentUploadScreen(
                onNavigateBack = { navController.popBackStack() },
                onUploadSuccess = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = Screen.DocumentPreview.route,
            arguments = listOf(
                navArgument("docId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getLong("docId") ?: 0L
            DocumentPreviewScreen(
                docId = docId,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
