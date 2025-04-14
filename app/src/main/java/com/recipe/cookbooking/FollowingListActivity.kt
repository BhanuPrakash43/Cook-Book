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
import com.recipe.cookbooking.Adapter.FollowingAdapter
import com.recipe.cookbooking.Model.User

class FollowingListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var tvNoFollowing: TextView
    private lateinit var adapter: FollowingAdapter
    private val userList = mutableListOf<User>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_following_list)

        val followingCloseButton = findViewById<ImageView>(R.id.followingCloseButton)
        recyclerView = findViewById(R.id.recyclerView)
        tvNoFollowing = findViewById(R.id.tv_no_following)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = FollowingAdapter(userList)
        recyclerView.adapter = adapter

        val userId = intent.getStringExtra("userId") ?: return

        followingCloseButton.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        loadFollowing(userId)
    }

    private fun loadFollowing(userId: String) {
        val followingRef = FirebaseDatabase.getInstance().getReference("users/$userId/following")

        followingRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val followingIds = snapshot.children.mapNotNull { it.key }

                if (followingIds.isEmpty()) {
                    showEmptyMessage()
                } else {
                    fetchUserDetails(followingIds)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@FollowingListActivity, "Failed to load following", Toast.LENGTH_SHORT).show()
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
        tvNoFollowing.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }

    private fun showList() {
        tvNoFollowing.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
}
