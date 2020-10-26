package me.heizi.learning.contact.loging.ui.check

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.findNavController
import me.heizi.learning.contact.loging.MainActivity.Companion.mainActivity
import me.heizi.learning.contact.loging.R
import me.heizi.learning.contact.loging.databinding.CheckFragmentBinding
import kotlin.reflect.KProperty

class CheckFragment : Fragment() {


    companion object {
        private const val TAG = "CheckFragment"
    }
    private val viewModel: CheckViewModel by viewModels()
    lateinit var dataBinding : CheckFragmentBinding


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View  {
        dataBinding = DataBindingUtil.inflate(layoutInflater,R.layout.check_fragment,container,false)
        return dataBinding.root
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        dataBinding.viewModel = viewModel
        dataBinding.toolbarCheck.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
        viewModel.name.value = requireArguments()["name"].toString()
        viewModel.phone.value = requireArguments()["phone"].toString()

    }
//    operator fun <T> MutableLiveData<T>.setValue(thisObj:T,property:KProperty<*>,value:T) {
//        this.value=value
//    }

}