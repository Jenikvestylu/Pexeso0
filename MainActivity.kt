package com.example.pexeso

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    // 16 symbolů = 16 párů = 32 karet celkem
    private val symbols = (1..16).map { "$it" }

    private lateinit var grid: GridLayout
    private lateinit var tvInfo: TextView
    private lateinit var tvBest: TextView

    private val deck   = MutableList(32) { "" }
    private val faceUp = BooleanArray(32)
    private val matched = BooleanArray(32)

    private var firstIndex = -1
    private var moves      = 0
    private var foundPairs = 0
    private var busy       = false

    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        grid   = findViewById(R.id.grid)
        tvInfo = findViewById(R.id.tvInfo)
        tvBest = findViewById(R.id.tvBest)

        findViewById<Button>(R.id.btnRestart).setOnClickListener { startNewGame() }
        startNewGame()
    }

    // -------------------------------------------------------------------------

    private fun startNewGame() {
        moves = 0; foundPairs = 0; firstIndex = -1; busy = false
        faceUp.fill(false)
        matched.fill(false)

        // Zamíchej 2× každý symbol
        val shuffled = (symbols + symbols).shuffled()
        shuffled.forEachIndexed { i, value -> deck[i] = value }

        // Vytvoř tlačítka karet
        grid.removeAllViews()
        repeat(32) { i -> grid.addView(createCard(i)) }

        updateInfo()
        updateBest()
    }

    private fun createCard(index: Int): Button {
        val button = Button(this).apply {
            text = "?"
            textSize = 20f
            setBackgroundColor(Color.parseColor("#1565C0"))
            setTextColor(Color.WHITE)
            setOnClickListener { onCardClick(index) }
        }
        val params = GridLayout.LayoutParams(
            GridLayout.spec(index / 4, 1f),
            GridLayout.spec(index % 4, 1f)
        ).apply { setMargins(4, 4, 4, 4) }
        button.layoutParams = params
        return button
    }

    // -------------------------------------------------------------------------

    private fun onCardClick(index: Int) {
        if (busy || matched[index] || faceUp[index] || index == firstIndex) return

        revealCard(index, true)

        if (firstIndex == -1) {
            // První karta otočena – čekej na druhou
            firstIndex = index
        } else {
            // Druhá karta otočena – porovnej
            moves++
            busy = true
            val first = firstIndex
            firstIndex = -1

            if (deck[first] == deck[index]) {
                onMatch(first, index)
            } else {
                onMismatch(first, index)
            }
            updateInfo()
        }
    }

    private fun onMatch(a: Int, b: Int) {
        matched[a] = true
        matched[b] = true
        foundPairs++
        busy = false
        cardAt(a).setBackgroundColor(Color.parseColor("#388E3C"))
        cardAt(b).setBackgroundColor(Color.parseColor("#388E3C"))

        if (foundPairs == 16) onGameWon()
    }

    private fun onMismatch(a: Int, b: Int) {
        // Po 900 ms otočit karty zpět
        Handler(Looper.getMainLooper()).postDelayed({
            revealCard(a, false)
            revealCard(b, false)
            busy = false
        }, 900)
    }

    private fun onGameWon() {
        val prefs = getSharedPreferences("pexeso", MODE_PRIVATE)
        val best  = prefs.getInt("best", 0)
        if (best == 0 || moves < best) prefs.edit().putInt("best", moves).apply()
        tvInfo.text = "🎉 Hotovo za $moves tahů!"
        updateBest()
    }

    // -------------------------------------------------------------------------

    private fun revealCard(index: Int, show: Boolean) {
        faceUp[index] = show
        cardAt(index).apply {
            text = if (show) deck[index] else "?"
            setBackgroundColor(if (show) Color.parseColor("#E3F2FD") else Color.parseColor("#1565C0"))
            setTextColor(if (show) Color.BLACK else Color.WHITE)
        }
    }

    private fun cardAt(index: Int) = grid.getChildAt(index) as Button

    private fun updateInfo() {
        tvInfo.text = "Tahy: $moves  |  Páry: $foundPairs / 16"
    }

    private fun updateBest() {
        val best = getSharedPreferences("pexeso", MODE_PRIVATE).getInt("best", 0)
        tvBest.text = "Nejlepší: ${if (best == 0) "-" else best}"
    }
}
