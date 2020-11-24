package me.heizi.learning.camora

import android.Manifest
import android.content.ContentValues
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.runBlocking
import me.heizi.learning.camora.databinding.ActivityMainBinding
import java.io.File

class MainActivity : AppCompatActivity() {

    //viewBinding
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    /** resign for results **/
    //拍照
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()){_->
        binding.imageView.setImageBitmap(BitmapFactory.decodeStream(contentResolver.openInputStream(url!!)))
    }//权限
    private val checkPms = registerForActivityResult(RequestMultiplePermissions()) {
        //回调 遍历权限
        it.values.forEach { b->
            //直接退出,并留下潇洒的一句话
            if (!b) {
                Toast.makeText(this,"无权限，正在退出。",Toast.LENGTH_LONG).show()
                finish()
            }
        }
        //没有退出时就bind了
        binding.bindView()
    }// url getter

    private val url by lazy {
        kotlin.runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, "tmp.img")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                    }
                )
            } else {
                File(externalCacheDir,"tmp.jpg").apply {
                    if (exists()) delete()
                }.let {
                    it.createNewFile()
                    FileProvider.getUriForFile(this@MainActivity,BuildConfig.FILES_AUTHORITY,it)
                }
            }
        }.onFailure { it.printStackTrace() }.getOrNull()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //检查权限,没有的时候直接退出(老流氓软件了
        if (!checkPms()) {
            AlertDialog.Builder(this@MainActivity)
                .setTitle("打开中……")
                .setMessage("本应用用于拍照。需要相机、写入存储空间权限，如果没有这些权限本应用则失去意义，请给予。\n")
                //设置按钮当按下时Dialog消失
                .setPositiveButton("确认") { dialogInterface: DialogInterface, _ -> dialogInterface.dismiss() }
                //在消失时启动 checkPms
                .setOnDismissListener { checkPms.launch(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA)) }
                .show()
        }else {
            //直接set content view 和 on click
            binding.bindView()
        }
    }

    private fun ActivityMainBinding.bindView() {
        setContentView(root)
        button.setOnClickListener {
            //此时get url 然后报错的时候直接空
            url?.let(takePhoto::launch) ?: AlertDialog.Builder(this@MainActivity).setMessage("失败").setNegativeButton("取消",null).show()
        }
    }
    private fun checkPms():Boolean {
        for (p in arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE,Manifest.permission.CAMERA))
            if (ActivityCompat.checkSelfPermission(this,p) != PackageManager.PERMISSION_GRANTED)
                return false
        return true
    }
}