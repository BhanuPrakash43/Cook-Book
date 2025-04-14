package com.recipe.cookbooking

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.recipe.cookbooking.Adapter.UserRecipeAdapter
import com.recipe.cookbooking.Model.Recipe
import com.recipe.cookbooking.Model.User
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.GenericTypeIndicator
import com.google.firebase.database.ValueEventListener
import kotlin.jvm.java

class UserProfileActivity : AppCompatActivity() {
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var rvUserRecipes: RecyclerView
    private val recipeList = mutableListOf<Recipe>()
    private lateinit var adapter: UserRecipeAdapter
    private lateinit var tvName: TextView
    private lateinit var tvBio: TextView
    private lateinit var tvProfession: TextView
    private lateinit var profileImage: ImageView
    private lateinit var addRecipeButton: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_user_profile)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val userMoreIcon = findViewById<ImageView>(R.id.profileMoreButton)
        val navProfile = findViewById<LinearLayout>(R.id.nav_profile)
        val navHome = findViewById<LinearLayout>(R.id.nav_home)
        val navSavedRecipe = findViewById<LinearLayout>(R.id.nav_bookmark)
        val navSearch = findViewById<LinearLayout>(R.id.nav_search)
        addRecipeButton = findViewById(R.id.fab_add_recipe)

        firebaseAuth = FirebaseAuth.getInstance()

        tvName = findViewById(R.id.tvName)
        tvBio = findViewById(R.id.tvBio)
        tvProfession = findViewById(R.id.tvProfession)
        profileImage = findViewById(R.id.userProfileImage)
        rvUserRecipes = findViewById(R.id.rvUserRecipes)

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

        findViewById<TextView>(R.id.tvFollowers).setOnClickListener {
            val intent = Intent(this, FollowersListActivity::class.java)
            intent.putExtra("userId", FirebaseAuth.getInstance().currentUser!!.uid)
            intent.putExtra("type", "followers")
            startActivity(intent)
        }

        findViewById<TextView>(R.id.tvFollowing).setOnClickListener {
            val intent = Intent(this, FollowingListActivity::class.java)
            intent.putExtra("userId", FirebaseAuth.getInstance().currentUser!!.uid)
            intent.putExtra("type", "following")
            startActivity(intent)
        }


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("PASTE_CLIENT_ID")
            .requestEmail()
            .build()
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        userMoreIcon.setOnClickListener {
            val popup = PopupMenu(this, userMoreIcon)
            popup.menuInflater.inflate(R.menu.user_action_menu, popup.menu)
            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.logout_menu -> {
                        SavedPreference.clearUserData(this)
                        firebaseAuth.signOut()

                        mGoogleSignInClient.signOut().addOnCompleteListener {
                            Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                        true
                    }

                    R.id.update_profile -> {
                        showUpdateProfileDialog()
                        true
                    }

                    else -> false
                }
            }
            popup.show()
        }

        setupRecyclerView()
        loadUserData()
        loadUserRecipes()

    }

    private fun loadUserData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        userRef.get().addOnSuccessListener { snapshot ->
            try {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    tvName.text = it.fullName
                    tvBio.text = it.bio.takeIf { it.isNotEmpty() } ?: "No bio available"
                    tvProfession.text =
                        it.profession.takeIf { it.isNotEmpty() } ?: "No profession listed"

                    // Get followers count - handling both formats
                    val followersCount = getCount(snapshot.child("followers"))
                    findViewById<TextView>(R.id.tvFollowers).text = followersCount.toString()

                    // Get following count - handling both formats
                    val followingCount = getCount(snapshot.child("following"))
                    findViewById<TextView>(R.id.tvFollowing).text = followingCount.toString()

                    Glide.with(this)
                        .load(it.profilePic)
                        .placeholder(R.drawable.profile_image)
                        .into(profileImage)
                }
            } catch (e: Exception) {
                // Fallback in case the model doesn't match database
                val name = snapshot.child("fullName").getValue(String::class.java) ?: ""
                val bio = snapshot.child("bio").getValue(String::class.java) ?: ""
                val profession = snapshot.child("profession").getValue(String::class.java) ?: ""
                val profilePic = snapshot.child("profilePic").getValue(String::class.java) ?: ""

                tvName.text = name
                tvBio.text = bio.takeIf { it.isNotEmpty() } ?: "No bio available"
                tvProfession.text = profession.takeIf { it.isNotEmpty() } ?: "No profession listed"

                // Get followers and following counts
                val followersSnapshot = snapshot.child("followers")
                val followersCount = followersSnapshot.childrenCount.toInt()
                findViewById<TextView>(R.id.tvFollowers).text = followersCount.toString()

                val followingSnapshot = snapshot.child("following")
                val followingCount = followingSnapshot.childrenCount.toInt()
                findViewById<TextView>(R.id.tvFollowing).text = followingCount.toString()

                Glide.with(this)
                    .load(profilePic)
                    .placeholder(R.drawable.profile_image)
                    .into(profileImage)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load user info", Toast.LENGTH_SHORT).show()
        }
    }

    // Helper function to get count from either a Long or a Map
    private fun getCount(snapshot: DataSnapshot): Int {
        return try {
            // First try to get as Long
            val longValue = snapshot.getValue(Long::class.java)
            if (longValue != null) {
                return longValue.toInt()
            }

            // If not a Long, try to get as Map and count entries
            val mapValue = snapshot.getValue(object : GenericTypeIndicator<Map<String, Any>>() {})
            mapValue?.size ?: 0
        } catch (e: Exception) {
            // If all else fails, count children (works for both formats)
            snapshot.childrenCount.toInt()
        }
    }

    private fun showUpdateProfileDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_profile, null)
        val editBio = dialogView.findViewById<EditText>(R.id.editBio)
        val editProfession = dialogView.findViewById<EditText>(R.id.editProfession)
        val btnUpdate = dialogView.findViewById<MaterialButton>(R.id.btnUpdateProfile)

        // Prefill current values
        editBio.setText(tvBio.text.toString().takeIf { it != "No bio available" } ?: "")
        editProfession.setText(tvProfession.text.toString().takeIf { it != "No profession listed" } ?: "")

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnUpdate.setOnClickListener {
            val newBio = editBio.text.toString().trim()
            val newProfession = editProfession.text.toString().trim()

            if (newBio.isEmpty() && newProfession.isEmpty()) {
                Toast.makeText(this, "Nothing to update", Toast.LENGTH_SHORT).show()
            } else {
                updateUserProfile(newBio, newProfession) {
                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }

    private fun updateUserProfile(newBio: String, newProfession: String, onSuccess: () -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)

        val updates = mapOf(
            "bio" to newBio,
            "profession" to newProfession
        )

        userRef.updateChildren(updates).addOnSuccessListener {
            tvBio.text = newBio.ifEmpty { "No bio available" }
            tvProfession.text = newProfession.ifEmpty { "No profession listed" }
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            onSuccess()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to update profile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupRecyclerView() {
        adapter = UserRecipeAdapter(recipeList) { recipe ->
            deleteRecipeFromDatabase(recipe)
        }
        rvUserRecipes.layoutManager = LinearLayoutManager(this)
        rvUserRecipes.adapter = adapter
    }

    private fun loadUserRecipes() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userId = currentUser.uid
        val recipeRef = FirebaseDatabase.getInstance().getReference("Recipes")

        recipeRef.orderByChild("authorId").equalTo(userId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    recipeList.clear()

                    for (child in snapshot.children) {
                        val recipe = child.getValue(Recipe::class.java)
                        recipe?.let { recipeList.add(it) }
                    }

                    recipeList.sortByDescending { it.timestamp }
                    adapter.notifyDataSetChanged()

                    // Update count AFTER loading recipes
                    findViewById<TextView>(R.id.tvRecipes).text = recipeList.size.toString()

                    // Show or hide the empty message
                    val emptyStateContainer = findViewById<LinearLayout>(R.id.emptyStateContainer)
                    val fadeIn =
                        AnimationUtils.loadAnimation(this@UserProfileActivity, R.anim.fade_in)

                    if (recipeList.isEmpty()) {
                        emptyStateContainer.visibility = View.VISIBLE
                        emptyStateContainer.startAnimation(fadeIn)
                    } else {
                        emptyStateContainer.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@UserProfileActivity,
                        "Failed to load recipes",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun deleteRecipeFromDatabase(recipe: Recipe) {
        val recipeRef = FirebaseDatabase.getInstance().getReference("Recipes")
        recipeRef.child(recipe.id).removeValue()
            .addOnSuccessListener {
                adapter.removeItem(recipe)
                Toast.makeText(this, "Recipe deleted", Toast.LENGTH_SHORT).show()
                findViewById<TextView>(R.id.tvRecipes).text = recipeList.size.toString()

                if (recipeList.isEmpty()) {
                    findViewById<LinearLayout>(R.id.emptyStateContainer).visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to delete recipe", Toast.LENGTH_SHORT).show()
            }
    }

}