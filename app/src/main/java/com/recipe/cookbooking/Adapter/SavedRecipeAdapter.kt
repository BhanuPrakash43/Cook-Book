package com.recipe.cookbooking.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.recipe.cookbooking.Model.Recipe
import com.recipe.cookbooking.R
import com.recipe.cookbooking.SingleRecipeActivity

class SavedRecipeAdapter(
    private val context: Context,
    private val recipeList: MutableList<Recipe>
) : RecyclerView.Adapter<SavedRecipeAdapter.SavedRecipeViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private val savedRecipesRef = FirebaseDatabase.getInstance().getReference("SavedRecipes")
        .child(currentUserId ?: "")

    inner class SavedRecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipeImage: ImageView = itemView.findViewById(R.id.recipeImage)
        val recipeTitle: TextView = itemView.findViewById(R.id.recipeTitle)
        val recipeCategory: TextView = itemView.findViewById(R.id.recipeCategory)
        val recipeSubCategory: TextView = itemView.findViewById(R.id.recipeSubCategory)
        val recipeAuthor: TextView = itemView.findViewById(R.id.recipeAuthor)
        val recipeRating: TextView = itemView.findViewById(R.id.recipeRating)
        val recipeTime: TextView = itemView.findViewById(R.id.recipeMakingTime)
        val saveIcon: ImageView = itemView.findViewById(R.id.saveRecipeIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedRecipeViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_saved_recipe, parent, false)
        return SavedRecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: SavedRecipeViewHolder, position: Int) {
        val recipe = recipeList[position]

        Glide.with(context).load(recipe.recipeImage)
            .placeholder(R.drawable.sample_recipe_image)
            .into(holder.recipeImage)

        holder.recipeTitle.text = recipe.name
        holder.recipeCategory.text = recipe.category
        holder.recipeSubCategory.text = recipe.subCategory
        holder.recipeAuthor.text = "By ${recipe.authorName}"
        holder.recipeRating.text = recipe.rating.toString()
        holder.recipeTime.text = "${recipe.cookTime} Min"

        holder.itemView.setOnClickListener {
            val intent = Intent(context, SingleRecipeActivity::class.java)
            intent.putExtra("recipe", recipe)
            context.startActivity(intent)
        }

        holder.saveIcon.setImageResource(R.drawable.redsavesign)

        holder.saveIcon.setOnClickListener {
            removeFromSaved(recipe, position)
        }
    }

    private fun removeFromSaved(recipe: Recipe, position: Int) {
        if (currentUserId == null || position < 0 || position >= recipeList.size) return

        savedRecipesRef.child(recipe.id).removeValue()
            .addOnSuccessListener {
                if (position < recipeList.size && recipeList[position].id == recipe.id) {
                    recipeList.removeAt(position)
                    notifyItemRemoved(position)
                } else {
                    val index = recipeList.indexOfFirst { it.id == recipe.id }
                    if (index != -1) {
                        recipeList.removeAt(index)
                        notifyItemRemoved(index)
                    }
                }

                Toast.makeText(context, "Recipe removed from saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to remove recipe", Toast.LENGTH_SHORT).show()
            }
    }


    override fun getItemCount(): Int = recipeList.size
}
