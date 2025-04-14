package com.recipe.cookbooking

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.recipe.cookbooking.Model.Recipe
import android.content.res.ColorStateList
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.recipe.cookbooking.Adapter.IngredientAdapter
import com.recipe.cookbooking.Adapter.ProcedureAdapter

class SingleRecipeActivity : AppCompatActivity() {
    private lateinit var followButton: Button
    private lateinit var database: FirebaseDatabase
    private lateinit var usersRef: DatabaseReference
    private lateinit var currentUserId: String

    private lateinit var moreButton: ImageView
    private lateinit var btnIngredient: Button
    private lateinit var btnProcedure: Button
    private lateinit var rvIngredients: RecyclerView
    private lateinit var rvProcedures: RecyclerView
    private lateinit var recipeRating: TextView

    private lateinit var likeButton: ImageView
    private lateinit var likeCountText: TextView
    private lateinit var recipeRef: DatabaseReference

    private lateinit var saveRecipeIcon: ImageView
    private var isSaved = false
    private lateinit var savedRecipesRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_single_recipe)

        database = FirebaseDatabase.getInstance()

        val recipe = intent.getParcelableExtra<Recipe>("recipe")

        val imgRecipe = findViewById<ImageView>(R.id.img_recipe)
        val recipeTitle = findViewById<TextView>(R.id.recipeTitle)
        val recipeTime = findViewById<TextView>(R.id.recipeMakingTime)
        val authorImage = findViewById<ImageView>(R.id.recipeAuthorImage)
        val recipeCategory = findViewById<TextView>(R.id.recipeCategory)
        val recipeSubCategory = findViewById<TextView>(R.id.recipeSubCategory)
        val authorName = findViewById<TextView>(R.id.recipeAuthorName)
        saveRecipeIcon = findViewById(R.id.saveRecipeIcon)

        recipeRating = findViewById(R.id.recipeRating)
        moreButton = findViewById(R.id.btn_more)

        likeButton = findViewById(R.id.recipeLikeButton)
        likeCountText = findViewById(R.id.likeCountText)

        val recipeId = recipe?.id ?: return
        recipeRef = database.getReference("Recipes").child(recipeId)

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        // Initialize saved recipes reference
        savedRecipesRef = database.getReference("SavedRecipes").child(currentUserId)

        // Check if the recipe is already saved
        checkSavedStatus(recipeId)

        // Set click listener for save icon
        saveRecipeIcon.setOnClickListener {
            toggleSaveRecipe(recipe)
        }

        // RecyclerViews for Ingredients & Procedures
        rvIngredients = findViewById(R.id.rv_ingredients)
        rvProcedures = findViewById(R.id.rv_procedures)

        btnIngredient = findViewById(R.id.btn_ingredient)
        btnProcedure = findViewById(R.id.btn_procedure)

        // Set Data to UI
        recipe?.let {
            recipeTitle.text = it.name
            recipeCategory.text = it.category
            recipeSubCategory.text = it.subCategory
            recipeTime.text = "${it.cookTime} Min"
            recipeRating.text = it.rating.toString()
            authorName.text = it.authorName

            // Load images using Glide
            Glide.with(this)
                .load(it.recipeImage)
                .placeholder(R.drawable.splash_background)
                .error(R.drawable.splash_background)
                .into(imgRecipe)

            Glide.with(this)
                .load(it.authorImage)
                .placeholder(R.drawable.profile_image)
                .error(R.drawable.profile_image)
                .into(authorImage)

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@let

            recipeRef.child("likedBy").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val likedUsers =
                        snapshot.children.mapNotNull { it.getValue(String::class.java) }
                    val alreadyLiked = likedUsers.contains(userId)

                    updateLikeUI(alreadyLiked, recipe.likeCount)
                }

                override fun onCancelled(error: DatabaseError) {}
            })

            likeButton.setOnClickListener {
                recipeRef.runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val recipeData =
                            currentData.getValue(Recipe::class.java) ?: return Transaction.success(
                                currentData
                            )

                        val likedBy = recipeData.likedBy.toMutableList()
                        val likeCount = recipeData.likeCount
                        val hasLiked = likedBy.contains(userId)

                        if (hasLiked) {
                            likedBy.remove(userId)
                            recipeData.likeCount = (likeCount - 1).coerceAtLeast(0)
                        } else {
                            likedBy.add(userId)
                            recipeData.likeCount = likeCount + 1
                        }

                        recipeData.likedBy = likedBy
                        currentData.value = recipeData
                        return Transaction.success(currentData)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        currentData: DataSnapshot?
                    ) {
                        val updatedRecipe = currentData?.getValue(Recipe::class.java)
                        val hasLiked = updatedRecipe?.likedBy?.contains(userId) == true
                        val updatedCount = updatedRecipe?.likeCount ?: 0

                        updateLikeUI(hasLiked, updatedCount)
                    }
                })
            }

            // Set Ingredients List
            rvIngredients.layoutManager = LinearLayoutManager(this)
            rvIngredients.adapter = IngredientAdapter(it.ingredients)

            rvProcedures.layoutManager = LinearLayoutManager(this)
            rvProcedures.adapter = ProcedureAdapter(it.procedures)

            // Show Ingredients Initially
            rvIngredients.visibility = RecyclerView.VISIBLE
            rvProcedures.visibility = RecyclerView.GONE

            // Toggle Between Ingredients & Procedures
            btnIngredient.setOnClickListener {
                rvIngredients.visibility = RecyclerView.VISIBLE
                rvProcedures.visibility = RecyclerView.GONE

                btnIngredient.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary100))
                btnIngredient.setTextColor(ContextCompat.getColor(this, R.color.white))

                btnProcedure.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
                btnProcedure.setTextColor(ContextCompat.getColor(this, R.color.gray1))
            }

            btnProcedure.setOnClickListener {
                rvIngredients.visibility = RecyclerView.GONE
                rvProcedures.visibility = RecyclerView.VISIBLE

                btnIngredient.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.white))
                btnIngredient.setTextColor(ContextCompat.getColor(this, R.color.gray1))

                btnProcedure.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primary100))
                btnProcedure.setTextColor(ContextCompat.getColor(this, R.color.white))
            }
        }

        moreButton.setOnClickListener {
            showPopupMenu(it)
        }

        findViewById<ImageView>(R.id.btn_back).setOnClickListener {
            finish()
        }

        followButton = findViewById(R.id.authorFollowButton)
        usersRef = database.getReference("users")

        val authorId = recipe?.authorId
        if (authorId == null || authorId == currentUserId) {
            followButton.visibility = View.GONE
        } else {
            usersRef.child(currentUserId).child("following").get()
                .addOnSuccessListener { snapshot ->
                    val isFollowing = snapshot.children.any { it.value?.toString() == authorId }
                    updateFollowButton(isFollowing)
                }

            followButton.setOnClickListener {
                toggleFollowState(authorId.toString())
            }
        }

    }

    // Method to check if recipe is saved
    private fun checkSavedStatus(recipeId: String) {
        savedRecipesRef.child(recipeId).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                isSaved = snapshot.exists()
                updateSaveIcon()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@SingleRecipeActivity,
                    "Error checking saved status",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    // Method to update save icon
    private fun updateSaveIcon() {
        saveRecipeIcon.setImageResource(
            if (isSaved) R.drawable.redsavesign else R.drawable.redsave
        )
    }

    // Method to toggle save state
    private fun toggleSaveRecipe(recipe: Recipe?) {
        if (recipe == null) return

        if (isSaved) {
            // Unsave recipe
            savedRecipesRef.child(recipe.id).removeValue()
                .addOnSuccessListener {
                    Toast.makeText(this, "Recipe removed from saved", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to remove recipe", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Save recipe
            savedRecipesRef.child(recipe.id).setValue(recipe)
                .addOnSuccessListener {
                    Toast.makeText(this, "Recipe saved", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to save recipe", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateLikeUI(isLiked: Boolean, count: Int) {
        likeButton.setImageResource(if (isLiked) R.drawable.red_full_heart else R.drawable.red_heart)
        likeButton.animate().scaleX(1.2f).scaleY(1.2f).setDuration(100).withEndAction {
            likeButton.animate().scaleX(1f).scaleY(1f).start()
        }.start()
        likeCountText.text = count.toString()
    }

    // Show Popup Menu
    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view, Gravity.END)
        popupMenu.menuInflater.inflate(R.menu.recipe_more_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {

                R.id.action_rate -> {
                    showRatingDialog()
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

    // Show Rating Dialog
    private fun showRatingDialog() {
        val dialogView = layoutInflater.inflate(R.layout.rating_bar, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)
        val submitButton = dialogView.findViewById<Button>(R.id.btn_submit_rating)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        // Enable button when rating > 0
        ratingBar.setOnRatingBarChangeListener { _, rating, _ ->
            if (rating > 0f) {
                submitButton.isEnabled = true
                submitButton.backgroundTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(this, R.color.secondary100))
            } else {
                submitButton.isEnabled = false
            }
        }

        submitButton.setOnClickListener {
            val rating = ratingBar.rating
            recipeRating.text = rating.toString()
            Toast.makeText(this, "Rated $rating Stars", Toast.LENGTH_SHORT).show()

            val recipe = intent.getParcelableExtra<Recipe>("recipe")
            val recipeId = recipe?.id ?: return@setOnClickListener

            val currentUser = FirebaseAuth.getInstance().currentUser
            val userId = currentUser?.uid ?: return@setOnClickListener

            val database = FirebaseDatabase.getInstance()
            val ratingsRef = database.getReference("Ratings").child(recipeId)
            val recipeRef = database.getReference("Recipes").child(recipeId)

            ratingsRef.child(userId).setValue(rating).addOnSuccessListener {

                ratingsRef.get().addOnSuccessListener { dataSnapshot ->
                    var totalRating = 0f
                    var count = 0

                    for (child in dataSnapshot.children) {
                        val userRating = child.getValue(Float::class.java)
                        if (userRating != null) {
                            totalRating += userRating
                            count++
                        }
                    }

                    val averageRating = if (count > 0) totalRating / count else 0f

                    recipeRef.child("rating").setValue(averageRating)
                        .addOnSuccessListener {
                            recipeRating.text = String.format("%.1f", averageRating)
                            Toast.makeText(this, "Rating updated!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Error updating rating: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                }.addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Failed to fetch ratings: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            }.addOnFailureListener { e ->
                Toast.makeText(this, "Failed to rate: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun updateFollowButton(isFollowing: Boolean) {
        followButton.text = if (isFollowing) "Unfollow" else "Follow"
        followButton.backgroundTintList = ContextCompat.getColorStateList(
            this,
            if (isFollowing) R.color.gray2 else R.color.primary100
        )
    }

    private fun toggleFollowState(authorId: String) {
        val followingRef = usersRef.child(currentUserId).child("following")
        val followersRef = usersRef.child(authorId).child("followers")

        followingRef.get().addOnSuccessListener { snapshot ->
            val currentFollowing = snapshot.children.mapNotNull { it.value?.toString() }
            val isFollowing = currentFollowing.contains(authorId)

            if (isFollowing) {
                // Unfollow
                val updates = hashMapOf<String, Any?>(
                    "/users/$currentUserId/following/$authorId" to null,
                    "/users/$authorId/followers/$currentUserId" to null
                )
                database.reference.updateChildren(updates).addOnSuccessListener {
                    updateFollowButton(false)
                    Toast.makeText(this, "Unfollowed", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Follow
                val updates = hashMapOf<String, Any?>(
                    "/users/$currentUserId/following/$authorId" to authorId,
                    "/users/$authorId/followers/$currentUserId" to currentUserId
                )
                database.reference.updateChildren(updates).addOnSuccessListener {
                    updateFollowButton(true)
                    Toast.makeText(this, "Followed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

}
