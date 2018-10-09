package it.emperor.animatedcheckbox.binding

import androidx.databinding.BindingAdapter
import it.emperor.animatedcheckbox.AnimatedCheckBox

@BindingAdapter("onChange")
fun AnimatedCheckBox.setListener(onChange: (checked: Boolean) -> Unit = {}) {
    this.setOnChangeListener(onChange)
}