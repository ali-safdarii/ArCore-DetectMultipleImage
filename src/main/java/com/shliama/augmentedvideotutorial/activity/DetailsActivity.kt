package com.shliama.augmentedvideotutorial.activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.constraintlayout.widget.ConstraintSet
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.rm.rmswitch.RMTristateSwitch
import com.shliama.augmentedvideotutorial.MainActivity
import com.shliama.augmentedvideotutorial.R
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.android.synthetic.main.activity_details.*
import kotlinx.android.synthetic.main.park_or_company_activity.*
import render.animations.*

class DetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        image_3.setOnClickListener {

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        val constraintSet=ConstraintSet()




        constriant_details.setOnTouchListener(object : OnSwipeTouchListener(this) {
            override fun onSwipeLeft() {

              //  imageView.visibility=View.VISIBLE
                profile_image.visibility = View.VISIBLE
                avrImage.visibility = View.VISIBLE
                image_1.visibility = View.VISIBLE
                image_2.visibility = View.VISIBLE
                image_3.visibility = View.VISIBLE

                val render = Render(this@DetailsActivity)
                val render2 = Render(this@DetailsActivity)
                val render3 = Render(this@DetailsActivity)
                val render4 = Render(this@DetailsActivity)
                val render5 = Render(this@DetailsActivity)
                val render6 = Render(this@DetailsActivity)

               // render.setAnimation(Slide().InRight(imageView))
                render2.setAnimation(Fade().In(profile_image))
                render3.setAnimation(Fade().In(avrImage))
                render4.setAnimation(Fade().In(image_1))
                render5.setAnimation(Fade().In(image_2))
                render6.setAnimation(Fade().In(image_3))



              //  render.start()
                render2.start()
                render3.start()
                render4.start()
                render5.start()
                render6.start()

            }

            override fun onSwipeRight() {

                profile_image.visibility = View.GONE
                avrImage.visibility = View.GONE
                image_1.visibility = View.GONE
                image_2.visibility = View.GONE
                image_3.visibility = View.GONE
             //   imageView.visibility=View.GONE
            }
        })




    }
}