package com.example.redmineapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.widget.ListView
import com.beust.klaxon.Klaxon
import com.example.redmineapp.adapters.TimeEntriesListViewAdapter
import com.example.redmineapp.data.TimeEntry
import com.example.redmineapp.services.ApiService
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.io.StringReader

class TimeEntryActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        val issueId = intent.getIntExtra("issue_id",-1)
        fillEntries(issueId)
    }

    private fun fillEntries(issueId: Int)
    {
        val prefs = getSharedPreferences("Server", Context.MODE_PRIVATE)
        ApiService(prefs).getProjects("time_entries.json?issue_id=$issueId", object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val body = response.body()?.string()
                val klaxon = Klaxon()
                val parsed = klaxon.parseJsonObject(StringReader(body))
                val dataArray = parsed.array<Any>("time_entries")
                var output = dataArray?.let { klaxon.parseFromJsonArray<TimeEntry>(it) }
                updateView(output as List<TimeEntry>)
            }
        })
    }

    private fun updateView(items: List<TimeEntry>)
    {
        runOnUiThread {
            val listView = findViewById(R.id.listView) as ListView
            val adapter = TimeEntriesListViewAdapter(this, items)
            listView.adapter = adapter
            listView.setOnItemClickListener { parent, view, position, id ->
            }
            adapter.notifyDataSetChanged()
        }
    }
}