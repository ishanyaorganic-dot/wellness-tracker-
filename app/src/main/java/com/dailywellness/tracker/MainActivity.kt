package com.dailywellness.tracker

import android.app.DatePickerDialog
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.view.inputmethod.InputMethodManager
import android.content.Context
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.dailywellness.tracker.data.AppDatabase
import com.dailywellness.tracker.data.DailyEntry
import com.dailywellness.tracker.security.PinManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val forest = Color.rgb(7, 59, 43)
    private val leaf = Color.rgb(13, 112, 75)
    private val cream = Color.rgb(247, 248, 242)
    private val dao by lazy { AppDatabase.get(this).dailyDao() }
    private val pins by lazy { PinManager(this) }
    private var selectedDate = today()
    private var entry = DailyEntry(date = selectedDate)

    private lateinit var dateText: TextView
    private lateinit var summary: TextView
    private lateinit var water: EditText
    private lateinit var juice: CheckBox
    private lateinit var juiceName: Spinner
    private lateinit var juiceMl: EditText
    private lateinit var omega: CheckBox
    private lateinit var creatine: CheckBox
    private lateinit var multi: CheckBox
    private lateinit var magnesium: CheckBox
    private lateinit var gym: CheckBox
    private lateinit var workout: Spinner
    private lateinit var minutes: EditText
    private lateinit var intensity: Spinner
    private lateinit var workoutNotes: EditText
    private lateinit var dailyNotes: EditText

    private val juices = arrayOf("ABC Juice", "Cucumber Juice", "Amla Juice", "Watermelon Juice", "Carrot Juice", "Tomato Juice", "Lemon Mint Drink")
    private val workouts = arrayOf("Strength training", "Cardio", "Lower body", "Upper body", "Full body", "Yoga / Mobility", "HIIT", "Walking", "Other")
    private val intensities = arrayOf("Light", "Moderate", "High")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = forest
        window.navigationBarColor = forest
        authenticate()
    }

    private fun authenticate() {
        if (!pins.isConfigured()) { pinDialog(true); return }
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (BiometricManager.from(this).canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_SUCCESS) {
            val prompt = BiometricPrompt(this, ContextCompat.getMainExecutor(this),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result); openApp()
                    }
                    override fun onAuthenticationError(code: Int, message: CharSequence) {
                        super.onAuthenticationError(code, message)
                        if (code != BiometricPrompt.ERROR_CANCELED) pinDialog(false)
                    }
                })
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock Daily Wellness")
                .setSubtitle("Use fingerprint or phone screen lock")
                .setAllowedAuthenticators(authenticators).build()
            prompt.authenticate(info)
        } else pinDialog(false)
    }

    private fun pinDialog(first: Boolean) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(22, 8, 22, 0) }
        val pin = EditText(this).apply {
            hint = if (first) "Create 4–8 digit PIN" else "Enter PIN"
            inputType = 2 or 16
            filters = arrayOf(android.text.InputFilter.LengthFilter(8))
        }
        box.addView(pin)
        val confirm = if (first) EditText(this).apply {
            hint = "Confirm PIN"; inputType = 2 or 16
            filters = arrayOf(android.text.InputFilter.LengthFilter(8))
        } else null
        if (confirm != null) box.addView(confirm)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(if (first) "Create your private PIN" else "Unlock Daily Wellness")
            .setMessage(if (first) "This PIN protects access to her personal wellness journal." else "Enter the app PIN to continue.")
            .setView(box).setCancelable(false)
            .setPositiveButton(if (first) "Create PIN" else "Unlock", null).create()
        dialog.setOnShowListener {
            dialog.getButton(-1).setOnClickListener {
                val value = pin.text.toString()
                when {
                    value.length !in 4..8 -> pin.error = "Enter 4 to 8 digits"
                    first && confirm?.text.toString() != value -> confirm?.error = "PINs do not match"
                    first -> { pins.setPin(value); dialog.dismiss(); openApp() }
                    pins.verify(value) -> { dialog.dismiss(); openApp() }
                    else -> pin.error = "Incorrect PIN"
                }
            }
        }
        dialog.show()
    }

    private fun openApp() {
        val scroll = ScrollView(this)
        val page = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(18, 16, 18, 28); setBackgroundColor(cream) }
        val banner = TextView(this).apply {
            text = "✦  DAILY WELLNESS\nYour private habit & fitness journal"
            textSize = 19f; setTextColor(Color.WHITE); setPadding(18, 22, 18, 22); setBackgroundColor(forest)
        }
        page.addView(banner)
        dateText = TextView(this).apply { textSize = 18f; setTextColor(forest); setPadding(0, 18, 0, 8) }
        page.addView(dateText)
        val dates = LinearLayout(this).apply { gravity = Gravity.CENTER }
        dates.addView(button("‹ Previous") { shiftDate(-1) }, LinearLayout.LayoutParams(0, -2, 1f))
        dates.addView(button("Choose date") { chooseDate() }, LinearLayout.LayoutParams(0, -2, 1f))
        dates.addView(button("Next ›") { shiftDate(1) }, LinearLayout.LayoutParams(0, -2, 1f))
        page.addView(dates)
        summary = TextView(this).apply { textSize = 16f; setTextColor(Color.WHITE); setPadding(16, 15, 16, 15); setBackgroundColor(leaf) }
        page.addView(summary, params(12, 14))

        section(page, "💧  WATER INTAKE")
        water = number("Water consumed today (ml)", "2500"); page.addView(water)
        section(page, "🥤  MORNING JUICE")
        juiceName = spinner(juices); page.addView(juiceName)
        juice = check("I had today's juice"); page.addView(juice)
        juiceMl = number("Quantity (ml)", "250"); page.addView(juiceMl)

        section(page, "💊  SUPPLEMENTS")
        omega = check("Omega-3 taken"); page.addView(omega)
        creatine = check("Creatine taken"); page.addView(creatine)
        multi = check("Multivitamin taken"); page.addView(multi)
        magnesium = check("Night magnesium taken"); page.addView(magnesium)

        section(page, "🏋️  GYM & FITNESS")
        gym = check("Gym / physical activity completed"); page.addView(gym)
        workout = spinner(workouts); page.addView(workout)
        minutes = number("Workout duration (minutes)", "45"); page.addView(minutes)
        intensity = spinner(intensities); page.addView(intensity)
        workoutNotes = notes("Workout notes: exercises, sets, reps…"); page.addView(workoutNotes)

        section(page, "📝  DAILY NOTES")
        dailyNotes = notes("How did you feel today?"); page.addView(dailyNotes)

        val save = button("SAVE DAILY ENTRY") { saveEntry() }.apply {
            setBackgroundColor(leaf); setTextColor(Color.WHITE); textSize = 15f
        }
        page.addView(save, params(18, 10))
        page.addView(button("MONTHLY PROGRESS & HISTORY") { monthlyReport() })
        page.addView(TextView(this).apply {
            text = "Saved privately on this phone. Android automatic backup depends on the device backup settings."
            textSize = 11f; setTextColor(Color.GRAY); setPadding(4, 14, 4, 10)
        })
        scroll.addView(page)
        setContentView(scroll)
        loadDate(selectedDate)
    }

    private fun section(parent: LinearLayout, title: String) {
        parent.addView(TextView(this).apply {
            text = title; textSize = 15f; setTextColor(forest); setPadding(0, 18, 0, 8)
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
    }
    private fun check(label: String) = CheckBox(this).apply { text = label; textSize = 14f; setTextColor(Color.DKGRAY) }
    private fun number(h: String, value: String) = EditText(this).apply {
        hint = h; setText(value); inputType = 2; textSize = 14f; setSingleLine(true); setPadding(12, 10, 12, 10)
    }
    private fun notes(h: String) = EditText(this).apply { hint = h; minLines = 2; maxLines = 4; textSize = 14f }
    private fun spinner(items: Array<String>) = Spinner(this).apply {
        adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, items)
    }
    private fun button(label: String, action: () -> Unit) = Button(this).apply {
        text = label; isAllCaps = false; textSize = 12f; setOnClickListener { action() }
    }
    private fun params(top: Int, bottom: Int) = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, top, 0, bottom) }

    private fun loadDate(date: String) {
        selectedDate = date
        lifecycleScope.launch {
            entry = dao.get(date) ?: DailyEntry(date = date)
            dateText.text = "Daily check-in · ${pretty(date)}"
            water.setText(entry.waterMl.toString())
            juice.isChecked = entry.juiceTaken
            juiceName.setSelection(juices.indexOf(entry.juiceName).coerceAtLeast(0))
            juiceMl.setText(entry.juiceMl.toString())
            omega.isChecked = entry.omega3; creatine.isChecked = entry.creatine
            multi.isChecked = entry.multivitamin; magnesium.isChecked = entry.magnesium
            gym.isChecked = entry.gymAttended
            workout.setSelection(workouts.indexOf(entry.workoutType).coerceAtLeast(0))
            minutes.setText(entry.workoutMinutes.toString())
            intensity.setSelection(intensities.indexOf(entry.intensity).coerceAtLeast(0))
            workoutNotes.setText(entry.workoutNotes); dailyNotes.setText(entry.dailyNotes)
            updateSummary()
        }
    }

    private fun saveEntry() {
        val w = water.text.toString().toIntOrNull() ?: 0
        val q = juiceMl.text.toString().toIntOrNull() ?: 0
        val mins = minutes.text.toString().toIntOrNull() ?: 0
        if (w < 0 || q < 0 || mins < 0) { Toast.makeText(this, "Enter valid quantities.", Toast.LENGTH_SHORT).show(); return }
        entry = DailyEntry(
            date = selectedDate, waterMl = w, juiceTaken = juice.isChecked,
            juiceName = juiceName.selectedItem.toString(), juiceMl = q,
            omega3 = omega.isChecked, creatine = creatine.isChecked,
            multivitamin = multi.isChecked, magnesium = magnesium.isChecked,
            gymAttended = gym.isChecked, workoutType = workout.selectedItem.toString(),
            workoutMinutes = if (gym.isChecked) mins else 0,
            intensity = intensity.selectedItem.toString(),
            workoutNotes = workoutNotes.text.toString(), dailyNotes = dailyNotes.text.toString()
        )
        lifecycleScope.launch {
            dao.save(entry); updateSummary()
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(dailyNotes.windowToken, 0)
            Toast.makeText(this@MainActivity, "Saved to this phone.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateSummary() {
        val done = listOf(entry.waterMl >= 2500, entry.juiceTaken, entry.omega3, entry.creatine,
            entry.multivitamin, entry.magnesium, entry.gymAttended).count { it }
        summary.text = "TODAY'S CHECK-IN     $done / 7 habits completed"
    }
    private fun shiftDate(delta: Int) {
        val c = Calendar.getInstance()
        c.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDate)!!
        c.add(Calendar.DAY_OF_MONTH, delta)
        loadDate(SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.time))
    }
    private fun chooseDate() {
        val c = Calendar.getInstance()
        c.time = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDate)!!
        DatePickerDialog(this, { _, y, m, d -> loadDate(String.format(Locale.US, "%04d-%02d-%02d", y, m + 1, d)) },
            c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
    }
    private fun monthlyReport() {
        lifecycleScope.launch {
            val prefix = selectedDate.substring(0, 7)
            val rows = dao.month(prefix)
            val gymDays = rows.count { it.gymAttended }
            val workoutMinutes = rows.sumOf { if (it.gymAttended) it.workoutMinutes else 0 }
            val waterDays = rows.count { it.waterMl >= 2500 }
            val juiceDays = rows.count { it.juiceTaken }
            val report = "Month: $prefix\n\nSaved days: ${rows.size}\nGym/activity days: $gymDays\nWorkout duration: $workoutMinutes minutes\nWater target days (2.5 L): $waterDays\nJuice days: $juiceDays"
            androidx.appcompat.app.AlertDialog.Builder(this@MainActivity)
                .setTitle("Monthly progress").setMessage(report).setPositiveButton("Close", null).show()
        }
    }
    private fun pretty(date: String): String {
        val d = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: return date
        return SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(d)
    }
    private fun today() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Calendar.getInstance().time)
}