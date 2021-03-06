package com.example.redminemobile.activities

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.example.redminemobile.services.ApiService
import com.example.redminemobile.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import android.widget.ArrayAdapter
import com.beust.klaxon.Klaxon
import com.example.redminemobile.models.KeyValue
import java.io.StringReader

class AddEntryActivity : AppCompatActivity() {

    private var issueId = -1
    private var activitiesMap:List<KeyValue>? = null
    private var isButtonBlocked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_entry)
        issueId = intent.getIntExtra("issue_id",-1)
        val issue = findViewById(R.id.addEntryIssue) as TextView?
        issue?.text = issueId.toString()
        initSpinner()
    }

    private fun initSpinner(){
        var dropdown = findViewById(R.id.spinner) as Spinner?
        val prefs = getSharedPreferences("Server", Context.MODE_PRIVATE)
        ApiService(prefs).requestEntryActivities(callback =  object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val body = response.body()?.string()
                val klaxon = Klaxon()
                val parsed = klaxon.parseJsonObject(StringReader(body))
                val dataArray = parsed.array<Any>("time_entry_activities")
                var output = dataArray?.let { klaxon.parseFromJsonArray<KeyValue>(it) }
                activitiesMap = output
                runOnUiThread{
                    val namesVertex = output?.map { it.name }
                    val adapter = ArrayAdapter(this@AddEntryActivity, android.R.layout.simple_spinner_dropdown_item, namesVertex)
                    dropdown?.adapter = adapter
                }
            }
        })
    }

    fun sendRequest(view: View){
        if (isButtonBlocked){
            return
        }
        else{
            isButtonBlocked = true
        }
        val hoursView= findViewById(R.id.addEntryHours) as TextView?
        val comment = findViewById(R.id.addEntryComments) as TextView?
        var dropdown = findViewById(R.id.spinner) as Spinner?
        val activity = dropdown?.selectedItem as String

        val selectedElements = activitiesMap?.filter { it.name == activity}
        val activityId = selectedElements?.first()?.id as Int
        val hours = hoursView?.text.toString().toInt()

        if (hours > 100) {
            isButtonBlocked = false
            runOnUiThread {
                Toast.makeText(this@AddEntryActivity, R.string.tooManyHoursError, Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (comment?.text?.trim().toString() == "") {
            isButtonBlocked = false
            runOnUiThread {
                Toast.makeText(this@AddEntryActivity, R.string.emptyCommentError, Toast.LENGTH_SHORT).show()
            }
            return
        }

        val prefs = getSharedPreferences("Server", Context.MODE_PRIVATE)
        ApiService(prefs).createEntry(hours, issueId,comment?.text.toString(),activityId,
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {}
                override fun onResponse(call: Call, response: Response) {
                    isButtonBlocked = false
                    if (response.code() == 201)
                        finish()
                    else{
                        var out = response.body().toString()
                        runOnUiThread{
                            Toast.makeText(this@AddEntryActivity,out,Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
    }
}