package did.pinbraerts.tgchart

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.*
import android.widget.ArrayAdapter
import android.widget.CheckBox
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import android.content.res.ColorStateList
import android.graphics.Color
import android.support.v4.widget.CompoundButtonCompat
import android.support.v7.widget.AppCompatCheckBox


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
                val view: AppCompatCheckBox = convertView as AppCompatCheckBox? ?: AppCompatCheckBox(context)
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
                view.isChecked = true
                return view
            }
        }
        columns.adapter = adapter
        columns.setOnItemClickListener { adapterView, view, i, l ->
            if(view is CheckBox) {
                chart.columnsToShow.set(i, view.isChecked)
                chart.invalidate()
            }
        }

        LoadScope().load(resources.openRawResource(R.raw.chart_data)) {
            synchronized(lock) {
                currentPage = it
            }
            runOnUiThread {
                pages.add(currentPage)
                chart.updatePage(currentPage)
                adapter.clear()
                adapter.addAll(chart.yColumns.asList())
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem) = when(item.itemId) {
        R.id.night_mode_on -> {
            TODO("change theme")
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.toolbar_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }
}
