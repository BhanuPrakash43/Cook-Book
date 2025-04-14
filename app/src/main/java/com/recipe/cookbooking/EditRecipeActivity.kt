package com.recipe.cookbooking

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.recipe.cookbooking.Model.Recipe
import com.recipe.cookbooking.utils.uploadImageToCloudinary
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.bumptech.glide.Glide

class EditRecipeActivity : AppCompatActivity() {
    private lateinit var ingredientsContainer: LinearLayout
    private lateinit var procedureContainer: LinearLayout
    private lateinit var addIngredientButton: Button
    private lateinit var addProcedureButton: Button
    private lateinit var recipeName: EditText
    private lateinit var cookTime: EditText
    private lateinit var categorySpinner: Spinner
    private lateinit var subCategorySpinner: Spinner
    private lateinit var recipeImageView: ImageView
    private lateinit var selectImageButton: Button
    private lateinit var submitUpdateButton: Button
    private lateinit var progressOverlay: ViewGroup
    private lateinit var progressText: TextView
    private lateinit var updateRecipeCloseButton: ImageView

    private lateinit var database: DatabaseReference
    private lateinit var usersDatabase: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private var imageUri: Uri? = null
    private var recipeId: String? = null
    private var currentRecipe: Recipe? = null

    private var isCategoryDataLoaded = false
    private var isRecipeDataLoaded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_recipe)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("Recipes")
        usersDatabase = FirebaseDatabase.getInstance().getReference("users")

        ingredientsContainer = findViewById(R.id.ingredientsContainer)
        procedureContainer = findViewById(R.id.procedureContainer)
        addIngredientButton = findViewById(R.id.addIngredientButton)
        addProcedureButton = findViewById(R.id.addProcedureButton)
        recipeName = findViewById(R.id.recipeName)
        cookTime = findViewById(R.id.cookTime)
        categorySpinner = findViewById(R.id.categorySpinner)
        subCategorySpinner = findViewById(R.id.subCategorySpinner)
        recipeImageView = findViewById(R.id.recipeImageView)
        selectImageButton = findViewById(R.id.selectImageButton)
        submitUpdateButton = findViewById(R.id.submitRecipe)
        progressOverlay = findViewById(R.id.progressOverlay)
        progressText = findViewById(R.id.progressText)
        updateRecipeCloseButton = findViewById(R.id.updateRecipeCloseButton)

        updateRecipeCloseButton.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        loadCategoriesAndSubcategories()

        addIngredientButton.setOnClickListener { addIngredientField() }
        addProcedureButton.setOnClickListener { addProcedureField() }
        selectImageButton.setOnClickListener { openGallery() }
        submitUpdateButton.setOnClickListener { updateRecipe() }

        recipeId = intent.getStringExtra("RECIPE_ID")
        if (recipeId != null) {
            loadRecipeData(recipeId!!)
        }
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
                    if (subcategoriesSnapshot.exists()) {
                        for (subSnapshot in subcategoriesSnapshot.children) {
                            subSnapshot.key?.let { subList.add(it) }
                        }
                    }

                    categoryMap[categoryName] = subList
                }

                val categoryAdapter = ArrayAdapter(
                    this@EditRecipeActivity,
                    android.R.layout.simple_spinner_dropdown_item,
                    categoryList
                )
                categorySpinner.adapter = categoryAdapter

                categorySpinner.onItemSelectedListener =
                    object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(
                            parent: AdapterView<*>, view: View?, position: Int, id: Long
                        ) {
                            val selectedCategory = categoryList[position]
                            val subCategoryList = if (selectedCategory == "Select Cuisine") {
                                listOf("Select Type")
                            } else {
                                categoryMap[selectedCategory] ?: listOf("Select Type")
                            }

                            val subCategoryAdapter = ArrayAdapter(
                                this@EditRecipeActivity,
                                android.R.layout.simple_spinner_dropdown_item,
                                subCategoryList
                            )
                            subCategorySpinner.adapter = subCategoryAdapter

                            // Attempt to set the subcategory selection if recipe is already loaded
                            if (isRecipeDataLoaded) {
                                val subIndex =
                                    subCategoryAdapter.getPosition(currentRecipe?.subCategory)
                                if (subIndex >= 0) subCategorySpinner.setSelection(subIndex)
                            }
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {}
                    }

                isCategoryDataLoaded = true
                setSpinnerSelectionsIfReady()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@EditRecipeActivity,
                    "Failed to load categories",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun setSpinnerSelectionsIfReady() {
        if (isCategoryDataLoaded && isRecipeDataLoaded && currentRecipe != null) {
            val recipe = currentRecipe!!

            val catIndex =
                (categorySpinner.adapter as? ArrayAdapter<String>)?.getPosition(recipe.category)
            if (catIndex != null && catIndex >= 0) {
                categorySpinner.setSelection(catIndex)
            }
        }
    }


    private fun loadRecipeData(recipeId: String) {
        database.child(recipeId).get().addOnSuccessListener { snapshot ->
            currentRecipe = snapshot.getValue(Recipe::class.java)
            currentRecipe?.let { recipe ->
                recipeName.setText(recipe.name)
                cookTime.setText(recipe.cookTime)
                Glide.with(this).load(recipe.recipeImage).into(recipeImageView)

                recipe.ingredients.forEach { addIngredientField(it) }
                recipe.procedures.forEach { addProcedureField(it) }

                isRecipeDataLoaded = true
                setSpinnerSelectionsIfReady()
            }
        }
    }


    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 101)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
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

    private fun updateRecipe() {
        val name = recipeName.text.toString().trim()
        val time = cookTime.text.toString().trim()
        val category = categorySpinner.selectedItem.toString()
        val subCategory = subCategorySpinner.selectedItem.toString()

        if (name.isEmpty() || time.isEmpty() || category == "Select Cuisine" || subCategory == "Select Type") {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        val ingredients = mutableListOf<String>()
        for (i in 0 until ingredientsContainer.childCount) {
            val field =
                (ingredientsContainer.getChildAt(i) as LinearLayout).getChildAt(0) as EditText
            if (field.text.toString().trim().isNotEmpty()) {
                ingredients.add(field.text.toString().trim())
            }
        }

        val procedures = mutableListOf<String>()
        for (i in 0 until procedureContainer.childCount) {
            val field =
                (procedureContainer.getChildAt(i) as LinearLayout).getChildAt(0) as EditText
            if (field.text.toString().trim().isNotEmpty()) {
                procedures.add(field.text.toString().trim())
            }
        }

        if (ingredients.isEmpty() || procedures.isEmpty()) {
            Toast.makeText(this, "Add at least one ingredient and procedure", Toast.LENGTH_SHORT)
                .show()
            return
        }

        showProgress("Updating recipe...")

        val updateInDatabase: (String?) -> Unit = { imageUrl ->
            val updatedRecipe = currentRecipe?.copy(
                name = name,
                cookTime = time,
                category = category,
                subCategory = subCategory,
                ingredients = ingredients,
                procedures = procedures,
                recipeImage = imageUrl ?: currentRecipe?.recipeImage.orEmpty(),
                timestamp = System.currentTimeMillis()
            )

            if (updatedRecipe != null) {
                database.child(recipeId!!).setValue(updatedRecipe)
                    .addOnSuccessListener {
                        hideProgress()
                        Toast.makeText(this, "Recipe Updated Successfully", Toast.LENGTH_SHORT)
                            .show()
                        finish()
                        startActivity(Intent(this, HomeActivity::class.java))
                    }
                    .addOnFailureListener {
                        hideProgress()
                        Toast.makeText(this, "Failed to update recipe", Toast.LENGTH_SHORT).show()
                    }
            }
        }

        if (imageUri != null) {
            uploadImageToCloudinary(
                this, imageUri!!,
                onSuccess = { updateInDatabase(it) },
                onFailure = {
                    hideProgress()
                    Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
                })
        } else {
            updateInDatabase(null)
        }
    }

    private fun addIngredientField(value: String = "") {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
            setPadding(12, 8, 12, 8)
            background = ContextCompat.getDrawable(this@EditRecipeActivity, R.drawable.input_bg)
        }

        val editText = EditText(this).apply {
            hint = "Ingredient"
            setText(value)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            background = ContextCompat.getDrawable(this@EditRecipeActivity, R.drawable.edittext_bg)
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

    private fun addProcedureField(value: String = "") {
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
            setPadding(12, 8, 12, 8)
            background = ContextCompat.getDrawable(this@EditRecipeActivity, R.drawable.input_bg)
        }

        val editText = EditText(this).apply {
            hint = "Procedure Step"
            setText(value)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            background = ContextCompat.getDrawable(this@EditRecipeActivity, R.drawable.edittext_bg)
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
}
