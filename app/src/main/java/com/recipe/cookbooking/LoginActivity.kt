package com.recipe.cookbooking

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.recipe.cookbooking.Model.User
import com.google.android.gms.auth.api.signin.*
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.*
import com.google.firebase.database.FirebaseDatabase

class LoginActivity : AppCompatActivity() {
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var googleSignInButton: LinearLayout
    private lateinit var signUpTextView: TextView
    private lateinit var progressOverlay: LinearLayout
    private lateinit var progressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuth = FirebaseAuth.getInstance()
        FirebaseApp.initializeApp(this)

        emailEditText = findViewById(R.id.loginEmailInput)
        passwordEditText = findViewById(R.id.loginPasswordInput)
        loginButton = findViewById(R.id.signInButton)
        googleSignInButton = findViewById(R.id.buttonGoogleSignIn)
        signUpTextView = findViewById(R.id.loginSignUpButton)
        progressOverlay = findViewById(R.id.progressOverlay)
        progressText = findViewById(R.id.progressText)

        loginButton.setOnClickListener { loginWithEmail() }
        googleSignInButton.setOnClickListener { signInGoogle() }
        signUpTextView.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("PASTE_CLIENT_ID")
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

    }

    private fun showProgress(message: String) {
        progressText.text = message
        progressOverlay.visibility = LinearLayout.VISIBLE
    }

    private fun hideProgress() {
        progressOverlay.visibility = LinearLayout.GONE
    }

    private fun loginWithEmail() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString().trim()

        when {
            email.isEmpty() -> {
                emailEditText.error = "Email is required"
                return
            }

            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                emailEditText.error = "Invalid email format"
                return
            }
        }

        when {
            password.isEmpty() -> {
                passwordEditText.error = "Password is required"
                return
            }

            password.length < 6 -> {
                passwordEditText.error = "Password must be at least 6 characters"
                return
            }
        }

        showProgress("Signing you in...")

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = firebaseAuth.currentUser?.uid
                    if (userId != null) {
                        val userRef =
                            FirebaseDatabase.getInstance().getReference("users").child(userId)

                        // In loginWithEmail() method, modify the userRef.get().addOnSuccessListener block:
                        userRef.get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot.exists()) {
                                    val userData = snapshot.getValue(User::class.java)
                                    if (userData != null) {
                                        // Save all important user data to preferences
                                        SavedPreference.setEmail(this, userData.email)
                                        SavedPreference.setUsername(this, userData.fullName)
                                        SavedPreference.setProfilePic(this, userData.profilePic)

                                        Toast.makeText(
                                            this,
                                            "Login successful!",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        val intent = Intent(this, HomeActivity::class.java)
                                        intent.flags =
                                            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                    } else {
                                        hideProgress()
                                        Toast.makeText(
                                            this,
                                            "User data not found",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    hideProgress()
                                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                    }
                } else {
                    hideProgress()
                    Toast.makeText(
                        this,
                        "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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

    // Replace the updateUI method in LoginActivity with this to match RegisterActivity pattern:
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
                            // User data exists, preserve existing data
                            val existingData = snapshot.getValue(User::class.java)

                            // Create user data preserving existing fields
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

                            // Save to preferences
                            SavedPreference.setEmail(this, userData.email)
                            SavedPreference.setUsername(this, userData.fullName)
                            SavedPreference.setProfilePic(this, userData.profilePic)

                            // Update the user data
                            userRef.setValue(userData)
                                .addOnSuccessListener {
                                    hideProgress()
                                    Toast.makeText(
                                        this,
                                        "Google Sign-In successful!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val intent = Intent(this, HomeActivity::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    hideProgress()
                                    Toast.makeText(
                                        this,
                                        "Error saving user data: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            // User data doesn't exist, create new data
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

                            // Save to preferences
                            SavedPreference.setEmail(this, userData.email)
                            SavedPreference.setUsername(this, userData.fullName)
                            SavedPreference.setProfilePic(this, userData.profilePic)

                            // Save the new user data
                            userRef.setValue(userData)
                                .addOnSuccessListener {
                                    hideProgress()
                                    Toast.makeText(
                                        this,
                                        "Google Sign-In successful!",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val intent = Intent(this, HomeActivity::class.java)
                                    intent.flags =
                                        Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    hideProgress()
                                    Toast.makeText(
                                        this,
                                        "Error saving user data: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }.addOnFailureListener { e ->
                        hideProgress()
                        Toast.makeText(
                            this,
                            "Error checking user data: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                hideProgress()
                Toast.makeText(this, "Google Authentication failed", Toast.LENGTH_SHORT).show()
            }
        }
    }


    // Automatically logs in if the user is already signed in
    override fun onStart() {
        super.onStart()
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            startActivity(Intent(this, HomeActivity::class.java))
            finish()
        }
    }
}