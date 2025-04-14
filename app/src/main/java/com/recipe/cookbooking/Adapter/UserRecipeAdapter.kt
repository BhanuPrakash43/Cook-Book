package com.recipe.cookbooking.Adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.recipe.cookbooking.EditRecipeActivity
import com.recipe.cookbooking.Model.Recipe
import com.recipe.cookbooking.R
import com.recipe.cookbooking.SingleRecipeActivity

class UserRecipeAdapter(
    private val recipes: MutableList<Recipe>,
    private val onDelete: (Recipe) -> Unit
) : RecyclerView.Adapter<UserRecipeAdapter.RecipeViewHolder>() {

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val recipeImage: ImageView = itemView.findViewById(R.id.recipeImage)
        val category: TextView = itemView.findViewById(R.id.recipeCategory)
        val subCategory: TextView = itemView.findViewById(R.id.recipeSubCategory)
        val recipeTitle: TextView = itemView.findViewById(R.id.recipeTitle)
        val recipeAuthor: TextView = itemView.findViewById(R.id.recipeAuthor)
        val recipeRating: TextView = itemView.findViewById(R.id.recipeRating)
        val recipeTime: TextView = itemView.findViewById(R.id.recipeMakingTime)
        val moreIcon: ImageView = itemView.findViewById(R.id.moreIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.user_item_recipe_card, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        val context = holder.itemView.context

        holder.category.text = recipe.category
        holder.subCategory.text = recipe.subCategory
        holder.recipeTitle.text = recipe.name
        holder.recipeAuthor.text = "By ${recipe.authorName}"
        holder.recipeRating.text = String.format("%.1f", recipe.rating)
        holder.recipeTime.text = "${recipe.cookTime} Min"

        Glide.with(context)
            .load(recipe.recipeImage)
            .placeholder(R.drawable.sample_recipe_image)
            .into(holder.recipeImage)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, SingleRecipeActivity::class.java)
            intent.putExtra("recipe", recipe)
            context.startActivity(intent)
        }

        holder.moreIcon.setOnClickListener {
            val popup = PopupMenu(context, holder.moreIcon)
            popup.menuInflater.inflate(R.menu.recipe_options_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.menu_update -> {
                        val intent = Intent(context, EditRecipeActivity::class.java)
                        intent.putExtra("RECIPE_ID", recipe.id)
                        context.startActivity(intent)
                        true
                    }

                    R.id.menu_delete -> {
                        onDelete(recipe)
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }
    }

    override fun getItemCount(): Int = recipes.size

    fun removeItem(recipe: Recipe) {
        val position = recipes.indexOf(recipe)
        if (position != -1) {
            recipes.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
