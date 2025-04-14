package com.recipe.cookbooking

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import com.recipe.cookbooking.Adapter.FollowersAdapter
import com.recipe.cookbooking.Model.User

class FollowersListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoFollower: TextView
    private lateinit var adapter: FollowersAdapter
    private val userList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_followers_list)

        val followersCloseButton = findViewById<ImageView>(R.id.followersCloseButton)
        recyclerView = findViewById(R.id.recyclerView)
        tvNoFollower = findViewById(R.id.tv_no_followers)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FollowersAdapter(userList)
        recyclerView.adapter = adapter

        val userId = intent.getStringExtra("userId") ?: return
        val isFollowers = intent.getBooleanExtra("isFollowers", true)

        followersCloseButton.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        loadUsers(userId, isFollowers)
    }

    private fun loadUsers(userId: String, isFollowers: Boolean) {
        val relationType = if (isFollowers) "followers" else "following"
        val ref = FirebaseDatabase.getInstance().getReference("users/$userId/$relationType")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ids = snapshot.children.mapNotNull { it.key }

                if (ids.isEmpty()) {
                    showEmptyMessage()
                } else {
                    fetchUserDetails(ids)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@FollowersListActivity,
                    "Failed to load $relationType",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun fetchUserDetails(userIds: List<String>) {
        val userRef = FirebaseDatabase.getInstance().getReference("users")

        userList.clear()
        var loadedCount = 0

        for (id in userIds) {
            userRef.child(id).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let { userList.add(it) }

                    loadedCount++
                    if (loadedCount == userIds.size) {
                        adapter.notifyDataSetChanged()
                        if (userList.isEmpty()) {
                            showEmptyMessage()
                        } else {
                            showList()
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    loadedCount++
                    if (loadedCount == userIds.size && userList.isEmpty()) {
                        showEmptyMessage()
                    }
                }
            })
        }
    }

    private fun showEmptyMessage() {
        tvNoFollower.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun showList() {
        tvNoFollower.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
}
