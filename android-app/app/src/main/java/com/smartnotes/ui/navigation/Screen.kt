package com.smartnotes.ui.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")

    data object Notes : Screen("notes")
    data object NoteEdit : Screen("note_edit/{noteId}") {
        fun createRoute(noteId: Long) = "note_edit/$noteId"
        fun createRoute() = "note_edit/0"
    }

    data object Vocabulary : Screen("vocabulary")
    data object WordBook : Screen("word_book/{bookId}") {
        fun createRoute(bookId: Long) = "word_book/$bookId"
    }

    data object Documents : Screen("documents")
    data object DocumentPreview : Screen("document_preview/{docId}") {
        fun createRoute(docId: Long) = "document_preview/$docId"
    }

    data object Settings : Screen("settings")
}
