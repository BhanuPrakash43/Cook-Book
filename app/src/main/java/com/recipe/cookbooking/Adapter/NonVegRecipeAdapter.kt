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

class NonVegRecipeAdapter(private var recipeList: List<Recipe>, private val context: Context) :
    RecyclerView.Adapter<NonVegRecipeAdapter.NonVegRecipeViewHolder>() {

    class NonVegRecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRecipeName: TextView = itemView.findViewById(R.id.tvRecipeName)
        val ivRecipeImage: ImageView = itemView.findViewById(R.id.ivRecipeImage)
        val tvNonVegLabel: TextView = itemView.findViewById(R.id.tvNonVegLabel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NonVegRecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_nonveg_recipe, parent, false)
        return NonVegRecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: NonVegRecipeViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.tvRecipeName.text = recipe.name
        holder.tvNonVegLabel.text = recipe.subCategory

        Glide.with(holder.itemView.context)
            .load(recipe.recipeImage)
            .placeholder(R.drawable.sample_recipe_image)
            .error(R.drawable.splash_background)
            .into(holder.ivRecipeImage)

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