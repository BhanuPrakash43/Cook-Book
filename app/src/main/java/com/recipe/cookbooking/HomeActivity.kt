package com.recipe.cookbooking

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Rect
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.recipe.cookbooking.Adapter.NewRecipeAdapter
import com.recipe.cookbooking.Adapter.RecipeAdapter
import com.recipe.cookbooking.Model.Recipe
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.recipe.cookbooking.Adapter.NonVegRecipeAdapter
import com.recipe.cookbooking.Adapter.QuickEasyAdapter
import com.recipe.cookbooking.Adapter.VegRecipeAdapter

class HomeActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseUser: FirebaseUser
    private lateinit var addRecipeButton: FloatingActionButton

    private lateinit var categoryContainer: LinearLayout
    private val categoriesRef = FirebaseDatabase.getInstance().getReference("Categories")

    private val allRecipes = mutableListOf<Recipe>()

    // Section-specific filtered lists
    private var featuredRecipes = mutableListOf<Recipe>()
    private var newRecipes = mutableListOf<Recipe>()
    private var quickEasyRecipes = mutableListOf<Recipe>()
    private var vegRecipes = mutableListOf<Recipe>()
    private var nonVegRecipes = mutableListOf<Recipe>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val navProfile = findViewById<LinearLayout>(R.id.nav_profile)
        val navHome = findViewById<LinearLayout>(R.id.nav_home)
        val navSavedRecipe = findViewById<LinearLayout>(R.id.nav_bookmark)
        val navSearch = findViewById<LinearLayout>(R.id.nav_search)
        addRecipeButton = findViewById(R.id.fab_add_recipe)
        categoryContainer = findViewById(R.id.categoryContainer)

        navHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
        }

        navSearch.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        addRecipeButton.setOnClickListener {
            startActivity(Intent(this, AddRecipeActivity::class.java))
            finish()
        }

        navSavedRecipe.setOnClickListener {
            val intent = Intent(this, SavedRecipeActivity::class.java)
            startActivity(intent)
        }

        navProfile.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }

        firebaseAuth = FirebaseAuth.getInstance()
        val currentUser = firebaseAuth.currentUser

        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        } else {
            firebaseUser = currentUser
        }

        val tvUsername = findViewById<TextView>(R.id.dashboardUsername)
        val profileImageView = findViewById<ImageView>(R.id.userProfileImage)

        val nameFromPrefs = SavedPreference.getUsername(this)
        val profilePicFromPrefs = SavedPreference.getProfilePic(this)

        if (nameFromPrefs != null && nameFromPrefs.isNotEmpty()) {
            tvUsername.text = "Hello, $nameFromPrefs"

            if (profilePicFromPrefs != null && profilePicFromPrefs.isNotEmpty()) {
                Glide.with(this)
                    .load(profilePicFromPrefs)
                    .placeholder(R.drawable.profile_image)
                    .into(profileImageView)
            } else {
                Glide.with(this)
                    .load(R.drawable.profile_image)
                    .into(profileImageView)
            }
        } else {
            val userId = firebaseUser.uid
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val fullName = document.getString("fullName") ?: "User"
                        val profilePicUrl = document.getString("profilePic") ?: ""

                        // Save to preferences for next time
                        SavedPreference.setUsername(this@HomeActivity, fullName)
                        SavedPreference.setProfilePic(this@HomeActivity, profilePicUrl)

                        tvUsername.text = "Hello, $fullName"

                        if (profilePicUrl.isNotEmpty()) {
                            Glide.with(this@HomeActivity)
                                .load(profilePicUrl)
                                .placeholder(R.drawable.profile_image)
                                .into(profileImageView)
                        } else {
                            loadFirebaseUserData(tvUsername, profileImageView)
                        }
                    } else {
                        loadFirebaseUserData(tvUsername, profileImageView)
                    }
                }
                .addOnFailureListener {
                    loadFirebaseUserData(tvUsername, profileImageView)
                }
        }

        val searchBox = findViewById<LinearLayout>(R.id.searchBox)

        searchBox.setOnClickListener {
            val intent = Intent(this, SearchActivity::class.java)
            startActivity(intent)
        }

        val filterIcon = findViewById<ImageView>(R.id.filterRecipes)
        filterIcon.setOnClickListener {
            showSortDialog()
        }

        val loaderOverlay = findViewById<LinearLayout>(R.id.loader_overlay)
        loaderOverlay.visibility = View.VISIBLE

        loadCategories()
        setupFeaturedRecipesRecyclerView()
        setupNewRecipesRecyclerView()
        setupQuickEasyRecyclerView()
        setupVegRecyclerView()
        setupNonVegRecyclerView()
    }

    private fun loadFirebaseUserData(tvUsername: TextView, profileImageView: ImageView) {
        val displayName = firebaseUser.displayName
            ?: firebaseUser.providerData.find { it.displayName != null }?.displayName ?: "User"
        val profilePicUrl = firebaseUser.photoUrl

        tvUsername.text = "Hello, $displayName"

        Glide.with(this)
            .load(profilePicUrl ?: R.drawable.profile_image)
            .placeholder(R.drawable.profile_image)
            .into(profileImageView)
    }

    private var totalSectionsToLoad = 5
    private var sectionsLoaded = 0

    private fun checkIfLoadingComplete() {
        sectionsLoaded++
        if (sectionsLoaded >= totalSectionsToLoad) {
            findViewById<LinearLayout>(R.id.loader_overlay).visibility = View.GONE
        }
    }

    private fun showSortDialog() {
        val options = arrayOf(
            "Default Order",
            "Newest First",
            "Oldest First",
            "Name (A-Z)",
            "Name (Z-A)",
            "Highest Rating",
            "Lowest Rating"
        )

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Sort Recipes")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> resetToDefaultOrder()
                1 -> sortRecipesByNewest()
                2 -> sortRecipesByOldest()
                3 -> sortRecipesByNameAZ()
                4 -> sortRecipesByNameZA()
                5 -> sortRecipesByRatingHigh()
                6 -> sortRecipesByRatingLow()
            }
        }
        builder.show()
    }

    private fun resetToDefaultOrder() {
        setupFeaturedRecipesRecyclerView()
        setupNewRecipesRecyclerView()
        setupQuickEasyRecyclerView()
        setupVegRecyclerView()
        setupNonVegRecyclerView()

        updateRecyclerViews()
    }

    private fun sortRecipesByNewest() {
        featuredRecipes.sortByDescending { it.timestamp }
        newRecipes.sortByDescending { it.timestamp }
        quickEasyRecipes.sortByDescending { it.timestamp }
        vegRecipes.sortByDescending { it.timestamp }
        nonVegRecipes.sortByDescending { it.timestamp }

        updateRecyclerViews()
    }

    private fun sortRecipesByOldest() {
        featuredRecipes.sortBy { it.timestamp }
        newRecipes.sortBy { it.timestamp }
        quickEasyRecipes.sortBy { it.timestamp }
        vegRecipes.sortBy { it.timestamp }
        nonVegRecipes.sortBy { it.timestamp }

        updateRecyclerViews()
    }

    private fun sortRecipesByNameAZ() {
        featuredRecipes.sortBy { it.name }
        newRecipes.sortBy { it.name }
        quickEasyRecipes.sortBy { it.name }
        vegRecipes.sortBy { it.name }
        nonVegRecipes.sortBy { it.name }

        updateRecyclerViews()
    }

    private fun sortRecipesByNameZA() {
        featuredRecipes.sortByDescending { it.name }
        newRecipes.sortByDescending { it.name }
        quickEasyRecipes.sortByDescending { it.name }
        vegRecipes.sortByDescending { it.name }
        nonVegRecipes.sortByDescending { it.name }

        updateRecyclerViews()
    }

    private fun sortRecipesByRatingHigh() {
        featuredRecipes.sortByDescending { it.rating }
        newRecipes.sortByDescending { it.rating }
        quickEasyRecipes.sortByDescending { it.rating }
        vegRecipes.sortByDescending { it.rating }
        nonVegRecipes.sortByDescending { it.rating }

        updateRecyclerViews()
    }

    private fun sortRecipesByRatingLow() {
        featuredRecipes.sortBy { it.rating }
        newRecipes.sortBy { it.rating }
        quickEasyRecipes.sortBy { it.rating }
        vegRecipes.sortBy { it.rating }
        nonVegRecipes.sortBy { it.rating }

        updateRecyclerViews()
    }

    private fun updateRecyclerViews() {
        (findViewById<RecyclerView>(R.id.rv_featured_recipes).adapter as? RecipeAdapter)?.updateRecipes(
            featuredRecipes
        )
        (findViewById<RecyclerView>(R.id.recycler_new_recipes).adapter as? NewRecipeAdapter)?.updateRecipes(
            newRecipes
        )
        (findViewById<RecyclerView>(R.id.recycler_quick_easy).adapter as? QuickEasyAdapter)?.updateRecipes(
            quickEasyRecipes
        )
        (findViewById<RecyclerView>(R.id.recycler_veg_recipes).adapter as? VegRecipeAdapter)?.updateRecipes(
            vegRecipes
        )
        (findViewById<RecyclerView>(R.id.recycler_non_veg_recipes).adapter as? NonVegRecipeAdapter)?.updateRecipes(
            nonVegRecipes
        )

        findViewById<TextView>(R.id.tv_no_featured_recipes).visibility =
            if (featuredRecipes.isEmpty()) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.tv_no_new_recipes).visibility =
            if (newRecipes.isEmpty()) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.tv_no_quick_easy).visibility =
            if (quickEasyRecipes.isEmpty()) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.tv_no_veg_recipes).visibility =
            if (vegRecipes.isEmpty()) View.VISIBLE else View.GONE
        findViewById<TextView>(R.id.tv_no_non_veg_recipes).visibility =
            if (nonVegRecipes.isEmpty()) View.VISIBLE else View.GONE
    }


    private fun loadCategories() {
        categoryContainer.removeAllViews()

        // Add the static "All" button first
        val allButton = createCategoryButton("All", true)
        categoryContainer.addView(allButton)

        // Fetch categories from Firebase
        categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (categorySnapshot in snapshot.children) {
                    val categoryName = categorySnapshot.key ?: continue
                    val categoryButton = createCategoryButton(categoryName, false)
                    categoryContainer.addView(categoryButton)
                    checkIfLoadingComplete()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@HomeActivity, "Failed to load categories", Toast.LENGTH_SHORT)
                    .show()
                checkIfLoadingComplete()
            }
        })
    }

    private fun createCategoryButton(text: String, isSelected: Boolean): Button {
        val button = Button(this)
        button.text = text
        button.textSize = 14f
        button.setTextColor(
            ContextCompat.getColor(this, if (isSelected) android.R.color.white else R.color.gray3)
        )
        button.setPadding(24, 12, 24, 12)
        button.background = ContextCompat.getDrawable(
            this,
            if (isSelected) R.drawable.category_selected else R.drawable.category_unselected
        )

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(12, 0, 0, 0)
        button.layoutParams = params

        button.setOnClickListener {
            Toast.makeText(this, "$text clicked", Toast.LENGTH_SHORT).show()
            updateCategorySelection(text)
        }

        return button
    }

    private fun updateCategorySelection(selectedCategory: String) {
        for (i in 0 until categoryContainer.childCount) {
            val button = categoryContainer.getChildAt(i) as? Button ?: continue
            val isSelected = button.text.toString() == selectedCategory

            button.background = ContextCompat.getDrawable(
                this,
                if (isSelected) R.drawable.category_selected else R.drawable.category_unselected
            )
            button.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (isSelected) android.R.color.white else R.color.gray3
                )
            )
        }

        // Apply filters to each section
        val filteredFeatured = filterRecipes(featuredRecipes, selectedCategory)
        val filteredNew = filterRecipes(newRecipes, selectedCategory)
        val filteredQuickEasy = filterRecipes(quickEasyRecipes, selectedCategory)
        val filteredVeg = filterRecipes(vegRecipes, selectedCategory)
        val filteredNonVeg = filterRecipes(nonVegRecipes, selectedCategory)

        // Update all adapters
        (findViewById<RecyclerView>(R.id.rv_featured_recipes).adapter as? RecipeAdapter)?.updateRecipes(
            filteredFeatured
        )
        (findViewById<RecyclerView>(R.id.recycler_new_recipes).adapter as? NewRecipeAdapter)?.updateRecipes(
            filteredNew
        )
        (findViewById<RecyclerView>(R.id.recycler_quick_easy).adapter as? QuickEasyAdapter)?.updateRecipes(
            filteredQuickEasy
        )
        (findViewById<RecyclerView>(R.id.recycler_veg_recipes).adapter as? VegRecipeAdapter)?.updateRecipes(
            filteredVeg
        )
        (findViewById<RecyclerView>(R.id.recycler_non_veg_recipes).adapter as? NonVegRecipeAdapter)?.updateRecipes(
            filteredNonVeg
        )

        // Show/hide "No recipes" messages
        findViewById<TextView>(R.id.tv_no_featured_recipes).visibility =
            if (filteredFeatured.isEmpty()) View.VISIBLE else View.GONE

        findViewById<TextView>(R.id.tv_no_new_recipes).visibility =
            if (filteredNew.isEmpty()) View.VISIBLE else View.GONE

        findViewById<TextView>(R.id.tv_no_quick_easy).visibility =
            if (filteredQuickEasy.isEmpty()) View.VISIBLE else View.GONE

        findViewById<TextView>(R.id.tv_no_veg_recipes).visibility =
            if (filteredVeg.isEmpty()) View.VISIBLE else View.GONE

        findViewById<TextView>(R.id.tv_no_non_veg_recipes).visibility =
            if (filteredNonVeg.isEmpty()) View.VISIBLE else View.GONE

    }

    private fun filterRecipes(list: List<Recipe>, category: String): List<Recipe> {
        return if (category == "All") list else list.filter {
            it.category.equals(
                category,
                ignoreCase = true
            )
        }
    }


    private fun setupFeaturedRecipesRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.rv_featured_recipes)
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val adapter = RecipeAdapter(mutableListOf(), this)
        recyclerView.adapter = adapter
        recyclerView.addItemDecoration(HorizontalSpacingItemDecoration(40))

        val databaseReference = FirebaseDatabase.getInstance().getReference("Recipes")
        databaseReference.orderByChild("rating")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allRecipes.clear()
                    featuredRecipes.clear()

                    for (recipeSnapshot in snapshot.children) {
                        val recipe = recipeSnapshot.getValue(Recipe::class.java)
                        if (recipe != null) {
                            allRecipes.add(recipe)
                            featuredRecipes.add(recipe)
                        }
                    }
                    adapter.updateRecipes(featuredRecipes.reversed())
                    checkIfLoadingComplete()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Failed to load featured recipes",
                        Toast.LENGTH_SHORT
                    ).show()
                    checkIfLoadingComplete()
                }
            })
    }

    private fun setupNewRecipesRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_new_recipes)
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        val adapter = NewRecipeAdapter(mutableListOf(), this)
        recyclerView.adapter = adapter

        val databaseReference = FirebaseDatabase.getInstance().getReference("Recipes")
        databaseReference.orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allRecipes.clear()
                    newRecipes.clear()

                    for (recipeSnapshot in snapshot.children) {
                        val recipe = recipeSnapshot.getValue(Recipe::class.java)
                        if (recipe != null) {
                            allRecipes.add(recipe)
                            newRecipes.add(recipe)
                        }
                    }
                    adapter.updateRecipes(newRecipes.reversed())
                    checkIfLoadingComplete()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Failed to load new recipes",
                        Toast.LENGTH_SHORT
                    ).show()
                    checkIfLoadingComplete()
                }
            })
    }

    private fun setupQuickEasyRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_quick_easy)
        recyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val adapter = QuickEasyAdapter(mutableListOf(), this)
        recyclerView.adapter = adapter

        val databaseReference = FirebaseDatabase.getInstance().getReference("Recipes")
        databaseReference.orderByChild("cookTime")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allRecipes.clear()
                    quickEasyRecipes.clear()

                    for (recipeSnapshot in snapshot.children) {
                        val recipe = recipeSnapshot.getValue(Recipe::class.java)
                        if (recipe != null && recipe.cookTime.toIntOrNull() != null) {
                            val cookTime = recipe.cookTime.toIntOrNull() ?: Int.MAX_VALUE
                            if (cookTime <= 30) {
                                allRecipes.add(recipe)
                                quickEasyRecipes.add(recipe)
                            }
                        }
                    }
                    adapter.updateRecipes(quickEasyRecipes.reversed())
                    checkIfLoadingComplete()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Failed to load quick & easy recipes",
                        Toast.LENGTH_SHORT
                    ).show()
                    checkIfLoadingComplete()
                }
            })
    }

    private fun setupVegRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_veg_recipes)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        val adapter = VegRecipeAdapter(mutableListOf(), this)
        recyclerView.adapter = adapter

        val databaseReference = FirebaseDatabase.getInstance().getReference("Recipes")
        databaseReference.orderByChild("cookTime")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allRecipes.clear()
                    vegRecipes.clear()

                    for (recipeSnapshot in snapshot.children) {
                        val recipe = recipeSnapshot.getValue(Recipe::class.java)
                        if (recipe != null && recipe.subCategory != null) {
                            val subCategoryLower = recipe.subCategory.lowercase()
                            if (
                                (
                                        (subCategoryLower.contains("veg") || subCategoryLower.contains(
                                            "vegetarian"
                                        )) &&
                                                !subCategoryLower.contains("non-veg") &&
                                                !subCategoryLower.contains("non-vegetarian") &&
                                                !subCategoryLower.contains("non vegetarian")
                                        ) ||
                                (
                                        // It's not clearly marked as veg or non-veg, so include as veg
                                        !subCategoryLower.contains("non-veg") &&
                                                !subCategoryLower.contains("non-vegetarian") &&
                                                !subCategoryLower.contains("non vegetarian")
                                        )
                            ) {
                                allRecipes.add(recipe)
                                vegRecipes.add(recipe)
                            }


                        }
                    }
                    adapter.updateRecipes(vegRecipes.reversed())
                    checkIfLoadingComplete()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Failed to load vegetarian recipes",
                        Toast.LENGTH_SHORT
                    ).show()
                    checkIfLoadingComplete()
                }
            })
    }

    private fun setupNonVegRecyclerView() {
        val recyclerView: RecyclerView = findViewById(R.id.recycler_non_veg_recipes)
        recyclerView.layoutManager = GridLayoutManager(this, 2)
        val adapter = NonVegRecipeAdapter(mutableListOf(), this)
        recyclerView.adapter = adapter

        val databaseReference = FirebaseDatabase.getInstance().getReference("Recipes")
        databaseReference.orderByChild("cookTime")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allRecipes.clear()
                    nonVegRecipes.clear()

                    for (recipeSnapshot in snapshot.children) {
                        val recipe = recipeSnapshot.getValue(Recipe::class.java)
                        if (recipe != null && recipe.subCategory != null) {
                            val subCategoryLower = recipe.subCategory.lowercase()
                            if (subCategoryLower.contains("non-veg") ||
                                subCategoryLower.contains("non-vegetarian") ||
                                subCategoryLower.contains("non vegetarian")
                            ) {
                                allRecipes.add(recipe)
                                nonVegRecipes.add(recipe)
                            }
                        }
                    }
                    adapter.updateRecipes(nonVegRecipes.reversed())
                    checkIfLoadingComplete()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@HomeActivity,
                        "Failed to load non-vegetarian recipes",
                        Toast.LENGTH_SHORT
                    ).show()
                    checkIfLoadingComplete()
                }
            })
    }

}

class HorizontalSpacingItemDecoration(private val space: Int) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.right = space
    }
}
