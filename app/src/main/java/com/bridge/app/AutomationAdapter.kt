package com.bridge.app

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AutomationAdapter(
    private val automations: List<Automation>,
    private val onDelete: (Automation) -> Unit
) : RecyclerView.Adapter<AutomationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val triggerText: TextView = view.findViewById(R.id.triggerText)
        val actionText: TextView = view.findViewById(R.id.actionText)
        val deleteButton: Button = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_automation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val automation = automations[position]
        holder.triggerText.text = "When: ${automation.trigger}"
        holder.actionText.text = "Do: ${automation.action}"
        holder.deleteButton.setOnClickListener { onDelete(automation) }
    }

    override fun getItemCount() = automations.size
}
