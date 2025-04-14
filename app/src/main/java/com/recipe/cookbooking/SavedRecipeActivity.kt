package com.recipe.cookbooking

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.recipe.cookbooking.Adapter.SavedRecipeAdapter
import com.recipe.cookbooking.Model.Recipe

class SavedRecipeActivity : AppCompatActivity() {

    private lateinit var savedCloseButton: ImageView
    private lateinit var tvNoResults: TextView
    private lateinit var rvSavedRecipes: RecyclerView

    private lateinit var recipeAdapter: SavedRecipeAdapter
    private val recipeList = mutableListOf<Recipe>()

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val savedRef =
        FirebaseDatabase.getInstance().getReference("SavedRecipes").child(currentUserId!!)
    private val recipeRef = FirebaseDatabase.getInstance().getReference("Recipes")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_saved_recipe)

        initViews()
        setupRecyclerView()
        fetchSavedRecipes()

        savedCloseButton.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }

    private fun initViews() {
        savedCloseButton = findViewById(R.id.savedCloseButton)
        tvNoResults = findViewById(R.id.tv_no_results)
        rvSavedRecipes = findViewById(R.id.rv_saved_recipes)
    }

    private fun setupRecyclerView() {
        rvSavedRecipes.layoutManager = LinearLayoutManager(this)
        recipeAdapter = SavedRecipeAdapter(this, recipeList)
        rvSavedRecipes.adapter = recipeAdapter
    }


    private fun fetchSavedRecipes() {
        savedRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                recipeList.clear()
                val savedIds = snapshot.children.mapNotNull { it.key }

                if (savedIds.isEmpty()) {
                    tvNoResults.visibility = View.VISIBLE
                    recipeAdapter.notifyDataSetChanged()
                    return
                }

                tvNoResults.visibility = View.GONE

                for (id in savedIds) {
                    recipeRef.child(id).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(recipeSnap: DataSnapshot) {
                            val recipe = recipeSnap.getValue(Recipe::class.java)
                            recipe?.let {
                                it.id = id
                                it.isSaved = true
                                recipeList.add(it)
                                recipeAdapter.notifyDataSetChanged()
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {}
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}
