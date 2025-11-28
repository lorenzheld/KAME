package com.example.kame.data.database

import androidx.room.TypeConverter
import com.example.kame.data.PlannedSet
import com.example.kame.data.CompletedSet
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String {
        return gson.toJson(list ?: emptyList<String>())
    }

    @TypeConverter
    fun fromPlannedSetList(value: String?): List<PlannedSet> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<PlannedSet>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun toPlannedSetList(list: List<PlannedSet>?): String {
        return gson.toJson(list ?: emptyList<PlannedSet>())
    }

    @TypeConverter
    fun fromCompletedSetList(value: String?): List<CompletedSet> {
        if (value == null) return emptyList()
        val listType = object : TypeToken<List<CompletedSet>>() {}.type
        return gson.fromJson(value, listType) ?: emptyList()
    }

    @TypeConverter
    fun toCompletedSetList(list: List<CompletedSet>?): String {
        return gson.toJson(list ?: emptyList<CompletedSet>())
    }
}