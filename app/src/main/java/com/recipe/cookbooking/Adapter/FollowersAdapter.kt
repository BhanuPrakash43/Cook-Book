package com.recipe.cookbooking.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.recipe.cookbooking.Model.User
import com.recipe.cookbooking.R

class FollowersAdapter(private val users: MutableList<User>) :
    RecyclerView.Adapter<FollowersAdapter.UserViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_user_follow, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]

        holder.name.text = user.fullName
        Glide.with(holder.itemView.context)
            .load(user.profilePic)
            .placeholder(R.drawable.profile_image)
            .into(holder.image)

        updateFollowButton(holder.followButton, user.userId)

        holder.followButton.setOnClickListener {
            toggleFollow(holder.followButton, user.userId)
        }

        holder.removeFollower.setOnClickListener {
            removeFollower(user.userId, position, holder.itemView)
        }
    }

    override fun getItemCount(): Int = users.size

    private fun updateFollowButton(button: Button, userId: String?) {
        if (userId == null || userId == currentUserId) {
            button.visibility = View.GONE
            return
        }

        val followRef = database.child("users").child(currentUserId!!).child("following")
        followRef.child(userId).get().addOnSuccessListener { snapshot ->
            val isFollowing = snapshot.exists()
            button.text = if (isFollowing) "Unfollow" else "Follow"
            button.backgroundTintList = ContextCompat.getColorStateList(
                button.context, if (isFollowing) R.color.gray2 else R.color.primary100
            )
        }
    }

    private fun toggleFollow(button: Button, targetUserId: String?) {
        if (targetUserId == null || currentUserId == null) return

        val updates = hashMapOf<String, Any?>()
        val followingRef = database.child("users").child(currentUserId).child("following")

        followingRef.child(targetUserId).get().addOnSuccessListener { snapshot ->
            val isFollowing = snapshot.exists()

            if (isFollowing) {
                updates["/users/$currentUserId/following/$targetUserId"] = null
                updates["/users/$targetUserId/followers/$currentUserId"] = null
            } else {
                updates["/users/$currentUserId/following/$targetUserId"] = targetUserId
                updates["/users/$targetUserId/followers/$currentUserId"] = currentUserId
            }

            database.updateChildren(updates).addOnSuccessListener {
                updateFollowButton(button, targetUserId)
            }
        }
    }

    private fun removeFollower(followerId: String?, position: Int, view: View) {
        if (followerId == null || currentUserId == null) return

        val updates = hashMapOf<String, Any?>()
        updates["/users/$currentUserId/followers/$followerId"] = null
        updates["/users/$followerId/following/$currentUserId"] = null

        database.updateChildren(updates).addOnSuccessListener {
            users.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, users.size)

            Toast.makeText(view.context, "Follower removed", Toast.LENGTH_SHORT).show()
        }

    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.userImage)
        val name: TextView = itemView.findViewById(R.id.userName)
        val followButton: Button = itemView.findViewById(R.id.followButton)
        val removeFollower: ImageButton = itemView.findViewById(R.id.removeFollower)
    }
}
