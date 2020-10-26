package me.heizi.learning.contact.loging.ui.check

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class CheckViewModel : ViewModel() {
    val name:MutableLiveData<String> = MutableLiveData()
    val phone:MutableLiveData<String> = MutableLiveData("DefaultValue")
}