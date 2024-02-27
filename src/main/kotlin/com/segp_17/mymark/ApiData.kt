package com.segp_17.mymark

// Response of fetching exercises
data class Exercises(
    val exercises: Array<String>
)

// Response of fetching modules
data class Modules(
    val modules: Array<String>
)

// Request for asking a question
data class QuestionData(
    val code: String,
    val context: List<Message>
)

// OpenAI message format
data class Message(
    val role: String,
    val content: String
)

// Response of asking a question
data class Answer(
    val answer: String
)