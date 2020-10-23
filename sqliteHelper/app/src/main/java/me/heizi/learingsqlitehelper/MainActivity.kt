package me.heizi.learingsqlitehelper

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.databinding.DialogInsertBinding
import java.lang.Exception
import kotlin.text.StringBuilder

class MainActivity : AppCompatActivity() {
    //轮子
    fun LayoutParams(weight: Int = LinearLayout.LayoutParams.MATCH_PARENT, height:Int = LinearLayout.LayoutParams.WRAP_CONTENT):LinearLayout.LayoutParams = LinearLayout.LayoutParams(weight,height);
    operator fun Cursor.get(columnName: String):String?= this.getString(getColumnIndex(columnName))
    fun makeToast(text:String,long:Boolean = false) = Toast.makeText(this,text,if (long) Toast.LENGTH_LONG else Toast.LENGTH_SHORT).show()

    companion object {
        private const val TAG = "MainActivity"
    }
    private val binding:ActivityMainBinding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private val helperMain = SQLiteHelper(this,"main",null,1)



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.create.setOnClickListener {
            try {
                helperMain.writableDatabase
                makeToast(if (!helperMain.isCreatedOrUpdatedBefore) "创建成功" else "别点了！早就创建过了！谢谢！")
            }catch (e:Exception){
                makeToast(e.stackTraceToString())
                e.printStackTrace()
            }
        }

        binding.insert.setOnClickListener {
            AlertDialog.Builder(this).apply {
                val bindingDialog=DialogInsertBinding.inflate(layoutInflater,null,false)
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
                        makeToast("输入框有空，请重新输入")
                    else
                        helperMain.writableDatabase.insert(MainTableNames.TABLE_NAME, null, ContentValues().apply { with(MainTableNames) { with(bindingDialog) {
                            put(Name_C, nameD.text.toString())
                            put(IDN_C, idnD.text.toString())
                            put(Major_C, majorD.text.toString())
                            put(Age_C, Integer.valueOf(ageD.text.toString()))
                        } } }).let {
                            if (it > 0) makeToast("插入成功")
                        }
                }
                show()
            }
        }

        binding.select.setOnClickListener {
            var text = "空"
            helperMain.writableDatabase.query(MainTableNames.TABLE_NAME,null,null,null,null,null,null).apply {
                if (moveToFirst()) {
                    val builder = StringBuilder()
                    do { builder.apply {
                        append(""" 
                            学生姓名:${get(MainTableNames.Name_C)}
                            年龄:${get(MainTableNames.Age_C)}
                            专业：${get(MainTableNames.Major_C)}
                            学号:${get(MainTableNames.IDN_C)}
                            

                            """.trimIndent())
                    } }while (moveToNext())
                    text= builder.toString()
                }
                close()
                AlertDialog.Builder(this@MainActivity).apply {
                    setTitle("结果")
                    setMessage(text)
                    setPositiveButton("确认",null)
                    show()
                }
            }
        }

        binding.update.setOnClickListener {
            AlertDialog.Builder(this).apply {
                setTitle("修改")
                val nameEditBefore= EditText(this@MainActivity).apply {
                    layoutParams = LayoutParams()
                    text = null
                    hint = "需要修改的名字……"
                }
                val nameEditAfter = EditText(this@MainActivity).apply {
                    layoutParams = LayoutParams()
                    text = null
                    hint = "改成……"
                }
                setView(LinearLayout( this@MainActivity).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LayoutParams()
                    addView(nameEditBefore)
                    addView(nameEditAfter)
                })
                setPositiveButton("确认"){_,_->
                    //判断edit_text非空后Update
                    if ((!nameEditBefore.text.isNullOrEmpty() and !nameEditAfter.text.isNullOrEmpty())) {
                        Log.i(TAG, "onCreate: not null")
                        helperMain.writableDatabase.update(
                            MainTableNames.TABLE_NAME,
                            ContentValues().apply { put(MainTableNames.Name_C,nameEditAfter.text.trim().toString()) },
                            "${MainTableNames.Name_C}=?",
                            arrayOf(nameEditBefore.text.trim().toString())
                        ).let {
                            makeToast("修改"+ (if (it !=0) "成功" else "失败") +"。")
                        }
                    }else{
                        makeToast("请输入文字")
                    }
                }
                show()
            }
        }
        binding.delect.setOnClickListener {
            AlertDialog.Builder(this).apply {
                val nameEditText = EditText(this@MainActivity).apply {
                    layoutParams = LayoutParams()
                    text = null
                    hint = "删除的学生信息的学号……"
                }
                setTitle("删除")
                setView(nameEditText)
                setPositiveButton("确认") { _,_->
                    if (!nameEditText.text.isNullOrEmpty()) {
                        helperMain.writableDatabase.delete(MainTableNames.TABLE_NAME,"${MainTableNames.IDN_C}=?", arrayOf(nameEditText.text.trim().toString())).let {
                            makeToast(if (it >0)"删除成功。" else "删除失败。")
                        }
                    }else{
                        makeToast("请输入学号！")
                    }
                }
                show()
            }
        }
        // end
    }
}

class SQLiteHelper(context:Context,name:String,factory:SQLiteDatabase.CursorFactory?,version:Int):SQLiteOpenHelper(context, name, factory, version) {

    //首先状态永远是被创建了，每次判断结果都是被创建过的，除非被oCreate函数设置成false
    var isCreatedOrUpdatedBefore = true
        get() {
            if (!field) {
                //当前为false 在下次get的时候会设置成为true
                field =true
                return false
            }
            return true
        }

    override fun onCreate(db: SQLiteDatabase?) {
        db?.execSQL(MainTableNames.CREATOR)
        isCreatedOrUpdatedBefore = false
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) = Unit
}

//常量用于避免写错
object MainTableNames {
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
