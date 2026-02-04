package hr.jkacan.setmaker.data.dao

import android.content.Context

fun getSongRepository(context: Context) = SongRepository(context)
fun getSetRepository(context: Context) = SetRepository(context)
fun getSetGraphRepository(context: Context) = SetGraphRepository(context)