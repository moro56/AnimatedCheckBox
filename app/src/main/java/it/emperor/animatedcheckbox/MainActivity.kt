package it.emperor.animatedcheckbox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        button.setOnClickListener {
            performClick()
        }

        radioGroup.setOnCheckedChangeListener { _, i ->
            when (i) {
                R.id.radioButton -> updateAnimationDuration(250L)
                R.id.radioButton2 -> updateAnimationDuration(500L)
                R.id.radioButton3 -> updateAnimationDuration(1000L)
                R.id.radioButton4 -> updateAnimationDuration(2000L)
                R.id.radioButton5 -> updateAnimationDuration(3000L)
            }

        }
    }

    fun updateAnimationDuration(duration: Long) {
        animatedcheckbox.updateDuration(duration)
        animatedcheckbox1.updateDuration(duration)
        animatedcheckbox2.updateDuration(duration)
        animatedcheckbox3.updateDuration(duration)
        animatedcheckbox11.updateDuration(duration)
    }

    fun performClick() {
        animatedcheckbox.performClick()
        animatedcheckbox1.performClick()
        animatedcheckbox2.performClick()
        animatedcheckbox3.performClick()
        animatedcheckbox11.performClick()
    }
}
