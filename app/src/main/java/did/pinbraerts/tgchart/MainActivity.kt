package did.pinbraerts.tgchart

import android.content.res.ColorStateList
import android.os.Bundle
import android.support.v4.widget.CompoundButtonCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.AppCompatCheckBox
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    var pages = Pages()
    var currentPage = Page()

    val lock = Any()

    lateinit var adapter: ArrayAdapter<Column>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        adapter = object: ArrayAdapter<Column>(this, android.R.layout.simple_list_item_checked) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view: AppCompatCheckBox = convertView as AppCompatCheckBox? ?:
                    layoutInflater.inflate(R.layout.list_item, parent, false) as AppCompatCheckBox
                val column = getItem(position) ?: throw IllegalArgumentException("No such item")
                view.text = column.name
                val colorStateList = ColorStateList(
                    arrayOf(
                        intArrayOf(-android.R.attr.state_checked), // unchecked
                        intArrayOf(android.R.attr.state_checked)  // checked
                    ),
                    intArrayOf(column.color, column.color)
                )
                CompoundButtonCompat.setButtonTintList(view, colorStateList)

                view.setOnCheckedChangeListener { _, checked ->
                    chart.setColumnToShow(position, checked)
                    chart.updateDimensions(currentPage["x"]!!)
                }
                return view
            }
        }
        columns.adapter = adapter

        LoadScope().load(resources.openRawResource(R.raw.chart_data)) {
            synchronized(lock) {
                currentPage = it
            }
            runOnUiThread {
                pages.add(currentPage)
                chart.updatePage(currentPage)
                adapter.clear()
                adapter.addAll(currentPage.filter { it.key != "x" }.values)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when(item.itemId) {
        R.id.night_mode_on -> {
            when(AppCompatDelegate.getDefaultNightMode()) {
                AppCompatDelegate.MODE_NIGHT_NO ->
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO)
                AppCompatDelegate.MODE_NIGHT_YES ->
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            }
            recreate()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.run {
            putLong("abscissa.start", chart.abscissa.start)
            putLong("abscissa.end", chart.abscissa.end)
        }
    }

    override fun onRestoreInstanceState(state: Bundle) {
        super.onRestoreInstanceState(state)

        chart.abscissa.start = state["abscissa.start"] as Long
        chart.abscissa.end = state["abscissa.end"] as Long
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}
