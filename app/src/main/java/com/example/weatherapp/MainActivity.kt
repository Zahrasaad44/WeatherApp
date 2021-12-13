package com.example.weatherapp

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.example.weatherapp.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.*
import org.json.JSONObject
import java.lang.Exception
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding


    private var city = "10001"
    private var APIKey = "a5d23dd3a1a5f9ba668176695ff35e2a"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        requestAPI()

        binding.zipBtn.setOnClickListener {
            city = binding.zipET.text.toString()
            requestAPI()
            binding.zipET.text.clear() // To clear the EditText after clicking zipBtn

            //To hide the keyboard
            val imm = ContextCompat.getSystemService(this, InputMethodManager::class.java)
            imm?.hideSoftInputFromWindow(this.currentFocus?.windowToken, 0)
            binding.zipRL.isVisible = false
        }

    }


    private fun requestAPI() {
        CoroutineScope(Dispatchers.IO).launch {
            updateUIStatus(0)
            val data = async { fetchWeather() }.await()
            if (data.isNotEmpty()) {
                displayWeatherDate(data)
                updateUIStatus(1)
            } else {
                updateUIStatus(-1)

                Snackbar.make(binding.mainCL, "Please Enter a Valid ZIP code", Snackbar.LENGTH_INDEFINITE)
                    .setAction("Retry") {
                        city = "10001"
                        requestAPI()
                    }.show()
            }
        }
    }

    private fun fetchWeather(): String {
        var response = ""

        try {
            response =
                URL("https://api.openweathermap.org/data/2.5/weather?zip=$city&units=metric&appid=$APIKey").readText()
        } catch (exce: Exception) {
            println("ERROR: $exce")
        }
        return response
    }

    @SuppressLint("SetTextI18n")
    private suspend fun displayWeatherDate(Weather: String) {
        withContext(Dispatchers.Main) {
            val jsonObject = JSONObject(Weather)  // normally parsing JSON (without using retrofit)
            val main = jsonObject.getJSONObject("main")
            //"main" object contains the "temp","temp_min","temp_max","pressure","humidity"

            val wind = jsonObject.getJSONObject("wind")
            //"wind" object contains "speed"

            val sys = jsonObject.getJSONObject("sys")
            //"sys" object contains "country", "sunrise", "sunset"

            val weather = jsonObject.getJSONArray("weather").getJSONObject(0)
            //"weather" object contains array of "id", "main", "description", "icon" ("weather": [{"id": 800, "main": "Clear", "description": "clear sky", "icon": "01d" } ])

            val updatedAt: Long = jsonObject.getLong("dt")
            binding.dateOfUpdateTV.text = "Updated at:" + SimpleDateFormat(
                "dd/MM/yyyy hh:mm a", Locale.ENGLISH
            ).format(Date(updatedAt * 1000))

            val currentTemp = main.getString("temp")

            val temp = try {
                currentTemp.substring(0, currentTemp.indexOf(".")) + "°C"
            } catch (e: Exception) {
                "$currentTemp°C"
            }
            val minTemp = main.getString("temp_min")
            val lowestTemp = "Lowest: " + minTemp.substring(0, minTemp.indexOf(".")) + "°C"
            val maxTemp = main.getString("temp_max")
            val highestTemp = "Highest: " + maxTemp.substring(0, maxTemp.indexOf(".")) + "°C"
            val pressure = main.getString("pressure")
            val humidity = main.getString("humidity")

            val sunrise: Long = sys.getLong("sunrise")
            val sunset: Long = sys.getLong("sunset")

            val windSpeed = wind.getString("speed")

            val weatherState = weather.getString("description")

            val location = "${jsonObject.getString("name")}, ${sys.getString("country")}"

            binding.cityNameTV.text = location
            binding.weatherStateTV.text =
                weatherState.capitalize(Locale.getDefault()) // To capitalize the first letter
            binding.temperatureTV.text = temp
            binding.lowestTempTV.text = lowestTemp
            binding.highestTempTV.text = highestTemp
            binding.sunriseTimeTV.text =
                SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunrise * 1000))
            binding.sunsetTimeTV.text =
                SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(sunset * 1000))
            binding.windTV.text = windSpeed
            binding.pressureTV.text = pressure
            binding.humidityTV.text = humidity
            binding.refreshLL.setOnClickListener { requestAPI() }
        }
    }

    private suspend fun updateUIStatus(state: Int) {
        withContext(Dispatchers.Main) {
            // states: -1 = error, 0 = loading, 1 = loaded
            when {
                state < 0 -> {
                    binding.progressBar.visibility = View.GONE
                    binding.zipRL.visibility = View.VISIBLE
                }
                state == 0 -> {
                    binding.progressBar.visibility = View.VISIBLE
                    binding.mainCL.visibility = View.GONE
                }
                state > 0 -> {
                    binding.progressBar.visibility = View.GONE
                    binding.mainCL.visibility = View.VISIBLE
                }
            }
        }
    }
}