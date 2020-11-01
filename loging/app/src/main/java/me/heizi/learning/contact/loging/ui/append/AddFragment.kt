package me.heizi.learning.contact.loging.ui.append

import android.Manifest
import android.content.ContentProviderOperation
import android.content.OperationApplicationException
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.os.RemoteException
import android.provider.ContactsContract
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.navigateUp
import me.heizi.learning.contact.loging.MainActivity.Companion.db
import me.heizi.learning.contact.loging.MainActivity.Companion.mainActivity
import me.heizi.learning.contact.loging.MainActivity.Companion.sharedViewModel
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
            val phones by lazy { Phone(name = name.toString(),phone = phone.toString()) }
            if (sharedViewModel.isSystemContact) {
                sendBySystem(phones)
            }else {
                db.dataDao().insert(phones)
            }

            exitFragment()
        }
    }
    fun exitFragment() {
        findNavController().navigateUp()
    }
    fun sendBySystem(phone: Phone) {
        val name = phone.name
        val number = phone.phone
        //抄来的
        val contentProviderOperations:ArrayList<ContentProviderOperation> = arrayListOf(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI).withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null).withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null).build(),ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name).build(), ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI).withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0).withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE).withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number).withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE).build())
        try {
            if ( mainActivity checkAndAskPms Manifest.permission.WRITE_CONTACTS ) {
                mainActivity.contentResolver.applyBatch(ContactsContract.AUTHORITY,contentProviderOperations)
            }else {
                "错误！无权限！".show()
            }
        } catch (e: RemoteException) {
            e.message?.show()
            e.printStackTrace()
        } catch (e: OperationApplicationException) {
            e.message?.show()
            e.printStackTrace()
        }
    }
    fun String.show() = Toast.makeText(mainActivity,this,Toast.LENGTH_SHORT).show()



}