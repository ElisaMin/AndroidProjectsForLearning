package me.heizi.learning.motionlayout.data.database

import androidx.room.*


@Entity(tableName = "images") data class Image(
        @PrimaryKey val id:Int?=null,
        @ColumnInfo val fileName:String,
        @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val bytes:ByteArray

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Image

        if (id != other.id) return false
        if (!bytes.contentEquals(other.bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id ?: 0
        result = 31 * result + bytes.contentHashCode()
        return result
    }
}



@Dao
interface ImageDao {
    @Query("SELECT * FROM images") fun _getAll() : List<Image>
    val all get() = _getAll()

    @Query("select count(*) from images")fun size():Int
    @Query("SELECT * FROM images WHERE id=:id") fun getImageInfoByID(id:Int):Image
    @Insert fun add(vararg images:Image)
}
@Database(entities = [Image::class],version = 1,exportSchema = false)
abstract class MyDatabase : RoomDatabase() {
    private val dao:ImageDao by lazy { _getDao() }
    abstract fun _getDao():ImageDao
    val all get() =  dao.all
    val size get() = dao.size()
    infix fun find(id:Int) = dao.getImageInfoByID(id)
}