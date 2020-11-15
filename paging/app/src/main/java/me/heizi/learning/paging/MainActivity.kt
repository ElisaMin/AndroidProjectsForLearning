package me.heizi.learning.paging

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.*
import androidx.paging.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.*
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collectLatest
import me.heizi.learning.paging.databinding.ActivityMainBinding
import kotlin.random.Random


class MainActivity : AppCompatActivity() {
    object Rooms {
        const val tableName = "source_table"
        @Entity(tableName = tableName)
        data class DataEntity(
                @PrimaryKey(autoGenerate = true) val id:Int?=null,
                @ColumnInfo(name = "text") val text:String
        )
        @Dao
        interface DataDao {


            @Insert
            fun _ADD(vararg info: DataEntity)
            @Query("SELECT * FROM source_table Order by ID ASC")
            fun _ALL():PagingSource<Int,DataEntity>

            //@Query("SELECT * FROM source_table Order by ID ASC")
            //fun _ALLAsMyPaggingSource():PagingSource<Int,DataEntity>
            @Query("SELECT * FROM source_table WHERE ID > :before ORDER BY id DESC LIMIT :limit ")
            fun getDataByBeforeTimes(before:Int,limit:Int = 10):List<DataEntity>

            @Query("SELECT * FROM source_table WHERE ID >= :before ORDER BY id DESC LIMIT :limit ")
            fun getLastDataByBeforeTimes(before:Int,limit:Int = 10):List<DataEntity>
            @Query("SELECT * FROM source_table Order by ID ASC")
            fun getDataAsList():List<DataEntity>
            @Query("SELECT COUNT(*) FROM source_table;")
            fun count():Int

        }
        @Database(entities = [DataEntity::class],version = 1,exportSchema = false)
        abstract class DataDataBase :RoomDatabase() {
            companion object {
                private val INSTANCE:DataDataBase?=null
                fun getInstance(context:Context) = INSTANCE ?:Room.databaseBuilder(context,DataDataBase::class.java,"name").build()
            }
            abstract fun _getData():DataDao
            val data get() = _getData()

        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            (111).isBiggerThen(20).let(::println)
        }
        fun Int.isBiggerThen(other:Int, equable:Boolean =false):Boolean {
            return if (equable) {val result = this.compareTo(other) ;(result == 1) or (result ==0) } else {this.compareTo(other) == 1}
        }
        fun Int.isSmallerThen(other:Int, equable:Boolean =false):Boolean {
            return if (equable) {val result = this.compareTo(other) ;(result == -1) or (result ==0) } else {this.compareTo(other) == -1}
        }
        private const val TAG = "MainActivity"
        val sourcesOfDrunkTalking = listOf("我爱了","脱下裤子，","雨后","酒后","怀孕","妈妈","爸爸","吧！","我","你","他","说","跳","，建议","爬。","和","SM","哈哈哈","，真是笑死爷了，","什么时候能站起来！","我哭了，","笑了","，然后","狗屁","狗屁不通","TM","直接","生成","，","马老师","战国","川宝","在","恰奥里给","和年轻人比武","不讲武德","一定关掉","的","之","一","肖战","谈恋爱","孙笑川","站起来","气冷抖","圣母院","拿","推","草","，啊这，","打死","然后","了","吗","呢","建议","您","请")

    }
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    val db by lazy { Rooms.DataDataBase.getInstance(this) }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean { menuInflater.inflate(R.menu.main_menu,menu);return true }
    override fun onOptionsItemSelected(item: MenuItem): Boolean = when(item.itemId) {
        R.id.ref_menuitem_main -> {
            binding.showing.run {
                val random = Random.nextInt(100)
                Log.i(TAG, "onOptionsItemSelected: {random:$random,count:${adapter!!.itemCount}}")
                smoothScrollToPosition(adapter!!.itemCount-1)
            }
            true
        }
        else ->false
    }


