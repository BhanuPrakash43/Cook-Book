package com.recipe.cookbooking.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.recipe.cookbooking.R

class IngredientAdapter(private val ingredients: List<String>) :
    RecyclerView.Adapter<IngredientAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ingredientText: TextView = view.findViewById(R.id.ingredientItem)
        val ingredientStepNumber: TextView = view.findViewById(R.id.ingredientStepNumber)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ingredient, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.ingredientStepNumber.text = "${position + 1}."
        holder.ingredientText.text = ingredients[position]
    }

    override fun getItemCount() = ingredients.size
}
