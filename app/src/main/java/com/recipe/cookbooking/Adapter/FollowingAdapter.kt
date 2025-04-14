package com.recipe.cookbooking.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
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

class FollowingAdapter(private val users: MutableList<User>) : RecyclerView.Adapter<FollowingAdapter.UserViewHolder>() {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user_following, parent, false)
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
            toggleFollow(user.userId, position, holder.itemView)
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

    private fun toggleFollow(targetUserId: String?, position: Int, view: View) {
        if (targetUserId == null || currentUserId == null) return

        val updates = hashMapOf<String, Any?>()
        val followingRef = database.child("users").child(currentUserId).child("following")

        followingRef.child(targetUserId).get().addOnSuccessListener { snapshot ->
            val isFollowing = snapshot.exists()

            if (isFollowing) {
                // Unfollow
                updates["/users/$currentUserId/following/$targetUserId"] = null
                updates["/users/$targetUserId/followers/$currentUserId"] = null

                database.updateChildren(updates).addOnSuccessListener {
                    users.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, users.size)
                    Toast.makeText(view.context, "Unfollowed", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Follow
                updates["/users/$currentUserId/following/$targetUserId"] = targetUserId
                updates["/users/$targetUserId/followers/$currentUserId"] = currentUserId

                database.updateChildren(updates).addOnSuccessListener {
                    notifyItemChanged(position)
                    Toast.makeText(view.context, "Followed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.userImage)
        val name: TextView = itemView.findViewById(R.id.userName)
        val followButton: Button = itemView.findViewById(R.id.followButton)
    }
}
