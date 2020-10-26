package me.heizi.learning.contact.loging.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import androidx.recyclerview.widget.RecyclerView
import me.heizi.learning.contact.loging.MainActivity.Companion.db
import me.heizi.learning.contact.loging.MainActivity.Companion.mainActivity
import me.heizi.learning.contact.loging.R
import me.heizi.learning.contact.loging.data.rooms.Phone
import me.heizi.learning.contact.loging.databinding.ItemMainRcvBinding

class MainViewModel : ViewModel() {
    var layoutInflater:LayoutInflater? =null
    var navController: NavController? = null
    var first = true
    val adapter by lazy { Adapter() }

     inner class Adapter(): RecyclerView.Adapter<Adapter.ViewHolder>() {
        val datas:List<Phone> get() = db.dataDao().all

        inner class ViewHolder(val binding:ItemMainRcvBinding):RecyclerView.ViewHolder(binding.root)
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(ItemMainRcvBinding.inflate(layoutInflater!!,parent,false))
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val data = datas[position]
            data.name.let { with(holder.binding) {
                fullNameS.text  = it
                singleName.text = it[0].toString()
            } }
            holder.binding.root.run {
                setOnClickListener {
                    navController!!.navigate(R.id.action_mainFragment_to_checkFragment, bundleOf(Pair("name",data.name), Pair("phone",data.phone)))
                }
                setOnLongClickListener {
                    AlertDialog.Builder(mainActivity).apply {
                        setTitle("删除")
                        setMessage("你确认要删除？")
                        setPositiveButton("确认") {_,_->
                            if(db.dataDao().delete(data) > 0) {
                                Toast.makeText(mainActivity,"成功",Toast.LENGTH_SHORT).show()
                                this@Adapter.notifyDataSetChanged()
                            }
                        }
                    }.show()
                    true
                }
            }
        }

        override fun getItemCount(): Int = datas.size
    }
}