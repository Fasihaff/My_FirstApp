package protect.budgetwatch.intro

import android.os.Bundle
import androidx.fragment.app.Fragment
import com.github.paolorotolo.appintro.AppIntro


class IntroActivity : AppIntro()
{
   override fun onCreate (savedInstanceState: Bundle?)
   {
       super.onCreate(savedInstanceState)
       addSlide(IntroSlide1())
        addSlide(IntroSlide2())
        addSlide(IntroSlide3())
        addSlide(IntroSlide4())
        addSlide(IntroSlide5())
        addSlide(IntroSlide6())
        addSlide(IntroSlide7())
    }

    override fun onSkipPressed(fragment: Fragment) {
        finish()
    }

    override fun onDonePressed(fragment: Fragment) {
        finish()
    }
}