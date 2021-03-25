package com.example.bluetoothdemo

import android.app.ActionBar
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 *
 * @ProjectName:    BluetoothDemo
 * @Package:        com.example.bluetoothdemo
 * @ClassName:      BluetoothAdapter
 * @Description:     java类作用描述
 * @Author:         作者名
 * @CreateDate:     2021/3/25 15:56
 * @UpdateUser:     更新者：
 * @UpdateDate:     2021/3/25 15:56
 * @UpdateRemark:   更新说明：
 * @Version:        1.0
 */
class BluetoothAdapter : RecyclerView.Adapter<BluetoothAdapter.BluetoothViewHolder>() {


    var names: MutableList<String> = mutableListOf()

    private var onItemClickListener: ((name: String) -> Unit?)? = null

    fun setData(name: String) {
        names.add(name)
        notifyDataSetChanged()
    }

    fun clearData() {
        names.clear()
        notifyDataSetChanged()
    }

    fun setOnAdapterItemClickListener(onItemClickListener: (name: String) -> Unit) {
        this.onItemClickListener = onItemClickListener
    }

    class BluetoothViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BluetoothViewHolder {
        val textView = TextView(parent.context)
        val layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        textView.layoutParams = layoutParams
        return BluetoothViewHolder(textView)
    }

    override fun onBindViewHolder(holder: BluetoothViewHolder, position: Int) {
        val textView = holder.itemView as TextView
        textView.text = names[position]
        onItemClickListener?.invoke(names[position])
    }

    override fun getItemCount(): Int = if (names.isNullOrEmpty()) {
        0
    } else {
        names.size
    }
}