package com.kalbitzer.dayplanner

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.kalbitzer.dayplanner.database.Appointment
import com.kalbitzer.dayplanner.database.AppointmentViewModel
import com.kalbitzer.dayplanner.databinding.FragmentDayViewBinding
import java.time.LocalDate
import java.util.*

class DayViewFragment(val date: Date) : Fragment() {
    private var _binding: FragmentDayViewBinding? = null
    private val binding get() = _binding!!
    private lateinit var mAppointmentViewModel: AppointmentViewModel
    private var tileSize: Float = 0f
    private var dottedLineView: DottedLineView? = null
    var dbAppointments: List<Appointment>? = null
    var isInEditText = false
    var previousSelectedSS: SelectorSquare? = null
    var openedDeleteDialog: Boolean = false
    private var timer: Timer? = null
    var isViewLoaded = false
    var isDbLoaded = false

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentDayViewBinding.inflate(inflater, container, false)
        val view = binding.root
        mAppointmentViewModel = ViewModelProvider(this)[AppointmentViewModel::class.java]
        mAppointmentViewModel.loadAppointmentsForDate(date)

        // Calculate tilesize and add hour tiles
        val screenHeight = getScreenHeightInPixels()!!
        val viewHeightTile = (screenHeight / 24)
        tileSize = viewHeightTile.toFloat()
        SharedVariables.tileSize = tileSize
        val constHourView = binding.constHours
        for (i in 0..23) {
            createAddHourTile(
                inflater,
                viewHeightTile,
                constHourView,
                R.layout.quarter_tile_view_top_border
            )
            createAddHourTile(inflater, viewHeightTile, constHourView, R.layout.quarter_tile_view)
            createAddHourTile(inflater, viewHeightTile, constHourView, R.layout.quarter_tile_view)
            createAddHourTile(
                inflater,
                viewHeightTile,
                constHourView,
                R.layout.quarter_tile_view_bottom_border
            )
        }
        binding.hourSidebarHours.setHourText(tileSize * 4)

