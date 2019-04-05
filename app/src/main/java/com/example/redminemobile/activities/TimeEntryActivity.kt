package com.example.redminemobile.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import com.beust.klaxon.Klaxon
import com.example.redminemobile.adapters.TimeEntriesListViewAdapter
import com.example.redminemobile.models.TimeEntry
import com.example.redminemobile.services.ApiService
import com.example.redminemobile.R
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Response
import java.io.IOException
import java.io.StringReader

class TimeEntryActivity : AppCompatActivity() {

    private var issueId: Int = -1
    private var entriesPlaceholder: TextView? = null
    private var listView: ListView? = null
    private var loadMoreButton: Button? = null
    private var offset: Int = 0
    private var quantity: Int = 4
    private var isAdapterCreated = false
    private val mSwipeRefreshLayout: android.support.v4.widget.SwipeRefreshLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry)
        issueId = intent.getIntExtra("issue_id",-1)
        entriesPlaceholder = findViewById(R.id.entriesPlaceholder) as TextView?
        loadMoreButton = findViewById(R.id.loadMoreButton) as Button?
        listView = findViewById(R.id.listView) as ListView?
        fillEntries()
        val mSwipeRefreshLayout = findViewById(R.id.swipeRefreshEntry) as SwipeRefreshLayout
        mSwipeRefreshLayout.setOnRefreshListener{
            fillEntries()
            mSwipeRefreshLayout.isRefreshing = false
        }
    }

    private fun fillEntries()
    {
        val prefs = getSharedPreferences("Server", Context.MODE_PRIVATE)
        ApiService(prefs).requestGet("time_entries.json", offset, quantity, additionalParams = "issue_id=$issueId", callback =  object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val body = response.body()?.string()
                val klaxon = Klaxon()
                val parsed = klaxon.parseJsonObject(StringReader(body))
                val dataArray = parsed.array<Any>("time_entries")
                var output = dataArray?.let { klaxon.parseFromJsonArray<TimeEntry>(it) }
                if (output != null && output.count() != 0){
                    updateView(output as ArrayList<TimeEntry>)
                }
                else if (offset==0){
                    entriesPlaceholder?.text = getString(R.string.entriesNotifyEmpty)
                }
            }
        })
    }

    private fun updateView(items: ArrayList<TimeEntry>)
    {
        runOnUiThread{
            var localItems: ArrayList<TimeEntry> = items
            if (items.count() == quantity){
                offset += quantity -1
                localItems = ArrayList(items.dropLast(1))
            }
            else
            {
                loadMoreButton?.visibility = View.GONE
            }

            if (isAdapterCreated){
                addItemsToAdapter(localItems)
            }
            else{
                createAdapter(localItems)
            }
        }
    }

    private fun createAdapter(items: ArrayList<TimeEntry>)
    {
        runOnUiThread {
            val listView = findViewById(R.id.listView) as ListView
            val adapter = TimeEntriesListViewAdapter(this, items)
            listView.adapter = adapter
            listView.setOnItemClickListener { parent, view, position, id ->
                var intent = Intent(this, AddEntryActivity::class.java)
                intent.putExtra("issue_id", issueId)
                startActivity(intent)
            }
            adapter.notifyDataSetChanged()
            entriesPlaceholder?.visibility = View.GONE
            isAdapterCreated = true
        }
    }

    private fun addItemsToAdapter(projects: ArrayList<TimeEntry>){
        val adapter = listView?.adapter as TimeEntriesListViewAdapter
        adapter.addItems(projects)
    }

    fun loadMore(view: View?){
        fillEntries()
    }
}