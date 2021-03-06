package com.anenha.weather.utils

import android.content.Context
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText

import com.anenha.weather.R
import com.anenha.weather.entity.TodayEntity
import com.anenha.weather.model.ChannelModel
import com.anenha.weather.model.FavoriteModel
import com.anenha.weather.entity.FavoritesEntity
import com.anenha.weather.repository.yahooWeather.WeatherServiceCallback
import com.anenha.weather.repository.yahooWeather.YahooWeatherService
import com.google.gson.Gson

import java.util.ArrayList

/**
 * Created by ajnen on 22/10/2017.
 *
 */

object Prefs {

    interface PrefsCallback {
        fun onAddCity(city: String)
    }

    interface WeatherCallback {
        fun onUpdate(fe: FavoritesEntity?)
    }

    fun getInitialCity(context: Context): String {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(context.getString(R.string.pref_city_key), context.getString(R.string.pref_default_display_city))
    }

    fun canUseGpsLocation(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_location_key), false)
    }

    fun isTransparentMode(context: Context): Boolean {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(context.getString(R.string.pref_widget_color_key), false)
    }

    fun useGpsLocation(context: Context, canUse: Boolean) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPref.edit()
        editor.putBoolean(context.getString(R.string.pref_location_key), canUse)
        editor.apply()
    }

    fun addCityDialog(context: Context, windowToken: IBinder, callback: PrefsCallback) {

        val view = LayoutInflater.from(context)
                .inflate(R.layout.layout_dialog_input, null, false)

        AlertDialog.Builder(context)
                .setView(view)
                .setPositiveButton(context.getString(R.string.add_city_dialog_positive_button)) { _, _ ->
                    val city = (view.findViewById<View>(R.id.dialog_input) as EditText).text.toString()
                    addCity(context, city, callback)
                }
                .setNegativeButton(context.getString(R.string.add_city_dialog_negative_button)) { _, _ ->
                    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(windowToken, 0)
                }
                .show()
    }

    fun updateFavorites(context: Context, cities: List<String>) {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val editor = sharedPref.edit()
        editor.putString(context.getString(R.string.pref_favorites_key), Gson().toJson(cities))
        editor.apply()
    }

    fun getFavoritesWeather(context: Context, callback: WeatherCallback) {
        val cities = getFavorites(context)
        val favorites = ArrayList<FavoriteModel>()
        if (!cities.isEmpty()) {
            var yahooService: YahooWeatherService
            for (pos in cities.indices) {
                val city = cities[pos]
                yahooService = YahooWeatherService(object : WeatherServiceCallback {
                    override fun serviceSuccess(channel: ChannelModel) {
                        addFavorite(context, favorites, cities, city, channel, callback)
                    }

                    override fun serviceFailure(exception: Exception) {
                        addFavorite(context, favorites, cities, city, null, callback)
                    }
                })
                yahooService.refreshWeather(city)
            }
        } else {
            callback.onUpdate(null)
        }
    }

    private fun getFavorites(context: Context): ArrayList<String> {
        val sharedPref = PreferenceManager.getDefaultSharedPreferences(context)
        val favorites = sharedPref.getString(context.getString(R.string.pref_favorites_key), null)
                ?: return ArrayList()
        return Gson().fromJson<ArrayList<String>>(favorites, ArrayList<String>().javaClass)
    }

    private fun addFavorite(context: Context, favorites: MutableList<FavoriteModel>,
                            cities: List<String>, city: String, channel: ChannelModel?,
                            callback: WeatherCallback) {
        favorites.add(FavoriteModel(city, TodayEntity(context, channel!!, object : TodayEntity.TodayCallback {
            override fun onCreate(te: TodayEntity) {
                if (favorites.size == cities.size) {
                    val fe = FavoritesEntity(favorites)
                    callback.onUpdate(fe)
                }
            }
        })))
    }

    private fun addCity(context: Context, city: String, callback: PrefsCallback) {
        val cities = getFavorites(context)
        cities.add(city)
        updateFavorites(context, cities)
        callback.onAddCity(city)
    }
}
