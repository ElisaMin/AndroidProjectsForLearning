package me.heizi.learning.contact.loging.data.rooms

import androidx.room.*

@Entity
data class Phone (
    @ColumnInfo(name = "name") val name:String,
    @ColumnInfo(name = "phone") val phone:String,
    @PrimaryKey val id:Int? =null)
@Dao
interface DataDao {
    @Query("select * from phone;")
    fun _getAll():List<Phone>
    val all:List<Phone> get() = _getAll()

    @Update()
    @Insert
    fun insert(vararg phones: Phone)
    @Delete
    fun delete(info: Phone):Int
}

@Database(entities = [Phone::class],version = 1)
abstract class MyDatabase : RoomDatabase() {
    abstract fun dataDao():DataDao
}

