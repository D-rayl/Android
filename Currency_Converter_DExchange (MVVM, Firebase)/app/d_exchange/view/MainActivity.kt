package dev.daryl.d_exchange.view

import android.R.color
import android.content.Context
import android.content.res.Resources
import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProviders
import dev.daryl.d_exchange.R
import dev.daryl.d_exchange.R.array.codes
import dev.daryl.d_exchange.R.array.currencies
import dev.daryl.d_exchange.databinding.ActivityMainBinding
import dev.daryl.d_exchange.viewModel.Market
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.reflect.Field


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), CurrenciesFragment.OnListFragmentInteractionListener {
    private lateinit var market: Market
    private var currencyFrag: Fragment = CurrenciesFragment()
    private lateinit var man : FragmentTransaction
    private lateinit var listAll: Button
    private lateinit var codePicker1: NumberPicker
    private lateinit var codePicker2: NumberPicker
    private var pickerMaxValue: Int = 0
    private val mBinding: ActivityMainBinding by lazy {
        DataBindingUtil.setContentView<ActivityMainBinding>(this,
            R.layout.activity_main
        )
    }

    override fun onCreate(state: Bundle?) {
       if(state != null && state.getBoolean("dark")) {
           var theme: Resources.Theme = super.getTheme()
           theme.applyStyle(R.style.DarkTheme, true)
       }
        super.onCreate(state)
        ConHelp.conGet = {this}
        market = ViewModelProviders.of(this)[Market::class.java]
        mBinding.data = market
        mBinding.lifecycleOwner = this

        val currencies = resources.getStringArray(currencies)
        val codes = resources.getStringArray(codes)
        market.passInPickerValues(codes, currencies)
        if(state == null && Market.pref.darkMode){
            recreate()
        }
        listAll = listAll_btn
        listAll.setOnClickListener {
            if(Market.exchange.base != "null"){
                            market.getConversionList()
                                        listCurrencies(false)
            }else{
                Toast.makeText(this, "Requires exchange rates", Toast.LENGTH_SHORT).show()
            }
        }
        codePicker1 = code_picker1
        codePicker2 = code_picker2
        setNumberPickers()

        var inputValue: EditText = val1_et
        inputValue.setOnClickListener { inputValue.selectAll() }
        inputValue.addTextChangedListener(object: TextWatcher{
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun afterTextChanged(p0: Editable?) {
                market.doConversion(codePicker1.value, codePicker2.value)
            }
        })

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("dark", Market.pref.darkMode)
        market.saveInternalStorage("pref")
        when {
            currencyFrag.isVisible -> {
                outState.putBoolean("frag", true)
            }
        }
    }

    private fun setNumberPickers(){
        pickerMaxValue = Market.pref.prefCodes.size-1
        codePicker1.displayedValues = null
        codePicker1.minValue = 0
        codePicker1.maxValue = pickerMaxValue
        codePicker1.value = Market.pref.prefCodes.keys.indexOf(Market.pref.fromCurrency)
        codePicker1.wrapSelectorWheel = true
        codePicker1.displayedValues = Market.pref.prefCodes.keys.toTypedArray()
        codePicker2.displayedValues = null
        codePicker2.minValue = 0
        codePicker2.maxValue = pickerMaxValue
        codePicker2.value = Market.pref.prefCodes.keys.indexOf(Market.pref.toCurrency)
        codePicker2.wrapSelectorWheel = true
        codePicker2.displayedValues = Market.pref.prefCodes.keys.toTypedArray()

        codePicker1.setOnValueChangedListener { _: NumberPicker, last: Int, current: Int ->
            market.doConversion(current, codePicker2.value)
        }
        codePicker2.setOnValueChangedListener{ _: NumberPicker, last: Int, current: Int ->
            market.doConversion(codePicker1.value, current)
        }
        if(Market.pref.darkMode){
            val color = resources.getColor(R.color.Green)
            setPickersColor(codePicker1,color)
            setPickersColor(codePicker2,color)
        }
    }

    private fun setPickersColor(picker: NumberPicker, color: Int){
        for(i in 0..picker.childCount){
            var child = picker.getChildAt(i)
            if(child is EditText){
                val selectorWheelPaintField: Field = picker.javaClass
                    .getDeclaredField("mSelectorWheelPaint")
                selectorWheelPaintField.isAccessible = true
                (selectorWheelPaintField[picker] as Paint).color = (color)
                child.setTextColor(color)
                picker.invalidate()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        super.onCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        super.onOptionsItemSelected(item)
        when(item.itemId){
            R.id.dark -> changeMode(true)
            R.id.light -> changeMode(false)
            R.id.currencies -> listCurrencies(true)
        }
        return true
    }

    private fun listCurrencies(justCurrencies: Boolean) {
        if(currencyFrag.isVisible){
            removeFragment()
        }
        market.justCurrencies = justCurrencies
        man = supportFragmentManager.beginTransaction()
        man.add(R.id.frag_container, currencyFrag)
        man.commit()
    }

    override fun onBackPressed() {
        if(currencyFrag.isVisible){
            removeFragment()
        }else
            super.onBackPressed()
        market.saveInternalStorage("pref")
    }

    private fun removeFragment(){
        man = supportFragmentManager.beginTransaction()
        man.remove(currencyFrag)
        man.commitNow()
    }

    private fun changeMode(dark: Boolean) {
        if(currencyFrag.isVisible){
            removeFragment()
        }
        Market.pref.darkMode = dark
        recreate()
    }

    override fun onListFragmentInteraction(currency: String) {
        market.setPref(currency, callback = {setNumberPickers()
                                                market.doConversion(0,1)})
    }
}
object ConHelp{
    var conGet: (() -> Context)? = null
}

/**override fun getTheme(): Resources.Theme {
    val theme = super.getTheme()
    when(Market.pref.darkMode){
        true -> {
            theme.applyStyle(R.style.DarkTheme, true)
        }
        false -> {
            theme.applyStyle(R.style.AppTheme, true)
        }
    }
    return theme
}*/