    override fun onCreate(savedInstanceState: Bundle?) = super.onCreate(savedInstanceState).also { with(binding) {
        setContentView(root)

        randomMakingDatas.setOnClickListener {
            repeat(Random.nextInt(10)) {
                StringBuilder().apply {
                    repeat(Random.nextInt(30)) {
                        append(sourcesOfDrunkTalking.random()) }
                }.toString().takeIf { it.isNotEmpty() }?.let {
                    lifecycleScope.launch(Dispatchers.IO) {
                        db.data._ADD( Rooms.DataEntity(text =  it))
                    }
                }
            }
            lifecycleScope.launch(Main) {
                //dataSource.load(PagingSource.LoadParams.Refresh(dataSource.params.key,10,false))
            }
            showing.run {
                smoothScrollToPosition(adapter!!.itemCount-1)
            }
        }

        showing.run {
            layoutManager = LinearLayoutManager(context,LinearLayoutManager.VERTICAL,false).apply {
                //stackFromEnd =true
            }
            addItemDecoration(DividerItemDecoration(context,DividerItemDecoration.VERTICAL))
            adapter = mAdapter
        }

        lifecycleScope.launch(Main) {
            viewModel.getDataSourceLiveData(dataSource,lifecycleScope).cachedIn(lifecycle).observe(this@MainActivity) {
                lifecycleScope.launch(Default){
                    mAdapter.submitData(it)
                }
            }
        }

    }}
    val dataSource = MyDataSource()
    val viewModel by viewModels<MyViewModel>()
    class MyViewModel:ViewModel(){
        lateinit var dataSourceLiveData:LiveData<PagingData<Rooms.DataEntity>>
        fun getDataSourceLiveData(dataSource:MyDataSource,lifecycleCoroutineScope: LifecycleCoroutineScope):LiveData<PagingData<Rooms.DataEntity>> {
            dataSourceLiveData = Pager(config = PagingConfig(10),){dataSource}.flow.cachedIn(lifecycleCoroutineScope).asLiveData()
            return dataSourceLiveData
        }

    }

    override fun onResume() {
        super.onResume()

    }

    fun doBindingDataSource() =lifecycleScope.launch(Dispatchers.Default) {
        MyDataSource().also {
            //it.load(PagingSource.LoadParams.Prepend(0,10,false))
            Pager(config = PagingConfig(10),){it}.flow.cachedIn(lifecycleScope).asLiveData()

        }
    }


    inner class MyDataSource : PagingSource<Int,Rooms.DataEntity>() {
        lateinit var params: LoadParams<Int>

        override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Rooms.DataEntity> = try {
            this.params = params
            val page = params.key?:0


            //每一页最后一个ID
            val endID = if (page==0) 0 else (page+1)*params.loadSize
            val data = withContext(IO) {
                db.data.getDataByBeforeTimes(endID, limit = params.loadSize)
            }
            val maxItem = withContext(IO) {
                db.data.count()
            }
            Log.i(TAG, "load: {page:$page,endID:$endID,max:$maxItem,dataCount:${data.size}}")
            //data.forEach { Log.i(TAG, "load: ${it.id}") }
            LoadResult.Page(
                    nextKey = page.takeIf { endID.isSmallerThen(maxItem,true) }?.plus(1),
                    //prevKey = page.takeIf { (page>0) and endID.isBiggerThen(0) }?.minus(1),
                    prevKey = null,
                    data = data
            )
        }catch (e:Exception) { e.printStackTrace();LoadResult.Error(e)}

    }

    val mAdapter by lazy { MyPagingAdapter() }
    inner class MyPagingAdapter: PagingDataAdapter<Rooms.DataEntity, MyPagingAdapter.MyViewHolder>(object : DiffUtil.ItemCallback<Rooms.DataEntity>() {
        override fun areItemsTheSame(oldItem: Rooms.DataEntity, newItem: Rooms.DataEntity): Boolean {
            Log.i(TAG, "areItemsTheSame: newItem:${newItem.id},oldItem:$oldItem")
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Rooms.DataEntity, newItem: Rooms.DataEntity): Boolean = oldItem.text == newItem.text
    }) {
        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            Log.i(TAG, "onBindViewHolder: $position")
            (holder.itemView as TextView).text = getItem(position)?.text?.plus("  ${getItem(position)?.id}") ?: "空空如也"
        }


        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder
            = MyViewHolder(TextView(this@MainActivity))
        //suspend operator fun Rooms.DataDao.unaryMinus():List<Rooms.DataEntity> = lifecycleScope.launch { _ALL() }.
        inner class MyViewHolder(itemView: View):RecyclerView.ViewHolder(itemView)
    }
}


//
//fun main() {
//    val isBigger = -1
//    val isE = 0
//    val isSmaller = 1
//
//    1 .isBiggerThen(0,true).let(::println)
//}