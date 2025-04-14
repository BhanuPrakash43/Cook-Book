package com.recipe.cookbooking

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.recipe.cookbooking.Model.Recipe
import com.recipe.cookbooking.utils.uploadImageToCloudinary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AddRecipeActivity : AppCompatActivity() {
    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var procedureContainer: LinearLayout
    private lateinit var addIngredientButton: Button
    private lateinit var addProcedureButton: Button
    private lateinit var submitRecipeButton: Button
    private lateinit var recipeName: EditText
    private lateinit var cookTime: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var subCategorySpinner: Spinner
    private lateinit var addRecipeCloseButton: ImageView
    private lateinit var recipeImageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var progressOverlay: ViewGroup
    private lateinit var progressText: TextView

    private lateinit var database: DatabaseReference
    private lateinit var usersDatabase: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_recipe)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Recipes")
        usersDatabase = FirebaseDatabase.getInstance().getReference("Users")

        ingredientsContainer = findViewById(R.id.ingredientsContainer)
        procedureContainer = findViewById(R.id.procedureContainer)
        addIngredientButton = findViewById(R.id.addIngredientButton)
        addProcedureButton = findViewById(R.id.addProcedureButton)
        submitRecipeButton = findViewById(R.id.submitRecipe)
        recipeName = findViewById(R.id.recipeName)
        cookTime = findViewById(R.id.cookTime)
        categorySpinner = findViewById(R.id.categorySpinner)
        subCategorySpinner = findViewById(R.id.subCategorySpinner)
        addRecipeCloseButton = findViewById(R.id.addRecipeCloseButton)
        recipeImageView = findViewById(R.id.recipeImageView)
        selectImageButton = findViewById(R.id.selectImageButton)
        progressOverlay = findViewById(R.id.progressOverlay)
        progressText = findViewById(R.id.progressText)

        addRecipeCloseButton.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            startActivity(intent)
            finish()
        }

        loadCategoriesAndSubcategories()

        repeat(3) { addIngredientField() }
        repeat(3) { addProcedureField() }

        addIngredientButton.setOnClickListener { addIngredientField() }
        addProcedureButton.setOnClickListener { addProcedureField() }
        selectImageButton.setOnClickListener { openGallery() }
        submitRecipeButton.setOnClickListener { submitRecipe() }
    }

    private val categoriesRef = FirebaseDatabase.getInstance().getReference("Categories")

    private fun loadCategoriesAndSubcategories() {
        categoriesRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val categoryList = mutableListOf("Select Cuisine")
                val categoryMap = mutableMapOf<String, List<String>>()

                for (catSnapshot in snapshot.children) {
                    val categoryName = catSnapshot.key ?: continue
                    categoryList.add(categoryName)

                    val subList = mutableListOf("Select Type")
                    val subcategoriesSnapshot = catSnapshot.child("subcategories")

                    if (!subcategoriesSnapshot.exists()) {
                        continue
                    }

                    for (subSnapshot in subcategoriesSnapshot.children) {
                        val subcategoryName = subSnapshot.key
                        if (subcategoryName != null) {
                            subList.add(subcategoryName)
                        }
                    }

                    categoryMap[categoryName] = subList
                }

                // Setup category spinner
                val categoryAdapter = ArrayAdapter(
                    this@AddRecipeActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    categoryList
                )
                categorySpinner.adapter = categoryAdapter

                // Handle subcategory updates on category selection
                categorySpinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>,
                            view: View?,
                            position: Int,
                            id: Long
                        ) {
                            val selectedCategory = categoryList[position]

                            if (selectedCategory == "Select Cuisine") {
                                val defaultList = listOf("Select Type")
                                val subCategoryAdapter = ArrayAdapter(
                                    this@AddRecipeActivity,
                                    android.R.layout.simple_spinner_dropdown_item,
                                    defaultList
                                )
                                subCategorySpinner.adapter = subCategoryAdapter
                                return
                            }

                            val subCategoryList =
                                categoryMap[selectedCategory] ?: listOf("Select Type")

                            val subCategoryAdapter = ArrayAdapter(
                                this@AddRecipeActivity,
                                android.R.layout.simple_spinner_dropdown_item,
                                subCategoryList
                            )
                            subCategorySpinner.adapter = subCategoryAdapter
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@AddRecipeActivity,
                    "Failed to load categories",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }


    private fun addIngredientField() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            setPadding(12, 8, 12, 8)
            background = ContextCompat.getDrawable(this@AddRecipeActivity, R.drawable.input_bg)
        }

        val editText = EditText(this).apply {
            hint = "Ingredient"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            background = ContextCompat.getDrawable(this@AddRecipeActivity, R.drawable.edittext_bg)
            setPadding(16, 12, 16, 12)
        }

        val deleteButton = ImageButton(this).apply {
            setImageResource(R.drawable.ic_trash)
            layoutParams = LinearLayout.LayoutParams(50, 50).apply { setMargins(12, 24, 12, 12) }
            setOnClickListener { ingredientsContainer.removeView(container) }
        }

        container.addView(editText)
        container.addView(deleteButton)
        ingredientsContainer.addView(container)
    }

    private fun addProcedureField() {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            setPadding(12, 8, 12, 8)
            background = ContextCompat.getDrawable(this@AddRecipeActivity, R.drawable.input_bg)
        }

        val editText = EditText(this).apply {
            hint = "Procedure Step"
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            background = ContextCompat.getDrawable(this@AddRecipeActivity, R.drawable.edittext_bg)
            setPadding(16, 12, 16, 12)
        }

        val deleteButton = ImageButton(this).apply {
            setImageResource(R.drawable.ic_trash)
            layoutParams = LinearLayout.LayoutParams(50, 50).apply { setMargins(12, 24, 12, 12) }
            setOnClickListener { procedureContainer.removeView(container) }
        }

        container.addView(editText)
        container.addView(deleteButton)
        procedureContainer.addView(container)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 100)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            imageUri = data?.data
            recipeImageView.setImageURI(imageUri)
        }
    }

    private fun showProgress(message: String) {
        progressText.text = message
        progressOverlay.visibility = ViewGroup.VISIBLE
    }

    private fun hideProgress() {
        progressOverlay.visibility = ViewGroup.GONE
    }

    private fun submitRecipe() {
        val name = recipeName.text.toString().trim()
        val time = cookTime.text.toString().trim()
        val category = categorySpinner.selectedItem.toString()
        val subCategory = subCategorySpinner.selectedItem.toString()

        if (name.isEmpty() || time.isEmpty() || category == "Select Cuisine" || subCategory == "Select Type") {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri == null) {
            Toast.makeText(this, "Please select an image for your recipe", Toast.LENGTH_SHORT)
                .show()
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        val authorId = user?.uid ?: return

        // Fetch user details from Firebase Realtime Database
        val usersDatabase = FirebaseDatabase.getInstance().getReference("users").child(authorId)
        usersDatabase.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val authorName =
                    snapshot.child("fullName").getValue(String::class.java) ?: "Anonymous"
                val authorImage = snapshot.child("profilePic").getValue(String::class.java) ?: ""

                val ingredients = mutableListOf<String>()
                for (i in 0 until ingredientsContainer.childCount) {
                    val ingredientField =
                        (ingredientsContainer.getChildAt(i) as LinearLayout).getChildAt(0) as EditText
                    val ingredient = ingredientField.text.toString().trim()
                    if (ingredient.isNotEmpty()) ingredients.add(ingredient)
                }

                if (ingredients.isEmpty()) {
                    Toast.makeText(
                        this@AddRecipeActivity,
                        "Please add at least one ingredient",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                val procedures = mutableListOf<String>()
                for (i in 0 until procedureContainer.childCount) {
                    val procedureField =
                        (procedureContainer.getChildAt(i) as LinearLayout).getChildAt(0) as EditText
                    val procedure = procedureField.text.toString().trim()
                    if (procedure.isNotEmpty()) procedures.add(procedure)
                }

                if (procedures.isEmpty()) {
                    Toast.makeText(
                        this@AddRecipeActivity,
                        "Please add at least one procedure step",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                showProgress("Uploading image...")

                uploadImageToCloudinary(
                    this@AddRecipeActivity,
                    imageUri!!,
                    onSuccess = { imageUrl ->
                        runOnUiThread {
                            progressText.text = "Saving recipe..."
                        }

                        val recipeId = database.push().key ?: run {
                            hideProgress()
                            Toast.makeText(
                                this@AddRecipeActivity,
                                "Failed to generate recipe ID",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@uploadImageToCloudinary
                        }

                        val recipe = Recipe(
                            id = recipeId,
                            name = name,
                            category = category,
                            subCategory = subCategory,
                            cookTime = time,
                            rating = 0.0f,
                            recipeImage = imageUrl,
                            authorId = authorId,
                            authorName = authorName,
                            authorImage = authorImage,
                            ingredients = ingredients,
                            procedures = procedures,
                            likedBy = mutableListOf(),
                            likeCount = 0,
                            isSaved = false,
                            timestamp = System.currentTimeMillis()
                        )

                        database.child(recipeId).setValue(recipe)
                            .addOnSuccessListener {
                                hideProgress()
                                Toast.makeText(
                                    this@AddRecipeActivity,
                                    "Recipe Added Successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                                startActivity(
                                    Intent(
                                        this@AddRecipeActivity,
                                        HomeActivity::class.java
                                    )
                                )
                            }
                            .addOnFailureListener { error ->
                                hideProgress()
                                Toast.makeText(
                                    this@AddRecipeActivity,
                                    "Failed to Add Recipe: ${error.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    },
                    onFailure = { errorMessage ->
                        runOnUiThread {
                            hideProgress()
                            Toast.makeText(
                                this@AddRecipeActivity,
                                "Image Upload Failed: $errorMessage",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                )
            } else {
                Toast.makeText(this@AddRecipeActivity, "User details not found", Toast.LENGTH_SHORT)
                    .show()
            }
        }.addOnFailureListener {
            Toast.makeText(
                this@AddRecipeActivity,
                "Failed to fetch user details",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

}
