package com.smeet.expencemanagement

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.switchmaterial.SwitchMaterial
import com.smeet.expencemanagement.model.ScheduledBill
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class ScheduledBillAdapter(
    private var billList: List<com.smeet.expencemanagement.model.ScheduledBill>,
    private val currencySymbol: String,
    private val onMarkAsPaid: (com.smeet.expencemanagement.model.ScheduledBill) -> Unit,
    private val onEditClick: (com.smeet.expencemanagement.model.ScheduledBill) -> Unit,
    private val onDeleteClick: (com.smeet.expencemanagement.model.ScheduledBill) -> Unit
) : androidx.recyclerview.widget.RecyclerView.Adapter<ScheduledBillAdapter.BillViewHolder>()  {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ScheduledBillAdapter.BillViewHolder {
        val view= LayoutInflater.from(parent.context).inflate(R.layout.item_scheduled_bill,parent,false)
        return BillViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: BillViewHolder,
        position: Int
    ) {
        val currentBill=billList[position]

        holder.category.text=currentBill.category
        holder.name.text=currentBill.billName

        val formateAmount=if(currentBill.amount%1==0.0){
            currentBill.amount.toInt().toString()
        }
        else{
            currentBill.amount.toString()
        }
        holder.amount.text="$currencySymbol$formateAmount"

        val today= System.currentTimeMillis()

        val calCurrent = Calendar.getInstance().apply { timeInMillis = today; set(Calendar.HOUR_OF_DAY, 0); set(
            Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val calDue= Calendar.getInstance().apply { timeInMillis = currentBill.dueDate; set(Calendar.HOUR_OF_DAY, 0); set(
            Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }

        val diffInMillis=calDue.timeInMillis-calCurrent.timeInMillis
        val daysDifference = TimeUnit.MILLISECONDS.toDays(diffInMillis)


        if(currentBill.isPaid){
            val sdf= SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            holder.statusBadge.text="Paid for ${sdf.format(Date(currentBill.dueDate))}"
        }
        else {
            when {
                daysDifference < 0 -> holder.statusBadge.text = "Overdue"
                daysDifference == 0L -> holder.statusBadge.text = "Due Today"
                daysDifference == 1L -> holder.statusBadge.text = "Due Tomorrow"
                else -> {
                    val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                    holder.statusBadge.text = "Due ${sdf.format(Date(currentBill.dueDate))}"
                }
            }
        }

        if (currentBill.isPaid) {
            holder.btnMarkPaid.text = "Paid"
            holder.btnMarkPaid.isEnabled = false
        } else {
            holder.btnMarkPaid.text = "Pay"
            holder.btnMarkPaid.isEnabled = true
        }

        holder.btnMarkPaid.setOnClickListener {
            if (!currentBill.isPaid) {
                onMarkAsPaid(currentBill)
            }
        }

        if(currentBill.isPaid){
            holder.iconMoreOptions.visibility=android.view.View.INVISIBLE
        }
        else {

            holder.iconMoreOptions.visibility = android.view.View.VISIBLE

            holder.iconMoreOptions.setOnClickListener { view ->
                val popup = android.widget.PopupMenu(view.context, holder.iconMoreOptions)

                popup.menu.add("Edit")
                popup.menu.add("Delete")

                popup.setOnMenuItemClickListener { menuItem ->
                    when (menuItem.title) {
                        "Edit" -> {
                            onEditClick(currentBill)
                            true
                        }

                        "Delete" -> {
                            onDeleteClick(currentBill)
                            true
                        }

                        else -> false
                    }
                }
                popup.show()
            }
        }
    }

    override fun getItemCount(): Int =billList.size

    fun updateData(newList: List<ScheduledBill>){

        val currentCalendar = Calendar.getInstance()
        val currentMonth = currentCalendar.get(Calendar.MONTH)
        val currentYear = currentCalendar.get(Calendar.YEAR)

        val filteredList=newList.filter { bill ->
            val billCalender= Calendar.getInstance().apply { timeInMillis=bill.dueDate }
            val billMonth = billCalender.get(Calendar.MONTH)
            val billYear = billCalender.get(Calendar.YEAR)

            (billYear < currentYear) || (billYear == currentYear && billMonth <= currentMonth)
        }

        billList=filteredList.toMutableList()
        notifyDataSetChanged()
    }

    inner class BillViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val category: TextView = itemView.findViewById(R.id.billCategory)
        val name: TextView = itemView.findViewById(R.id.billName)
        val amount: TextView = itemView.findViewById(R.id.billAmount)
        val statusBadge: TextView = itemView.findViewById(R.id.billStatusBadge)
        val btnMarkPaid: android.widget.Button = itemView.findViewById(R.id.btnMarkPaid)
        val iconMoreOptions: android.widget.ImageView = itemView.findViewById(R.id.iconMoreOptions)
    }
}