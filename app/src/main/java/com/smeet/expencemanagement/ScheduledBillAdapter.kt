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
    private var billsList: MutableList<ScheduledBill>,
    private val currencySymbole: String,
    private val onMarkAsPaid:(ScheduledBill) -> Unit
): RecyclerView.Adapter<ScheduledBillAdapter.BillViewHolder>() {
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
        val currentBill=billsList[position]

        holder.category.text=currentBill.category
        holder.name.text=currentBill.billName

        val formateAmount=if(currentBill.amount%1==0.0){
            currentBill.amount.toInt().toString()
        }
        else{
            currentBill.amount.toString()
        }
        holder.amount.text="$currencySymbole$formateAmount"

        val today= System.currentTimeMillis()

        val calCurrent = Calendar.getInstance().apply { timeInMillis = today; set(Calendar.HOUR_OF_DAY, 0); set(
            Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val calDue= Calendar.getInstance().apply { timeInMillis = currentBill.dueDate; set(Calendar.HOUR_OF_DAY, 0); set(
            Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }

        val diffInMillis=calDue.timeInMillis-calCurrent.timeInMillis
        val daysDifference = TimeUnit.MILLISECONDS.toDays(diffInMillis)

        when {
            daysDifference < 0 -> holder.statusBadge.text = "Overdue"
            daysDifference == 0L -> holder.statusBadge.text = "Due Today"
            daysDifference == 1L -> holder.statusBadge.text = "Due Tomorrow"
            else -> {
                val sdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                holder.statusBadge.text = "Due ${sdf.format(Date(currentBill.dueDate))}"
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
    }

    override fun getItemCount(): Int =billsList.size

    fun updateData(newList: List<ScheduledBill>){
        billsList=newList.toMutableList()
        notifyDataSetChanged()
    }

    inner class BillViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val category: TextView = itemView.findViewById(R.id.billCategory)
        val name: TextView = itemView.findViewById(R.id.billName)
        val amount: TextView = itemView.findViewById(R.id.billAmount)
        val statusBadge: TextView = itemView.findViewById(R.id.billStatusBadge)
        val btnMarkPaid: android.widget.Button = itemView.findViewById(R.id.btnMarkPaid)
    }
}