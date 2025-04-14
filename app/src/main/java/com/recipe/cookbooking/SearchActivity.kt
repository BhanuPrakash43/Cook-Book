package com.recipe.cookbooking

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.recipe.cookbooking.Adapter.SearchRecipeAdapter
import com.recipe.cookbooking.Model.Recipe

class SearchActivity : AppCompatActivity() {
    private lateinit var searchAdapter: SearchRecipeAdapter
    private val allRecipes = mutableListOf<Recipe>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val searchCloseButton = findViewById<ImageView>(R.id.searchCloseButton)
        val searchView = findViewById<EditText>(R.id.et_search)
        val recyclerView = findViewById<RecyclerView>(R.id.rv_search_results)

        searchCloseButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        searchAdapter = SearchRecipeAdapter(allRecipes, this)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        recyclerView.adapter = searchAdapter

        loadAllRecipes()

        searchView.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterRecipes(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadAllRecipes() {
        val dbRef = FirebaseDatabase.getInstance().getReference("Recipes")
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allRecipes.clear()
                for (recipeSnap in snapshot.children) {
                    val recipe = recipeSnap.getValue(Recipe::class.java)
                    recipe?.let { allRecipes.add(it) }
                }
                searchAdapter.updateRecipes(allRecipes)

                val noResultsText = findViewById<TextView>(R.id.tv_no_results)
                noResultsText.visibility = if (allRecipes.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun filterRecipes(query: String) {
        val filtered = allRecipes.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.category.contains(query, ignoreCase = true) ||
                    it.subCategory.contains(query, ignoreCase = true) ||
                    it.authorName.contains(query, ignoreCase = true) ||
                    it.ingredients.any { ingredient ->
                        ingredient.contains(query, ignoreCase = true)
                    }
        }
        searchAdapter.updateRecipes(filtered)

        val noResultsText = findViewById<TextView>(R.id.tv_no_results)
        noResultsText.visibility = if (filtered.isEmpty()) View.VISIBLE else View.GONE
    }

}
