package com.smeet.expencemanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.smeet.expencemanagement.model.Expence

class ExpenseAdapter(
    private var expenseList: MutableList<Expence>,
    private val currencySymbole: String,
    private val onActionClick: (Expence,String) -> Unit
) : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_expence, parent, false)
        return ExpenseViewHolder(view)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val currentExpense = expenseList[position]
        holder.category.text = currentExpense.category
        holder.name.text = currentExpense.note
        holder.amount.text = "- $currencySymbole${currentExpense.amount}"
        val formatter = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
        val formattedDateString = formatter.format(java.util.Date(currentExpense.date))
        holder.date.text = formattedDateString

        holder.optionButton.setOnClickListener { view ->
            val popupMenu=android.widget.PopupMenu(view.context,view)
            popupMenu.inflate(R.menu.expense_options_menu)

            popupMenu.setOnMenuItemClickListener { menuItem ->
                when(menuItem.itemId){
                    R.id.action_edit -> {
                        onActionClick(currentExpense,"EDIT")
                        true
                    }
                    R.id.action_delete->{
                        onActionClick(currentExpense,"DELETE")
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
    }

    override fun getItemCount(): Int {
        return expenseList.size
    }

    inner class ExpenseViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
        val category = itemView.findViewById<TextView>(R.id.expenseCategory)
        val name = itemView.findViewById<TextView>(R.id.expenseName)
        val date = itemView.findViewById<TextView>(R.id.expenseDate)
        val amount = itemView.findViewById<TextView>(R.id.expenseAmount)
        val optionButton=itemView.findViewById<ImageView>(R.id.iconMoreOptions)
    }

    fun updateData(newList: List<Expence>) {
        expenseList = newList.toMutableList()
        notifyDataSetChanged()
    }
}