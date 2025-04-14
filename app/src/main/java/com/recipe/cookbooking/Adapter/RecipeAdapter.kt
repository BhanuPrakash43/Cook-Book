package com.recipe.cookbooking.Adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.recipe.cookbooking.Model.Recipe
import com.recipe.cookbooking.R
import com.recipe.cookbooking.SingleRecipeActivity

class RecipeAdapter(private var recipeList: List<Recipe>, private val context: Context) :
    RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    private val savedRecipesRef = FirebaseDatabase.getInstance().getReference("SavedRecipes")
        .child(currentUserId ?: "")

    inner class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvRecipeName: TextView = itemView.findViewById(R.id.tv_recipe_name)
        val tvCookTime: TextView = itemView.findViewById(R.id.tv_cook_time)
        val tvRating: TextView = itemView.findViewById(R.id.tv_rating)
        val imgRecipe: ImageView = itemView.findViewById(R.id.img_recipe)
        val saveRecipe: ImageView = itemView.findViewById(R.id.iv_bookmark)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recipe, parent, false)
        return RecipeViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipeList[position]

        holder.tvRecipeName.text = recipe.name
        holder.tvCookTime.text = "${recipe.cookTime} Min"
        holder.tvRating.text = String.format("%.1f", recipe.rating)

        Glide.with(holder.itemView.context)
            .load(recipe.recipeImage)
            .placeholder(R.drawable.splash_background)
            .error(R.drawable.splash_background)
            .into(holder.imgRecipe)

        checkSavedStatus(recipe.id, holder.saveRecipe)

        holder.saveRecipe.setOnClickListener {
            toggleSaveRecipe(recipe, holder.saveRecipe)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, SingleRecipeActivity::class.java)
            intent.putExtra("recipe", recipe)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int = recipeList.size

    fun updateRecipes(newRecipes: List<Recipe>) {
        recipeList = newRecipes
        notifyDataSetChanged()
    }

    private fun checkSavedStatus(recipeId: String, saveIcon: ImageView) {
        if (currentUserId == null) return

        savedRecipesRef.child(recipeId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isSaved = snapshot.exists()
                saveIcon.setImageResource(
                    if (isSaved) R.drawable.redsavesign else R.drawable.redsave
                )
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun toggleSaveRecipe(recipe: Recipe, saveIcon: ImageView) {
        if (currentUserId == null) {
            Toast.makeText(context, "Please login to save recipes", Toast.LENGTH_SHORT).show()
            return
        }

        savedRecipesRef.child(recipe.id).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                savedRecipesRef.child(recipe.id).removeValue()
                    .addOnSuccessListener {
                        Toast.makeText(context, "Recipe removed from saved", Toast.LENGTH_SHORT)
                            .show()
                    }
            } else {
                savedRecipesRef.child(recipe.id).setValue(recipe)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Recipe saved", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}
