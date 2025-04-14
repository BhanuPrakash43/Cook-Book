package com.recipe.cookbooking.Model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Recipe(
    var id: String = "",
    var name: String = "",
    var category: String = "",
    var subCategory: String = "",
    var cookTime: String = "",
    var rating: Float = 0.0f,
    var recipeImage: String = "",
    var authorId: String = "",
    var authorName: String = "",
    var authorImage: String = "",
    var ingredients: MutableList<String> = mutableListOf(),
    var procedures: MutableList<String> = mutableListOf(),
    var likedBy: MutableList<String> = mutableListOf(),
    var likeCount: Int = 0,
    var isSaved: Boolean = false,
    var timestamp: Long = System.currentTimeMillis()
) : Parcelable
