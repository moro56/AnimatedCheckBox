package it.emperor.animatedcheckbox.binding

import androidx.databinding.BindingAdapter
import it.emperor.animatedcheckbox.AnimatedCheckBox

@BindingAdapter("checked")
fun AnimatedCheckBox.setChecked(checked: Boolean) {
    this.updateState(checked)
}

@BindingAdapter("onChange")
fun AnimatedCheckBox.setListener(onChange: (checked: Boolean) -> Unit = {}) {
    this.setOnChangeListener(onChange)
}