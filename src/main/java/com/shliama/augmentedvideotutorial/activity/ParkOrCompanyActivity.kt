package com.shliama.augmentedvideotutorial.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat

import com.rm.rmswitch.RMTristateSwitch
import com.rm.rmswitch.RMTristateSwitch.RMTristateSwitchObserver
import com.shliama.augmentedvideotutorial.R
import kotlinx.android.synthetic.main.park_or_company_activity.*


class ParkOrCompanyActivity : AppCompatActivity(), RMTristateSwitchObserver {



    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.park_or_company_activity)
        switchButton.addSwitchObserver(this@ParkOrCompanyActivity)
        switchButton.setSwitchToggleMiddleColor(Color.BLUE);

        switchButton.setOnTouchListener(object : OnSwipeTouchListener(this) {
             override fun onSwipeLeft() {
                 switchButton.state = RMTristateSwitch.STATE_LEFT
                 val intent = Intent(this@ParkOrCompanyActivity, DetailsActivity::class.java)
                 startActivity(intent)

             }

             override fun onSwipeRight() {

                 switchButton.state = RMTristateSwitch.STATE_RIGHT
                 val intent = Intent(this@ParkOrCompanyActivity, DemoActivity::class.java)
                 startActivity(intent)


             }
         })







    }

    override fun onCheckStateChange(switchView: RMTristateSwitch?, state: Int) {
        when (state) {
            RMTristateSwitch.STATE_LEFT -> {

            }

            RMTristateSwitch.STATE_RIGHT -> {

            }

            RMTristateSwitch.STATE_MIDDLE -> {

            }
        }
    }

    override fun onStart() {
        super.onStart()
        switchButton.state = RMTristateSwitch.STATE_MIDDLE
    }
}