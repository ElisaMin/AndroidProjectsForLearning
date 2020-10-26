package me.heizi.learning.contact.loging.ui.main

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import me.heizi.learning.contact.loging.MainActivity.Companion.db
import me.heizi.learning.contact.loging.MainActivity.Companion.mainActivity
import me.heizi.learning.contact.loging.R
import me.heizi.learning.contact.loging.databinding.MainFragmentBinding

class MainFragment : Fragment() {




    val viewModel: MainViewModel by viewModels()

    private val viewBinding : MainFragmentBinding by lazy {
        MainFragmentBinding.inflate(layoutInflater)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = viewBinding.root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel.layoutInflater = layoutInflater
        viewModel.navController  = findNavController()
        if (viewModel.first) {
            viewBinding.rcvMain.let {
                it.adapter=viewModel.adapter
                it.layoutManager = LinearLayoutManager(mainActivity)
                viewModel.first=false
            }
        }else {
            viewBinding.rcvMain.adapter!!.notifyDataSetChanged()
        }
        viewBinding.floatingActionButton.setOnClickListener {
            findNavController().navigate(R.id.addNewOne)
        }



    }

}