        // dotted time line
        dottedLineView = DottedLineView(context)
        binding.hourDropArea.addView(dottedLineView)
        drawCurrentTimeLine()

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Log.e("destroy", "destroyed" + date.toString())
        isViewLoaded = false
        isDbLoaded = false
        mAppointmentViewModel.allAppointments.removeObservers(viewLifecycleOwner)
        _binding = null
    }

    /**
     * Load the appointments from the db and init the selector squares
     * For this both the view and the db have to be loaded
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mAppointmentViewModel.appointmentsForDate.observe(viewLifecycleOwner) {
            dbAppointments = it
            isDbLoaded = true
            if (isViewLoaded) {
                loadAllSelectorSquares(it)
                scrollToCorrectPosition(it)
            }
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                //tileSize = getTileSize(binding)
                setListeners(tileSize)
                isViewLoaded = true
                if (isDbLoaded) {
                    val appointments = mAppointmentViewModel.appointmentsForDate.value
                    loadAllSelectorSquares(appointments)
                    scrollToCorrectPosition(appointments)
                }
            }
        }
        )
    }

    private fun scrollToCorrectPosition(appointments: List<Appointment>?) {
        val scrollPosition = TimeUtil.getScrollPosition(tileSize, date, appointments)
        binding.scrollView.scrollTo(0, scrollPosition.toInt())

    }

    private fun loadAllSelectorSquares(appointments: List<Appointment>?) {
        dbAppointments?.let {
            for (appointment in it) {
                loadSelectorSquareFromDB(appointment)
            }
        }
    }

    /**
     * Code responsible for drawing the time-line every five minutes
     */
    override fun onResume() {
        super.onResume()
        // Schedule the first execution when the fragment is resumed
        scheduleTask()
    }

    override fun onPause() {
        super.onPause()
        // Cancel the timer when the fragment is paused
        cancelTask()
    }

    private fun scheduleTask() {
        timer = Timer()
        val timerTask = object : TimerTask() {
            override fun run() {
                activity?.runOnUiThread {
                    drawCurrentTimeLine()
                }
            }
        }
        timer?.schedule(timerTask, 0, 5 * 60 * 1000)
    }

    private fun cancelTask() {
        timer?.cancel()
        timer = null
    }

    /**
     * draws dotted Line at current time
     */
    fun drawCurrentTimeLine() {
        val totalMinutesInDay = 24 * 60
        //get percentage of the day e.g.: 12:00 = 0.5f
        val dayPercent = CustomTime.getCurrentTime().toMinutes().toFloat() / totalMinutesInDay
        dottedLineView?.setPosition((tileSize * 24 * 4) * dayPercent)
    }

    /**
     * creates and adds one quarter hour tile view to the target view
     */
    private fun createAddHourTile(
        inflater: LayoutInflater,
        viewHeight: Int,
        targetView: ViewGroup,
        addViewId: Int
    ) {
        val quarterTileView =
            inflater.inflate(addViewId, targetView, false)

        val layoutParams = quarterTileView.layoutParams
        layoutParams.height = viewHeight
        quarterTileView.layoutParams = layoutParams

        targetView.addView(quarterTileView)
    }

    /**
     * initialisation of selector squares
     */
    private fun loadSelectorSquareFromDB(appointment: Appointment) {
        val position = TimeUtil.getPixelPositionFromTime(CustomTime(appointment.timeStart), tileSize)
        val size =
            TimeUtil.getSizeFromTime(CustomTime(appointment.timeStart),CustomTime(appointment.timeEnd), tileSize)
        val selSquareNew = SelectorSquare(
            requireContext(),
            null,
            0,
            binding,
            tileSize,
            appointment.date,
            appointment.title,
            position,
            size,
            appointment.id
        )
        for (child in binding.hourDropArea.children) {
            if (child is SelectorSquare) {
                if (child.appointmentId == appointment.id) {
                    //remove old
                    binding.hourDropArea.removeView(child)
                }
            }
        }
        selSquareNew.isSaved = true
        binding.hourDropArea.addView(selSquareNew)
    }

    @SuppressLint("ClickableViewAccessibility")
    fun setListeners(tileSize: Float) {
        var yPosition = 0f
        val touchListener = View.OnTouchListener { v, event -> // save the X,Y coordinates
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                yPosition = event.y
            }
            false
        }
        val longClickListener = View.OnLongClickListener {
            val selSquareNew =
                SelectorSquare(
                    it.context,
                    null,
                    0,
                    binding,
                    tileSize,
                    date,
                    "",
                    yPosition,
                    tileSize
                )
            binding.hourDropArea.addView(selSquareNew)
            selSquareNew.titleField.requestFocus()
        }
        val clickListener = View.OnClickListener {
            removeFocusTextFields(it)
        }
        binding.hourDropArea.setOnTouchListener(touchListener)
        binding.hourDropArea.setOnClickListener(clickListener)
        binding.hourSidebar.setOnClickListener(clickListener)
        binding.hourDropArea.setOnLongClickListener(longClickListener)
    }

    /***
     * removes focus keyboard from textfields and resets flag
     */
    fun removeFocusTextFields(v: View) {
//        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
//        imm.hideSoftInputFromWindow(v.windowToken, 0)
        //remove focus on keyboard when clicked and delete ss with "" content
        for (childView in binding.hourDropArea.children) {
            if (childView is SelectorSquare) {
                childView.titleField.clearFocus()
            }
        }
        isInEditText = false
    }

    // Helper method to get screen height in pixels
    @RequiresApi(Build.VERSION_CODES.R)
    private fun getScreenHeightInPixels(): Int? {
        val windowMetrics: WindowMetrics? = activity?.windowManager?.currentWindowMetrics
        val bounds = windowMetrics?.bounds
        val height = bounds?.height()
        return height
    }

    /**
     * gets the pixelheight of time tile
     */

    private fun getTileSize(binding: FragmentDayViewBinding): Float {
        var tileDelta = 0f
        var yPosPrev = 0f
        for (childView in binding.constHours.children) {
            if (childView is TextView) {
                val offsetViewBounds = Rect()
                childView.getDrawingRect(offsetViewBounds)
                binding.constHours.offsetDescendantRectToMyCoords(
                    childView,
                    offsetViewBounds
                )
                val relativeTop: Int = offsetViewBounds.top
                tileDelta = relativeTop.toFloat() - yPosPrev
                yPosPrev = relativeTop.toFloat()
            }
        }
        return tileDelta
    }

    companion object {
        private const val ARG_TAB_POSITION = "arg_tab_position"

//        fun newInstance(tabPosition: Int): DayViewFragment {
//            val fragment = DayViewFragment(Date.now())
//            val args = Bundle()
//            args.putInt(ARG_TAB_POSITION, tabPosition)
//            fragment.arguments = args
//            return fragment
//        }
    }
}