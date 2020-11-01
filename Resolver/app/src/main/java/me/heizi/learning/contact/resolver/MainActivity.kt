package me.heizi.learning.contact.resolver

import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import me.heizi.learning.contact.resolver.MainActivity.Companion.activity
import me.heizi.learning.contact.resolver.MainActivity.Companion.show
import me.heizi.learning.contact.resolver.MainTableNames.uriHeader
import me.heizi.learning.contact.resolver.databinding.ActivityMainBinding
import me.heizi.learning.contact.resolver.databinding.DialogInsertBinding
import me.heizi.learning.contact.resolver.databinding.SelectDropdownViewBinding

class MainActivity : AppCompatActivity() {
    init {
        activity = this
    }
    companion object {
        //private const val TAG = "MainActivity"

        lateinit var activity:MainActivity
        fun String.show(short:Boolean = false)=
                Toast.makeText(activity,this,if (short)Toast.LENGTH_SHORT else Toast.LENGTH_LONG).show()
    }

    val viewModel: me.heizi.learning.contact.resolver.ViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        activity = this
        super.onCreate(savedInstanceState)
        setContentView(viewModel.binding.root)
        viewModel.clickedListeners.invoke(viewModel.binding)
    }
}

fun String.toUri(): Uri = Uri.parse(this)
class ViewModel :ViewModel(){
    companion object {
        private const val TAG = "MainActivity"
    }
    val binding:ActivityMainBinding by lazy { ActivityMainBinding.inflate(activity.layoutInflater) }
    var clickedListeners :(ActivityMainBinding.()->Unit) = {
        create.setOnClickListener {
            it.visibility = View.INVISIBLE
            "此处不可创建SQLITE数据库".show()
        }

        insert.setOnClickListener {

            AlertDialog.Builder(activity).apply {
                val bindingDialog= DialogInsertBinding.inflate(activity.layoutInflater,null,false)
                setTitle("添加")
                setView(bindingDialog.root)
                setPositiveButton("确认") {_,_->
                    fun checkNotnull(): Boolean {
                        for (i in 0 until bindingDialog.root.childCount) {
                            if ((bindingDialog.root.getChildAt(i)as EditText).text.trim().isEmpty()) return false
                        }
                        return true
                    }
                    if (!checkNotnull())
                        "输入框有空，请重新输入".show()
                    else
                        activity.contentResolver.insert(Uri.parse("${uriHeader}/add"),ContentValues().apply { with(MainTableNames) { with(bindingDialog) {
                            put(Name_C, nameD.text.toString())
                            put(IDN_C, idnD.text.toString())
                            put(Major_C, majorD.text.toString())
                            put(Age_C, Integer.valueOf(ageD.text.toString()))
                        } } })
                }
            }.show()
        }
        operator fun Cursor.get(columnName: String):String?= this.getString(getColumnIndex(columnName))
        select.setOnClickListener {
            Log.i(TAG, "select: Onclicked")
            var text = "空"
            activity.contentResolver.query(Uri.parse("${uriHeader}/get"), null, null, null, null,)?.apply {
                val builder = StringBuilder()
                Log.i(TAG, "stat: on apply")
                while (moveToNext()) {
                    Log.i(TAG, "stat: move to next is true")
                    builder.apply {
                        append(
                                """ 
                            学生姓名:${get(MainTableNames.Name_C)}
                            年龄:${get(MainTableNames.Age_C)}
                            专业：${get(MainTableNames.Major_C)}
                            学号:${get(MainTableNames.IDN_C)}
                            

                            """.trimIndent()
                        )
                    }
                }
                text = builder.toString()
                Log.i(TAG, "text: $text")
                close()
                AlertDialog.Builder(activity).apply {
                    setTitle("结果")
                    setMessage(text)
                    setPositiveButton("确认", null)
                    show()
                }
            }
        }
        fun LayoutParams(weight: Int = LinearLayout.LayoutParams.MATCH_PARENT, height:Int = LinearLayout.LayoutParams.WRAP_CONTENT):LinearLayout.LayoutParams = LinearLayout.LayoutParams(weight,height);
        update.setOnClickListener {
            AlertDialog.Builder(activity).apply {
                setTitle("修改")
                val nameEditBefore = EditText(activity).apply {
                    layoutParams = LayoutParams()
                    text = null
                    hint = "需要修改的名字……"
                }
                val nameEditAfter = EditText(activity).apply {
                    layoutParams = LayoutParams()
                    text = null
                    hint = "改成……"
                }
                setView(LinearLayout(activity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LayoutParams()
                    addView(nameEditBefore)
                    addView(nameEditAfter)
                })
                setPositiveButton("确认") { _, _ ->
                    //判断edit_text非空后Update
                    if ((!nameEditBefore.text.isNullOrEmpty() and !nameEditAfter.text.isNullOrEmpty())) {
                        activity.contentResolver.update(
                            Uri.parse("${uriHeader}/update"),
                            ContentValues().apply {
                                put(
                                    MainTableNames.Name_C,
                                    nameEditAfter.text.trim().toString()
                                )
                            },
                            "${MainTableNames.Name_C}=?",
                            arrayOf(nameEditBefore.text.trim().toString())
                        ).let {
                            ("修改" + (if (it != 0) "成功" else "失败") + "。").show()
                        }
                    } else {
                        "请输入文字".show()
                    }
                }
                show()
            }
        }
        delect.setOnClickListener {
            AlertDialog.Builder(activity).apply {
                val nameEditText = EditText(activity).apply {
                    layoutParams = LayoutParams()
                    text = null
                    hint = "删除的学生信息的学号……"
                }
                setTitle("删除")
                setView(nameEditText)
                setPositiveButton("确认") { _,_->
                    if (!nameEditText.text.isNullOrEmpty()) {
                        activity.contentResolver.delete(Uri.parse("${uriHeader}/delete"),"${MainTableNames.IDN_C}=?", arrayOf(nameEditText.text.trim().toString())).let {
                            (if (it >0)"删除成功。" else "删除失败。").show()
                        }
                    }else{
                        "请输入学号！".show()
                    }
                }
                show()
            }
        }
        selectSingle.setOnClickListener {
            val map  = HashMap<String,String>()
            activity.contentResolver.query("$uriHeader/get/".toUri(),null,null,null,null)?.apply {
                while (moveToNext()){
                    val id = get("id")
                    val name = get("name")
                    Log.i(TAG, "$id: $name")
                    if (!id.isNullOrEmpty() and !name.isNullOrEmpty()) {
                        map[id!!] = name!!
                        Log.i(TAG, "map: $map")
                    }
                }
            }
            if (map.isEmpty()) {
                "无数据，不允许修改。".show()
            }else {
                Log.i(TAG, "values: ${map.keys.toString()}")
                SelectDropdownViewBinding.inflate(activity.layoutInflater,null,false).apply {
                    dropdownInputSelect.setAdapter(ArrayAdapter(activity,R.layout.list_item,map.values.toList()))
                    AlertDialog.Builder(activity).setView(root).setPositiveButton("确认") {_,_ ->
                        activity.contentResolver.query("$uriHeader/get/${map.keys.toList()[map.values.indexOf(dropdownInputSelect.text!!.toString())]}".toUri(),null,null,null,null)?.apply {
                            while (moveToNext()) {
                                this["major"]?.show()
                            }
                        }
                    }.setMessage("插入").show()
                }
            }
        }
    }

}
object MainTableNames {
    const val uriHeader = "content://me.heizi.learning.contact.provider"
    const val TABLE_NAME = "student"
    const val ID_C = "id"
    const val Name_C="name"
    const val IDN_C = "idn"
    const val Age_C = "age"
    const val Major_C= "major"
    const val CREATOR = "create table $TABLE_NAME(" +
            "$ID_C integer primary key autoincrement ," +
            "$IDN_C text,$Name_C text,$Age_C integer,$Major_C text);"
}
