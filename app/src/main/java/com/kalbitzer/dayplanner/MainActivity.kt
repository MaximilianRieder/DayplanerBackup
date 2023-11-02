package com.kalbitzer.dayplanner

import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.TextView
import android.widget.inline.InlineContentView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager.widget.ViewPager
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.kalbitzer.dayplanner.TimeUtil.isSameDay
import com.kalbitzer.dayplanner.database.Appointment
import com.kalbitzer.dayplanner.database.AppointmentViewModel
import com.kalbitzer.dayplanner.databinding.ActivityMainBinding
import com.kalbitzer.dayplanner.databinding.FragmentDayViewBinding
import java.util.*
import java.util.concurrent.CountDownLatch


class MainActivity : AppCompatActivity() {
    val numDays = 7
    private lateinit var _binding: ActivityMainBinding
    private lateinit var viewPagerAdapter: ViewPagerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater);
        setContentView(_binding.root);
        val contentView = _binding.root
        val viewPager: ViewPager = _binding.viewPager
        val nextButton: Button = _binding.btnNext
        val prevButton: Button = _binding.btnPrevious
        val dayText: TextView = _binding.textView
        viewPagerAdapter = ViewPagerAdapter(supportFragmentManager)
        initViewPager(viewPager)
        initDaySwitchButtons()

    }

    private fun initViewPager(viewPager: ViewPager){
        val relevantDates: List<Date> = TimeUtil.getDatesList(numDays)
        for (date in relevantDates) {
            val dayViewFragment = DayViewFragment(date)
            viewPagerAdapter.addFragment(dayViewFragment, TimeUtil.parseDateToString(date))
        }
        viewPager.adapter = viewPagerAdapter
        viewPager.offscreenPageLimit = 2
        //check if after 21:00 and if so, set to tomorrow
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        if(hour >= 21){
            viewPager.currentItem =numDays + 1
        }
        else{
            viewPager.currentItem = numDays
        }
    }
    
    private fun initDaySwitchButtons(){
        _binding.btnNext.setOnClickListener {
            _binding.viewPager.currentItem = _binding.viewPager.currentItem + 1
            _binding.textView.text = TimeUtil.parseDateToString(viewPagerAdapter.getItem(_binding.viewPager.currentItem).date)
        }
        _binding.btnPrevious.setOnClickListener {
            _binding.viewPager.currentItem = _binding.viewPager.currentItem - 1
            _binding.textView.text = TimeUtil.parseDateToString(viewPagerAdapter.getItem(_binding.viewPager.currentItem).date)
        }
    }

    private inner class ViewPagerAdapter(manager: FragmentManager) :
    // had to use viewPager instead of viewPager2 to override onInterceptTouchEvent
        FragmentPagerAdapter(
            manager
        ) { // todo schauen ob das sinn macht
        private val fragmentList = ArrayList<DayViewFragment>()
        private val titleList = ArrayList<String>()

        override fun getCount(): Int {
            return fragmentList.size
        }

        override fun getItem(position: Int): DayViewFragment {
            return fragmentList[position]
        }

        fun addFragment(fragment: DayViewFragment, title: String) {
            fragmentList.add(fragment)
            titleList.add(title)
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return titleList[position]
        }

        fun getFragmentList(): ArrayList<DayViewFragment> {
            return fragmentList
        }

    }

}