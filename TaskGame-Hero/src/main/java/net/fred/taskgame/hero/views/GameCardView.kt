/*
 * Copyright (c) 2012-2017 Frederic Julian
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http:></http:>//www.gnu.org/licenses/>.
 */

package net.fred.taskgame.hero.views

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.util.Property
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import net.fred.taskgame.hero.R
import net.fred.taskgame.hero.models.Card
import java.util.*

class GameCardView : FrameLayout {

    constructor(context: Context) : super(context) {
        inflateView(context, null, 0)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        inflateView(context, attrs, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        inflateView(context, attrs, defStyleAttr)
    }

    private val creature_icon: ImageView by lazy { getChildAt(0).findViewById(R.id.icon) as ImageView }
    private val creature_needed_slots: TextView by lazy { getChildAt(0).findViewById(R.id.needed_slots) as TextView }
    private val creature_name: TextView by lazy { getChildAt(0).findViewById(R.id.name) as TextView }
    private val creature_desc: TextView by lazy { getChildAt(0).findViewById(R.id.desc) as TextView }
    private val creature_speciality: TextView by lazy { getChildAt(0).findViewById(R.id.speciality) as TextView }
    private val creature_attack: TextView by lazy { getChildAt(0).findViewById(R.id.attack) as TextView }
    private val creature_defense: TextView by lazy { getChildAt(0).findViewById(R.id.defense) as TextView }

    private val support_icon: ImageView by lazy { getChildAt(1).findViewById(R.id.icon) as ImageView }
    private val support_needed_slots: TextView by lazy { getChildAt(1).findViewById(R.id.needed_slots) as TextView }
    private val support_name: TextView by lazy { getChildAt(1).findViewById(R.id.name) as TextView }
    private val support_desc: TextView by lazy { getChildAt(1).findViewById(R.id.desc) as TextView }

    var card: Card? = null
        set(card) {
            field = card
            if (card == null) {
                visibility = View.INVISIBLE
            } else if (card.type == Card.Type.CREATURE) {
                visibility = View.VISIBLE
                getChildAt(0).visibility = View.VISIBLE
                getChildAt(1).visibility = View.GONE
                updateUI()
            } else {
                visibility = View.VISIBLE
                getChildAt(0).visibility = View.GONE
                getChildAt(1).visibility = View.VISIBLE
                updateUI()
            }
        }

    private fun inflateView(context: Context, attrs: AttributeSet?, defStyle: Int) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.GameCardView, defStyle, 0)
        val useLargeLayout = a.getBoolean(R.styleable.GameCardView_useLargeLayout, false)
        a.recycle()

        if (!isInEditMode) {
            val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            if (!useLargeLayout) {
                inflater.inflate(R.layout.view_generic_card, this, true)
            } else {
                inflater.inflate(R.layout.view_generic_card_large, this, true)
            }
            getChildAt(1).visibility = View.GONE
        }

        updateUI()
    }

    private fun updateUI() {
        card?.let {

            if (it.type == Card.Type.CREATURE) {
                creature_icon.setImageResource(it.iconResId)
                creature_needed_slots.text = it.neededSlots.toString()
                creature_name.text = it.name
                creature_desc.text = it.desc
                creature_speciality.text = if (it.useMagic) "Use magic" else if (it.useWeapon) "Use weapon" else ""
                creature_attack.text = it.attack.toString()
                creature_defense.text = it.defense.toString()
            } else {
                support_icon.setImageResource(it.iconResId)
                support_needed_slots.text = it.neededSlots.toString()
                support_name.text = it.name
                support_desc.text = it.desc
            }
        }
    }

    fun animateValueChange(endAction: Runnable?): Boolean {
        val property = object : Property<TextView, Int>(Int::class.javaPrimitiveType, "textColor") {
            override fun get(v: TextView): Int {
                return v.currentTextColor
            }

            override fun set(v: TextView, value: Int) {
                v.setTextColor(value)
            }
        }

        val animators = ArrayList<Animator>()

        card?.let {
            Integer.valueOf(creature_attack.text.toString())?.let { previousAttack ->
                if (previousAttack > it.attack || previousAttack < it.attack) {
                    creature_attack.text = it.attack.toString()
                    val colorAnim = ObjectAnimator.ofInt(creature_attack, property, if (previousAttack > it.attack) Color.RED else Color.GREEN)
                    colorAnim.setEvaluator(ArgbEvaluator())
                    colorAnim.repeatCount = 1
                    colorAnim.repeatMode = ObjectAnimator.REVERSE
                    val scaleXAnim = ObjectAnimator.ofFloat(creature_attack, "scaleX", 1.5f)
                    scaleXAnim.repeatCount = 1
                    scaleXAnim.repeatMode = ObjectAnimator.REVERSE
                    val scaleYAnim = ObjectAnimator.ofFloat(creature_attack, "scaleY", 1.5f)
                    scaleYAnim.repeatCount = 1
                    scaleYAnim.repeatMode = ObjectAnimator.REVERSE

                    animators.add(colorAnim)
                    animators.add(scaleXAnim)
                    animators.add(scaleYAnim)
                }
            }

            Integer.valueOf(creature_defense.text.toString())?.let { previousDefense ->
                if (previousDefense > it.defense || previousDefense < it.defense) {
                    creature_defense.text = it.defense.toString()
                    val colorAnim = ObjectAnimator.ofInt(creature_defense, property, if (previousDefense > it.defense) Color.RED else Color.GREEN)
                    colorAnim.setEvaluator(ArgbEvaluator())
                    colorAnim.repeatCount = 1
                    colorAnim.repeatMode = ObjectAnimator.REVERSE
                    val scaleXAnim = ObjectAnimator.ofFloat(creature_defense, "scaleX", 1.5f)
                    scaleXAnim.repeatCount = 1
                    scaleXAnim.repeatMode = ObjectAnimator.REVERSE
                    val scaleYAnim = ObjectAnimator.ofFloat(creature_defense, "scaleY", 1.5f)
                    scaleYAnim.repeatCount = 1
                    scaleYAnim.repeatMode = ObjectAnimator.REVERSE

                    animators.add(colorAnim)
                    animators.add(scaleXAnim)
                    animators.add(scaleYAnim)
                }
            }
        }

        if (!animators.isEmpty()) {
            val animSet = AnimatorSet()
            if (endAction != null) {
                animSet.addListener(object : Animator.AnimatorListener {
                    override fun onAnimationStart(animator: Animator) {}

                    override fun onAnimationEnd(animator: Animator) {
                        endAction.run()
                    }

                    override fun onAnimationCancel(animator: Animator) {}

                    override fun onAnimationRepeat(animator: Animator) {}
                })
            }
            animSet.playTogether(animators)
            animSet.start()
            return true
        }
        return false
    }
}
