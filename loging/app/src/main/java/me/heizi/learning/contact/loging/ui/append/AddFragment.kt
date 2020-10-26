package me.heizi.learning.contact.loging.ui.append

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import me.heizi.learning.contact.loging.MainActivity.Companion.db
import me.heizi.learning.contact.loging.MainActivity.Companion.mainActivity
import me.heizi.learning.contact.loging.R
import me.heizi.learning.contact.loging.data.rooms.Phone
import me.heizi.learning.contact.loging.databinding.AddFragmentBinding

class AddFragment : Fragment() {

    companion object {
        private const val TAG = "AddFragment"
    }
    private val viewBinding :AddFragmentBinding by lazy {
        AddFragmentBinding.inflate(layoutInflater)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = viewBinding.root

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        viewBinding.toolbarAddFragment.run {
            setNavigationOnClickListener {
                exitFragment()
                Log.i(TAG, "onActivityCreated: clicked done")
            }
            menu.findItem(R.id.done_item_menu).setOnMenuItemClickListener {
                sendData()
                true
            }
        }

    }
    fun sendData() {
        val phone = viewBinding.phoneInput.editText!!.text
        val name  = viewBinding.nameInput.editText!!.text
        if (!phone.isNullOrEmpty() and !name.isNullOrEmpty()) {
            db.dataDao().insert(Phone(name = name.toString(),phone = phone.toString()))
            findNavController().navigateUp()
        }
    }
    fun exitFragment() {
        findNavController().navigateUp()
    }

}