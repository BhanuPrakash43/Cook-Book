package com.recipe.cookbooking.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.recipe.cookbooking.Model.Recipe
import com.recipe.cookbooking.R
import com.recipe.cookbooking.SingleRecipeActivity

class NewRecipeAdapter(private var recipeList: List<Recipe>, private val context: Context) :
    RecyclerView.Adapter<NewRecipeAdapter.NewRecipeViewHolder>() {

    class NewRecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRecipeName: TextView = itemView.findViewById(R.id.new_recipe_name)
        val tvCookTime: TextView = itemView.findViewById(R.id.tv_new_time)
        val tvRating: TextView = itemView.findViewById(R.id.new_rating)
        val tvAuthorName: TextView = itemView.findViewById(R.id.new_recipe_author)
        val imgRecipe: ImageView = itemView.findViewById(R.id.img_recipe)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewRecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.new_recipe_card, parent, false)
        return NewRecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: NewRecipeViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.tvRecipeName.text = recipe.name
        holder.tvCookTime.text = "${recipe.cookTime} Min"
        holder.tvRating.text = String.format("%.1f", recipe.rating)
        holder.tvAuthorName.text = "By ${recipe.authorName}"

        // Load image from URL using Glide
        Glide.with(holder.itemView.context)
            .load(recipe.recipeImage)
            .placeholder(R.drawable.splash_background)
            .error(R.drawable.splash_background)
            .into(holder.imgRecipe)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, SingleRecipeActivity::class.java)
            intent.putExtra("recipe", recipe)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = recipeList.size

    // Function to update the recipe list dynamically
    fun updateRecipes(newRecipes: List<Recipe>) {
        recipeList = newRecipes
        notifyDataSetChanged()
    }
}

