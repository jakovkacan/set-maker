package hr.jkacan.setmaker.data.dao

interface Repository<T> {
    fun insert(item: T): Long
    fun update(item: T): Int
    fun delete(id: Int): Int
    fun getById(id: Int): T?
    fun getAll(): List<T>
}