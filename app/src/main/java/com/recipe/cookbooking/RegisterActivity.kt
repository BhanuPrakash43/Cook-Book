package com.recipe.cookbooking

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.recipe.cookbooking.Model.User
import com.recipe.cookbooking.utils.uploadImageToCloudinary
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase

class RegisterActivity : AppCompatActivity() {
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var profilePicInput: ImageView
    private lateinit var fullNameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var progressOverlay: LinearLayout
    private lateinit var progressText: TextView

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuth = FirebaseAuth.getInstance()
        profilePicInput = findViewById(R.id.profilePicInput)
        fullNameEditText = findViewById(R.id.registerNameInput)
        emailEditText = findViewById(R.id.registerEmailInput)
        passwordEditText = findViewById(R.id.registerPasswordInput)
        confirmPasswordEditText = findViewById(R.id.registerConfirmPasswordInput)
        registerButton = findViewById(R.id.signUpButton)
        progressOverlay = findViewById(R.id.progressOverlay)
        progressText = findViewById(R.id.progressText)

        registerButton.setOnClickListener {
            registerUser()
        }
        profilePicInput.setOnClickListener {
            openGallery()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("PASTE_CLIENT_ID")
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        val googleSignUpButton = findViewById<LinearLayout>(R.id.buttonGoogleSignUp)
        googleSignUpButton.setOnClickListener {
            signInGoogle()
        }

        val signUpLoginButton = findViewById<TextView>(R.id.signUpLoginButton)
        signUpLoginButton.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
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
            profilePicInput.setImageURI(imageUri)
        }
    }

    private fun showProgress(message: String) {
        progressText.text = message
        progressOverlay.visibility = LinearLayout.VISIBLE
    }

    private fun hideProgress() {
        progressOverlay.visibility = LinearLayout.GONE
    }

    private fun registerUser() {
        val fullName = fullNameEditText.text.toString().trim()
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        if (fullName.isEmpty()) {
            showToast("Full Name is required")
            return
        }
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showToast("Valid Email is required")
            return
        }
        if (password.isEmpty() || password.length < 6) {
            showToast("Password must be at least 6 characters")
            return
        }
        if (password != confirmPassword) {
            showToast("Passwords do not match")
            return
        }

        // Show progress before starting Firebase operations
        showProgress("Creating account...")

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        val userId = user.uid

                        if (imageUri != null) {
                            // Update progress message
                            progressText.text = "Uploading profile image..."

                            uploadImageToCloudinary(
                                this,
                                imageUri!!,
                                onSuccess = { imageUrl ->
                                    // After successful upload, save user data
                                    runOnUiThread {
                                        progressText.text = "Saving user data..."
                                    }
                                    saveUserData(userId, fullName, email, imageUrl)
                                },
                                onFailure = { errorMessage ->
                                    runOnUiThread {
                                        hideProgress()
                                        showToast("Image Upload Failed: $errorMessage")
                                    }
                                }
                            )
                        } else {
                            saveUserData(userId, fullName, email, "")
                        }
                    } else {
                        hideProgress()
                        showToast("User ID is null")
                    }
                } else {
                    hideProgress()
                    showToast("Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun saveUserData(
        userId: String,
        fullName: String,
        email: String,
        profilePicUrl: String
    ) {
        val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
        val userData = User(
            userId = userId,
            fullName = fullName,
            email = email,
            profilePic = profilePicUrl,
            bio = "",
            profession = "",
            followers = HashMap<String, Boolean>(),  // Empty map instead of 0L
            following = HashMap<String, Boolean>()
        )
        userRef.setValue(userData)
            .addOnSuccessListener {
                SavedPreference.setEmail(this, email)
                SavedPreference.setUsername(this, fullName)
                SavedPreference.setProfilePic(this, profilePicUrl)

                hideProgress()
                showToast("Registration successful!")
                goToHome()
            }
            .addOnFailureListener { e ->
                hideProgress()
                showToast("Error saving user data: ${e.message}")
            }
    }

    private fun signInGoogle() {
        showProgress("Authenticating with Google...")
        val signInIntent = mGoogleSignInClient.signInIntent
        signInLauncher.launch(signInIntent)
    }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val data = result.data
        if (result.resultCode == RESULT_OK && data != null) {
            val task: Task<GoogleSignInAccount> =
                GoogleSignIn.getSignedInAccountFromIntent(data)
            handleResult(task)
        } else {
            hideProgress()
            Toast.makeText(this, "Google Sign-In failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            account?.let { updateUI(it) }
        } catch (e: ApiException) {
            hideProgress()
            Toast.makeText(
                this, "Sign-in failed: ${e.localizedMessage}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateUI(account: GoogleSignInAccount) {
        val credential = GoogleAuthProvider.getCredential(account.idToken, null)

        // Update progress message
        progressText.text = "Signing in..."

        firebaseAuth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = firebaseAuth.currentUser
                if (user != null) {
                    val userId = user.uid
                    val profilePicUrl = account.photoUrl?.toString() ?: ""

                    // Check if user data already exists
                    val userRef = FirebaseDatabase.getInstance().getReference("users").child(userId)
                    userRef.get().addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            // User data exists, no need to overwrite
                            val existingData = snapshot.getValue(User::class.java)
                            // You can access existing fields like existingData?.bio, existingData?.followers, etc.

                            // Use the existing data to update the profile
                            val userData = User(
                                userId = userId,
                                fullName = existingData?.fullName ?: account.displayName ?: "",
                                email = existingData?.email ?: account.email ?: "",
                                profilePic = existingData?.profilePic ?: profilePicUrl,
                                bio = existingData?.bio ?: "",
                                profession = existingData?.profession ?: "",
                                followers = (existingData?.followers ?: 0L) as Map<String, Boolean>,
                                following = (existingData?.following ?: 0L) as Map<String, Boolean>
                            )

                            // Save the user data again if needed, for example, to update the profile picture
                            userRef.setValue(userData)
                                .addOnSuccessListener {
                                    hideProgress()
                                    showToast("Google Sign-Up successful!")
                                    goToHome()
                                }
                                .addOnFailureListener { e ->
                                    hideProgress()
                                    showToast("Error saving Google user data: ${e.message}")
                                }
                        } else {
                            // User data does not exist, create new data
                            val userData = User(
                                userId = userId,
                                fullName = account.displayName ?: "",
                                email = account.email ?: "",
                                profilePic = profilePicUrl,
                                bio = "",
                                profession = "",
                                followers = HashMap<String, Boolean>(),  // Empty map instead of 0L
                                following = HashMap<String, Boolean>()
                            )

                            // Save the new user data
                            userRef.setValue(userData)
                                .addOnSuccessListener {
                                    hideProgress()
                                    showToast("Google Sign-Up successful!")
                                    goToHome()
                                }
                                .addOnFailureListener { e ->
                                    hideProgress()
                                    showToast("Error saving Google user data: ${e.message}")
                                }
                        }
                    }.addOnFailureListener { e ->
                        hideProgress()
                        showToast("Error checking user data: ${e.message}")
                    }
                }
            } else {
                hideProgress()
                Toast.makeText(this, "Google Authentication failed", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun goToHome() {
        val intent = Intent(this@RegisterActivity, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
}