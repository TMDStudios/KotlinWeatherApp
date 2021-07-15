package com.example.kotlinweatherapp

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.lang.Exception
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var city = "10001"
    private val API = "8a316bae40ca552c86771c6d73150592"

    private lateinit var errorButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        errorButton = findViewById(R.id.btError)
        errorButton.setOnClickListener {
            city = "10001"
            requestAPI()
        }

        requestAPI()
    }

    private fun requestAPI(){
        println("CITY: $city")
        CoroutineScope(IO).launch {
            updateStatus(-1)
            val data = async {
                fetchWeatherData()
            }.await()
            if(data.isNotEmpty()){
                updateWeatherData(data)
                updateStatus(0)
            }else{
                updateStatus(1)
            }
        }
    }

    private suspend fun updateWeatherData(result: String){
        withContext(Main){
            val jsonObj = JSONObject(result)
            val main = jsonObj.getJSONObject("main")
            val sys = jsonObj.getJSONObject("sys")
            val wind = jsonObj.getJSONObject("wind")
            val weather = jsonObj.getJSONArray("weather").getJSONObject(0)

            val lastUpdate:Long = jsonObj.getLong("dt")
            val lastUpdateText = "Updated at: " + SimpleDateFormat(
                "dd/MM/yyyy hh:mm a",
                Locale.ENGLISH).format(Date(lastUpdate*1000))
            val currentTemperature = main.getString("temp")
            val temp = currentTemperature.substring(0, currentTemperature.indexOf(".")) + "°C"
            val minTemperature = main.getString("temp_min")
            val tempMin = "Low: " + minTemperature.substring(0, minTemperature.indexOf("."))+"°C"
            val maxTemperature = main.getString("temp_max")
            val tempMax = "High: " + maxTemperature.substring(0, maxTemperature.indexOf("."))+"°C"
            val pressure = main.getString("pressure")
            val humidity = main.getString("humidity")

            val sunrise:Long = sys.getLong("sunrise")
            val sunset:Long = sys.getLong("sunset")
            val windSpeed = wind.getString("speed")
            val weatherDescription = weather.getString("description")

            val address = jsonObj.getString("name")+", "+sys.getString("country")

            findViewById<TextView>(R.id.tvAddress).text = address
            findViewById<TextView>(R.id.tvAddress).setOnClickListener {
                customAlert()
            }
            findViewById<TextView>(R.id.tvLastUpdated).text =  lastUpdateText
            findViewById<TextView>(R.id.tvStatus).text = weatherDescription.capitalize(Locale.getDefault())
            findViewById<TextView>(R.id.tvTemperature).text = temp
            findViewById<TextView>(R.id.tvTempMin).text = tempMin
            findViewById<TextView>(R.id.tvTempMax).text = tempMax
            findViewById<TextView>(R.id.tvSunrise).text = SimpleDateFormat("hh:mm a",
                Locale.ENGLISH).format(Date(sunrise*1000))
            findViewById<TextView>(R.id.tvSunset).text = SimpleDateFormat("hh:mm a",
                Locale.ENGLISH).format(Date(sunset*1000))
            findViewById<TextView>(R.id.tvWind).text = windSpeed
            findViewById<TextView>(R.id.tvPressure).text = pressure
            findViewById<TextView>(R.id.tvHumidity).text = humidity
            findViewById<LinearLayout>(R.id.llRefresh).setOnClickListener { requestAPI() }
        }
    }

    private fun fetchWeatherData(): String{
        var response = ""
        try {
            response = URL("https://api.openweathermap.org/data/2.5/weather?zip=$city&units=metric&appid=$API")
                .readText(Charsets.UTF_8)
        }catch (e: Exception){
            println("Error: $e")
        }
        return response
    }

    private suspend fun updateStatus(state: Int){
//        states: -1 = loading, 0 = loaded, 1 = error
        withContext(Main){
            when{
                state < 0 -> {
                    findViewById<ProgressBar>(R.id.pbProgress).visibility = View.VISIBLE
                    findViewById<RelativeLayout>(R.id.rlMain).visibility = View.GONE
                    findViewById<LinearLayout>(R.id.llErrorContainer).visibility = View.GONE
                }
                state == 0 -> {
                    findViewById<ProgressBar>(R.id.pbProgress).visibility = View.GONE
                    findViewById<RelativeLayout>(R.id.rlMain).visibility = View.VISIBLE
                }
                state > 0 -> {
                    findViewById<ProgressBar>(R.id.pbProgress).visibility = View.GONE
                    findViewById<LinearLayout>(R.id.llErrorContainer).visibility = View.VISIBLE
                }
            }
        }
    }

    private fun customAlert(){
        val dialogBuilder = AlertDialog.Builder(this@MainActivity)

        // Set up the input
        val input = EditText(this)
        // Specify the type of input expected
        input.hint = "Enter Zip"
        input.inputType = InputType.TYPE_CLASS_NUMBER

        // set message of alert dialog
        dialogBuilder.setMessage("Enter your Zip code:")
            // if the dialog is cancelable
            .setCancelable(false)
            // positive button text and action
            .setPositiveButton("Submit", DialogInterface.OnClickListener {
                    dialog, id ->
                run { city = input.text.toString(); requestAPI() }
            })
            // negative button text and action
            .setNegativeButton("Cancel", DialogInterface.OnClickListener {
                    dialog, id -> dialog.cancel()
            })

        // create dialog box
        val alert = dialogBuilder.create()
        // set title for alert dialog box
        alert.setTitle("Change City")
        // show alert dialog

        alert.setView(input)

        alert.show()
    }
}