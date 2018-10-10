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

        radioGroup.setOnCheckedChangeListener { _, id ->
            when (id) {
                R.id.radioButton -> updateAnimationDuration(250L)
                R.id.radioButton2 -> updateAnimationDuration(500L)
                R.id.radioButton3 -> updateAnimationDuration(1000L)
                R.id.radioButton4 -> updateAnimationDuration(2000L)
                R.id.radioButton5 -> updateAnimationDuration(3000L)
            }
        }

        checkBox.setOnCheckedChangeListener { _, checked ->
            animatedcheckbox.ignoreAnimation = checked
            animatedcheckbox1.ignoreAnimation = checked
            animatedcheckbox2.ignoreAnimation = checked
            animatedcheckbox3.ignoreAnimation = checked
            animatedcheckbox11.ignoreAnimation = checked
        }
    }

    private fun updateAnimationDuration(duration: Long) {
        animatedcheckbox.duration = duration
        animatedcheckbox1.duration = duration
        animatedcheckbox2.duration = duration
        animatedcheckbox3.duration = duration
        animatedcheckbox11.duration = duration
    }

    private fun performClick() {
        animatedcheckbox.performClick()
        animatedcheckbox1.performClick()
        animatedcheckbox2.performClick()
        animatedcheckbox3.performClick()
        animatedcheckbox11.performClick()
    }
}
