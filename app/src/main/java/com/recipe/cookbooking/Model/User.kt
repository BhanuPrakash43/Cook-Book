package com.recipe.cookbooking.Model

data class User(
    val userId: String = "",
    val fullName: String = "",
    val email: String = "",
    val profilePic: String = "",
    val bio: String = "",
    val profession: String = "",
    val followers: Any? = null,
    val following: Any? = null
)

