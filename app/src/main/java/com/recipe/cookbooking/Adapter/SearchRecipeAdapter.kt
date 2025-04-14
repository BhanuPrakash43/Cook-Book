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

class SearchRecipeAdapter(private var recipeList: List<Recipe>, private val context: Context) :
    RecyclerView.Adapter<SearchRecipeAdapter.SearchRecipeViewHolder>() {

    class SearchRecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.recipeImage)
        val title: TextView = itemView.findViewById(R.id.recipeTitle)
        val author: TextView = itemView.findViewById(R.id.recipeAuthor)
//        val rating: TextView = itemView.findViewById(R.id.recipeRating)
//        val time: TextView = itemView.findViewById(R.id.recipeMakingTime)
//        val category: TextView = itemView.findViewById(R.id.recipeCategory)
//        val subCategory: TextView = itemView.findViewById(R.id.recipeSubCategory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchRecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_search_recipe, parent, false)
        return SearchRecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: SearchRecipeViewHolder, position: Int) {
        val recipe = recipeList[position]
        holder.title.text = recipe.name
        holder.author.text = "By ${recipe.authorName}"
//        holder.rating.text = recipe.rating.toString()
//        holder.time.text = "${recipe.cookTime} Min"
//        holder.category.text = recipe.category
//        holder.subCategory.text = recipe.subCategory

        Glide.with(holder.itemView.context)
            .load(recipe.recipeImage)
            .placeholder(R.drawable.splash_background)
            .error(R.drawable.splash_background)
            .into(holder.image)

        holder.itemView.setOnClickListener {
            val intent = Intent(context, SingleRecipeActivity::class.java)
            intent.putExtra("recipe", recipe)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = recipeList.size

    fun updateRecipes(newList: List<Recipe>) {
        recipeList = newList
        notifyDataSetChanged()
    }

}
