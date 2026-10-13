/*
 * SPDX-FileCopyrightText: The Yasvara Project
 * SPDX-License-Identifier: Apache-2.0
 */
package org.yasvara.pentas.utils

import android.content.SharedPreferences
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class ObservableBooleanPref(
    private val db: SharedPreferences,
    private val key: String,
    default: Boolean,
) : ReadWriteProperty<Any?, Boolean> {
    val state: MutableState<Boolean> = mutableStateOf(db.getBoolean(key, default))

    fun set(value: Boolean) {
        state.value = value
        db.edit().putBoolean(key, value).apply()
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>) = state.value
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) = set(value)
}

class ObservableIntPref(
    private val db: SharedPreferences,
    private val key: String,
    default: Int,
) : ReadWriteProperty<Any?, Int> {
    val state: MutableIntState = mutableIntStateOf(db.getInt(key, default))

    fun set(value: Int) {
        state.intValue = value
        db.edit().putInt(key, value).apply()
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>) = state.intValue
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) = set(value)
}

class ObservableStringPref(
    private val db: SharedPreferences,
    private val key: String,
    default: String,
) : ReadWriteProperty<Any?, String> {
    val state: MutableState<String> = mutableStateOf(db.getString(key, default) ?: default)

    fun set(value: String) {
        state.value = value
        db.edit().putString(key, value).apply()
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>) = state.value
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) = set(value)
}

class ObservableIntClampedPref(
    private val db: SharedPreferences,
    private val key: String,
    default: Int,
    private val range: IntRange,
) : ReadWriteProperty<Any?, Int> {
    val state: MutableIntState =
        mutableIntStateOf(db.getInt(key, default).coerceIn(range))

    fun set(value: Int) {
        val v = value.coerceIn(range)
        state.intValue = v
        db.edit().putInt(key, v).apply()
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>) = state.intValue
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) = set(value)
}

class ObservableStringIntPref(
    private val db: SharedPreferences,
    private val key: String,
    default: Int,
) : ReadWriteProperty<Any?, Int> {
    val state: MutableIntState =
        mutableIntStateOf(db.getString(key, default.toString())?.toIntOrNull() ?: default)

    fun set(value: Int) {
        state.intValue = value
        db.edit().putString(key, value.toString()).apply()
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>) = state.intValue
    override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) = set(value)
}

/** SharedPreferences exposed as Compose state. */
class ObservablePreferences(private val db: SharedPreferences) {

    private val booleanDelegates = ConcurrentHashMap<String, ObservableBooleanPref>()
    private val intDelegates = ConcurrentHashMap<String, ObservableIntPref>()
    private val stringDelegates = ConcurrentHashMap<String, ObservableStringPref>()
    private val intClampedDelegates = ConcurrentHashMap<String, ObservableIntClampedPref>()
    private val stringIntDelegates = ConcurrentHashMap<String, ObservableStringIntPref>()

    fun boolean(key: String, default: Boolean): ObservableBooleanPref =
        booleanDelegates.getOrPut(key) { ObservableBooleanPref(db, key, default) }

    fun int(key: String, default: Int): ObservableIntPref =
        intDelegates.getOrPut(key) { ObservableIntPref(db, key, default) }

    fun string(key: String, default: String): ObservableStringPref =
        stringDelegates.getOrPut(key) { ObservableStringPref(db, key, default) }

    fun intClamped(key: String, default: Int, range: IntRange): ObservableIntClampedPref =
        intClampedDelegates.getOrPut(key) { ObservableIntClampedPref(db, key, default, range) }

    fun stringInt(key: String, default: Int): ObservableStringIntPref =
        stringIntDelegates.getOrPut(key) { ObservableStringIntPref(db, key, default) }

    fun booleanState(key: String, default: Boolean): MutableState<Boolean> =
        boolean(key, default).state

    fun intState(key: String, default: Int): MutableIntState =
        int(key, default).state

    fun stringState(key: String, default: String): MutableState<String> =
        string(key, default).state

    fun intClampedState(key: String, default: Int, range: IntRange): MutableIntState =
        intClamped(key, default, range).state

    fun stringIntState(key: String, default: Int): MutableIntState =
        stringInt(key, default).state

    fun setBoolean(key: String, default: Boolean, value: Boolean) =
        boolean(key, default).set(value)

    fun setInt(key: String, default: Int, value: Int) =
        int(key, default).set(value)

    fun setString(key: String, default: String, value: String) =
        string(key, default).set(value)

    fun setIntClamped(key: String, default: Int, range: IntRange, value: Int) =
        intClamped(key, default, range).set(value)

    fun setStringInt(key: String, default: Int, value: Int) =
        stringInt(key, default).set(value)

    fun flush() {
        db.edit().commit()
    }
}