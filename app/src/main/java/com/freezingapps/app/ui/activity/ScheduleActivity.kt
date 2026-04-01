package com.freezingapps.app.ui.activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.freezingapps.app.R
import com.freezingapps.app.databinding.ActivityScheduleBinding
import com.freezingapps.app.worker.FreezeWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Activity for scheduling freeze/unfreeze tasks.
 * Uses WorkManager for reliable task scheduling.
 */
class ScheduleActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScheduleBinding
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScheduleBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.schedule_task)

        setupActionSpinner()
        setupDateTimePicker()
        setupScheduleButton()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupActionSpinner() {
        val actions = arrayOf(
            getString(R.string.freeze),
            getString(R.string.unfreeze)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, actions)
        binding.actionSpinner.adapter = adapter
    }

    private fun setupDateTimePicker() {
        // Set initial time to one hour from now
        calendar.add(Calendar.HOUR_OF_DAY, 1)
        updateDateTimeDisplay()

        binding.dateTimeButton.setOnClickListener {
            showDatePicker()
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                showTimePicker()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(
            this,
            { _, hour, minute ->
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                updateDateTimeDisplay()
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun updateDateTimeDisplay() {
        binding.dateTimeText.text = dateFormat.format(calendar.time)
    }

    private fun setupScheduleButton() {
        binding.scheduleButton.setOnClickListener {
            val packageName = binding.packageNameInput.text.toString().trim()
            if (packageName.isEmpty()) {
                binding.packageNameLayout.error = getString(R.string.package_name_required)
                return@setOnClickListener
            }

            binding.packageNameLayout.error = null

            val action = if (binding.actionSpinner.selectedItemPosition == 0) "freeze" else "unfreeze"
            val delayMillis = calendar.timeInMillis - System.currentTimeMillis()

            if (delayMillis <= 0) {
                Toast.makeText(this, R.string.schedule_time_past, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            scheduleTask(packageName, action, delayMillis)
        }
    }

    /**
     * Schedule a freeze/unfreeze task using WorkManager.
     */
    private fun scheduleTask(packageName: String, action: String, delayMillis: Long) {
        val data = Data.Builder()
            .putString(FreezeWorker.KEY_PACKAGE_NAME, packageName)
            .putString(FreezeWorker.KEY_ACTION, action)
            .build()

        val workRequest = OneTimeWorkRequestBuilder<FreezeWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("freeze_schedule")
            .build()

        WorkManager.getInstance(this).enqueue(workRequest)

        val scheduledTime = dateFormat.format(Date(System.currentTimeMillis() + delayMillis))
        Toast.makeText(
            this,
            getString(R.string.task_scheduled, action, packageName, scheduledTime),
            Toast.LENGTH_LONG
        ).show()

        finish()
    }
}
