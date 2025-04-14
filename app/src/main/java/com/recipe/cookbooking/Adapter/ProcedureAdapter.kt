package com.recipe.cookbooking.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.recipe.cookbooking.R

class ProcedureAdapter(private val procedures: List<String>) :
    RecyclerView.Adapter<ProcedureAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val procedureText: TextView = view.findViewById(R.id.procedureItem)
        val procedureStepNumber: TextView = view.findViewById(R.id.stepCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_procedure, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.procedureStepNumber.text = "${position + 1}."
        holder.procedureText.text = procedures[position]

    }

    override fun getItemCount() = procedures.size
}
