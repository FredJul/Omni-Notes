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

package net.fred.taskgame.hero.fragments

import android.os.Bundle
import android.os.Parcelable
import android.support.annotation.RawRes
import android.support.design.widget.BottomSheetBehavior
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Toast
import kotlinx.android.synthetic.main.fragment_battle.*
import net.fred.taskgame.hero.R
import net.fred.taskgame.hero.activities.MainActivity
import net.fred.taskgame.hero.logic.BattleManager
import net.fred.taskgame.hero.models.Card
import net.fred.taskgame.hero.models.Level
import net.fred.taskgame.hero.utils.Dog
import net.fred.taskgame.hero.utils.UiUtils
import net.fred.taskgame.hero.views.GameCardView
import net.frju.androidquery.gen.LEVEL
import org.jetbrains.anko.onClick
import org.parceler.Parcels


class BattleFragment : BaseFragment() {

    private var battleManager = BattleManager()
    private var level: Level? = null
    private var selectCardBottomSheetBehavior: BottomSheetBehavior<*>? = null
    private var selectStrategyBottomSheetBehavior: BottomSheetBehavior<*>? = null
    private var currentlyAnimatedCard: GameCardView? = null

    override val mainMusicResId: Int
        @RawRes
        get() {
            level?.let { lvl ->
                if (lvl.battleMusicResId != Level.INVALID_ID) {
                    return lvl.battleMusicResId
                }
            }

            return R.raw.battle_theme
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val layout = inflater.inflate(R.layout.fragment_battle, container, false)

        level = Parcels.unwrap<Level>(arguments.getParcelable<Parcelable>(ARG_LEVEL))

        if (savedInstanceState != null) {
            battleManager = Parcels.unwrap<BattleManager>(savedInstanceState.getParcelable<Parcelable>(STATE_BATTLE_MANAGER))
        } else {
            level?.let { lvl ->
                battleManager.addEnemyCards(lvl.enemyCards)
                val playerCards = Parcels.unwrap<List<Card>>(arguments.getParcelable<Parcelable>(ARG_PLAYER_CARDS))
                battleManager.addPlayerCards(playerCards)
            }
        }

        return layout
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        selectCardBottomSheetBehavior = BottomSheetBehavior.from(select_card_bottom_sheet)
        selectCardBottomSheetBehavior?.peekHeight = UiUtils.dpToPixel(100)

        selectStrategyBottomSheetBehavior = BottomSheetBehavior.from(select_strategy_bottom_sheet)
        selectStrategyBottomSheetBehavior?.peekHeight = UiUtils.dpToPixel(100)

        level?.let { lvl ->
            enemy_portrait.setImageResource(lvl.getEnemyIcon(context))
        }

        updateUI()

        back.onClick { fragmentManager.popBackStack() }
        use_card_button.onClick { onUseCardClicked() }
        attack_strategy_btn.onClick { onAttackStrategyButtonClicked() }
        defense_strategy_btn.onClick { onDefenseStrategyButtonClicked() }
        aleatory_strategy_btn.onClick { onAleatoryStrategyButtonClicked() }
        dark_layer.onClick { onDarkLayerClicked() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putParcelable(STATE_BATTLE_MANAGER, Parcels.wrap(battleManager))
        super.onSaveInstanceState(outState)
    }

    fun onUseCardClicked() {
        stopCardHighlighting()

        currentlyAnimatedCard?.let { animatedCard ->
            animatedCard.card?.let { playedCard ->
                if (playedCard.type == Card.Type.CREATURE) {
                    val cardPaddingStart = animatedCard.paddingStart - select_card_bottom_sheet.scrollX
                    val cardPaddingTop = animatedCard.paddingTop + select_card_bottom_sheet.paddingTop
                    animatedCard.animate().scaleX(1f).scaleY(1f).translationX(0f).translationY(0f)
                            .x(player_card.x - select_card_bottom_sheet.x - cardPaddingStart.toFloat()).y(player_card.y - select_card_bottom_sheet.y - cardPaddingTop.toFloat())
                            .withEndAction {
                                player_card.alpha = 1f // it has maybe been hidden if a creature died
                                player_card.card = animatedCard.card
                                card_list.post(// to avoid flickering
                                        {
                                            card_list.removeView(currentlyAnimatedCard)
                                            currentlyAnimatedCard = null
                                            battleManager.play(playedCard)
                                            animateNextStep()
                                        })
                            }
                } else {
                    animatedCard.animate().alpha(0f)
                            .withEndAction {
                                mainActivity?.playSound(MainActivity.SOUND_USE_SUPPORT)
                                animatedCard.animate().alpha(0f).withEndAction {
                                    card_list.removeView(currentlyAnimatedCard)
                                    currentlyAnimatedCard = null
                                    battleManager.play(playedCard)
                                    animateNextStep()
                                }
                            }

                }
            }
        }
    }

    fun onAttackStrategyButtonClicked() {
        battleManager.applyNewStrategy(BattleManager.BattleStrategy.ATTACK)
        onStrategySelected()
    }

    fun onDefenseStrategyButtonClicked() {
        battleManager.applyNewStrategy(BattleManager.BattleStrategy.DEFENSE)
        onStrategySelected()
    }

    fun onAleatoryStrategyButtonClicked() {
        battleManager.applyNewStrategy(BattleManager.BattleStrategy.ALEATORY)?.let { result ->
            val displayResult = (if (result.bonusOrPenalty > 0) "+" else "") + result.bonusOrPenalty + " of " + if (result.affectedField == BattleManager.AleatoryAffectedField.ATTACK) "attack" else "defense"
            Toast.makeText(context, displayResult, Toast.LENGTH_SHORT).show()
            onStrategySelected()
        }
    }

    private fun animateNextStep() {
        if (mainActivity == null) {
            return   // Do nothing if not correctly attached
        }

        level?.let { lvl ->
            val step = battleManager.executeNextStep()
            when (step) {
                BattleManager.BattleStep.APPLY_PLAYER_SUPPORT -> {
                    val animationInProgress = enemy_card.animateValueChange(Runnable { animateNextStep() })

                    if (animationInProgress) {
                        player_card.animateValueChange(null)
                    } else if (!player_card.animateValueChange(Runnable { animateNextStep() })) {
                        animateNextStep() // if no animation at all, let's do the next step already now
                    } else {
                        // nothing to do
                    }
                }
                BattleManager.BattleStep.SELECT_STRATEGY -> {
                    stopCardHighlighting()
                    updateBottomSheetUI()
                }
                BattleManager.BattleStep.APPLY_ENEMY_SUPPORT -> {
                    enemy_support_card.card = battleManager.lastUsedEnemySupportCard
                    enemy_support_card.alpha = 0f
                    enemy_support_card.visibility = View.VISIBLE
                    enemy_support_card.animate().alpha(1f).scaleX(1.2f).scaleY(1.2f).setDuration(500).withEndAction {
                        mainActivity?.playSound(MainActivity.SOUND_USE_SUPPORT)
                        enemy_support_card.animate().alpha(0f).scaleX(1f).scaleY(1f).withEndAction {
                            enemy_support_card.card = null
                            enemy_support_card.visibility = View.GONE

                            val animationInProgress = enemy_card.animateValueChange(Runnable { animateNextStep() })

                            if (animationInProgress) {
                                player_card.animateValueChange(null)
                            } else {
                                if (!player_card.animateValueChange(Runnable { animateNextStep() })) {
                                    animateNextStep() // If no animation at all, we do it immediately
                                }
                            }
                        }
                    }
                }
                BattleManager.BattleStep.FIGHT -> {
                    var cardsXDiff = (player_card.x - enemy_card.x - player_card.width.toFloat()) / 2
                    cardsXDiff += player_card.width / 6 // add a small superposition
                    var cardsYDiff = (player_card.y - enemy_card.y) / 2
                    cardsYDiff -= player_card.height / 8 // add a small superposition

                    player_card.card?.let { playedCard ->
                        if (playedCard.useMagic) {
                            mainActivity?.playSound(MainActivity.SOUND_FIGHT_MAGIC)
                        } else if (playedCard.useWeapon) {
                            mainActivity?.playSound(MainActivity.SOUND_FIGHT_WEAPON)
                        } else {
                            mainActivity?.playSound(MainActivity.SOUND_FIGHT)
                        }
                    }
                    enemy_card.postDelayed({
                        player_card.card?.let { playedEnemyCard ->
                            if (playedEnemyCard.useMagic) {
                                mainActivity?.playSound(MainActivity.SOUND_FIGHT_MAGIC)
                            } else if (playedEnemyCard.useWeapon) {
                                mainActivity?.playSound(MainActivity.SOUND_FIGHT_WEAPON)
                            } else {
                                mainActivity?.playSound(MainActivity.SOUND_FIGHT)
                            }
                        }
                    }, 100)

                    player_card.animate()
                            .translationX(-cardsXDiff).translationY(-cardsYDiff)
                            .withEndAction {
                                player_card.animate()
                                        .translationX(0f).translationY(0f)
                                        .withEndAction { animateNextStep() }
                            }

                    enemy_card.animate()
                            .translationX(cardsXDiff).translationY(cardsYDiff)
                            .withEndAction {
                                enemy_card.animate()
                                        .translationX(0f).translationY(0f)
                            }
                }
                BattleManager.BattleStep.APPLY_DAMAGES -> {
                    enemy_card.animateValueChange(null)
                    player_card.animateValueChange(null)
                    animateNextStep()
                }
                BattleManager.BattleStep.PLAYER_DEATH -> {
                    mainActivity?.playSound(MainActivity.SOUND_DEATH)
                    player_card.animate().alpha(0f).withEndAction { animateNextStep() }
                }
                BattleManager.BattleStep.ENEMY_DEATH -> {
                    mainActivity?.playSound(MainActivity.SOUND_DEATH)
                    enemy_card.animate().alpha(0f).withEndAction {
                        val nextEnemyCard = battleManager.nextEnemyCreatureCard
                        if (nextEnemyCard != null) {
                            enemy_card.card = nextEnemyCard
                            enemy_card.animate().alpha(1f).withEndAction { animateNextStep() }
                        } else {
                            animateNextStep()
                        }
                    }
                }
                BattleManager.BattleStep.END_TURN -> {
                    val isPlayerCreatureStillAlive = battleManager.isPlayerCreatureStillAlive
                    if (isPlayerCreatureStillAlive) {
                        val newCards = if (isPlayerCreatureStillAlive) battleManager.remainingPlayerSupportCards else battleManager.remainingPlayerCharacterCards
                        if (newCards.isEmpty()) {
                            // The battle is not finished, but the user can only fight without using support, let's automatically start that
                            battleManager.play()
                            animateNextStep()
                        } else {
                            updateUI()
                        }
                    } else {
                        updateUI()
                    }
                }
                BattleManager.BattleStep.PLAYER_WON -> {
                    battleManager.lastUsedPlayerCreatureCard?.let {
                        if (it.defense <= 0) {
                            player_card.card = battleManager.nextPlayerCreatureCard
                            player_card.animate().alpha(1f).withEndAction { displayVictory() }
                        } else {
                            displayVictory()
                        }
                    }
                }
                BattleManager.BattleStep.ENEMY_WON -> {
                    mainActivity?.playSound(MainActivity.SOUND_DEFEAT)

                    val dialog = EndBattleDialogFragment.newInstance(lvl, lvl.isCompleted, EndBattleDialogFragment.EndType.ENEMY_WON)
                    val transaction = fragmentManager.beginTransaction().addToBackStack(null)
                    dialog.show(transaction, EndBattleDialogFragment::class.java.name)
                }
                BattleManager.BattleStep.DRAW -> {
                    mainActivity?.playSound(MainActivity.SOUND_DEFEAT)

                    val dialog = EndBattleDialogFragment.newInstance(lvl, lvl.isCompleted, EndBattleDialogFragment.EndType.DRAW)
                    val transaction = fragmentManager.beginTransaction().addToBackStack(null)
                    dialog.show(transaction, EndBattleDialogFragment::class.java.name)
                }
            }
        }
    }

    private fun displayVictory() {
        level?.let { lvl ->
            if (mainActivity != null && isVisible) {
                mainActivity?.playSound(MainActivity.SOUND_VICTORY)

                val dialog = EndBattleDialogFragment.newInstance(lvl, lvl.isCompleted, EndBattleDialogFragment.EndType.PLAYER_WON)
                val transaction = fragmentManager.beginTransaction().addToBackStack(null)
                dialog.show(transaction, EndBattleDialogFragment::class.java.name)
            }
            lvl.isCompleted = true
            LEVEL.save(lvl).query()
        }
    }

    fun onDarkLayerClicked() {
        // if null, we already clicked on it once and are in the middle of the animation
        currentlyAnimatedCard?.let { animatedCard ->
            stopCardHighlighting()

            animatedCard.animate().scaleX(1f).scaleY(1f).translationX(0f).translationY(0f).withEndAction { updateBottomSheetUI() }
            currentlyAnimatedCard = null
        }
    }

    private fun highlightCard(cardView: GameCardView) {
        if (currentlyAnimatedCard != null) {
            return  // We already clicked on it once and are in the middle of the animation
        }

        selectCardBottomSheetBehavior?.let {
            it.isHideable = true
            it.state = BottomSheetBehavior.STATE_HIDDEN
        }

        currentlyAnimatedCard = cardView
        cardView.animate()
                .scaleX(1.7f)
                .scaleY(1.7f)
                .translationX((view?.width ?: 0) / 2f - cardView.width / 2f - cardView.x + select_card_bottom_sheet.scrollX)
                .translationY(-(card_list.height * 1.8f))

        dark_layer.visibility = View.VISIBLE
        dark_layer.isClickable = true
        dark_layer.animate().alpha(1f)
    }

    private fun stopCardHighlighting() {
        dark_layer.isClickable = false
        dark_layer.animate().alpha(0f).withEndAction { dark_layer.visibility = View.GONE }
    }

    private fun onStrategySelected() {
        selectStrategyBottomSheetBehavior?.let {
            it.isHideable = true
            it.state = BottomSheetBehavior.STATE_HIDDEN
        }

        if (!player_card.animateValueChange(Runnable { animateNextStep() })) {
            animateNextStep() // if no animation at all, let's do the next step already now
        }
    }

    private fun updateUI() {
        if (context == null) {
            return  // we have been detached, we shouldn't do anything
        }

        val isPlayerCreatureStillAlive = battleManager.isPlayerCreatureStillAlive
        if (isPlayerCreatureStillAlive) {
            player_card.card = battleManager.lastUsedPlayerCreatureCard
        } else {
            player_card.card = null
        }
        enemy_card.card = battleManager.currentOrNextAliveEnemyCreatureCard

        // The "no support" button view needs to be removed before card population
        if (card_list.childCount > 0 && card_list.getChildAt(card_list.childCount - 1) is Button) {
            card_list.removeViewAt(card_list.childCount - 1)
        }

        // Create or remove only the necessary number of views and reuse the existing ones
        val newCards = if (isPlayerCreatureStillAlive) battleManager.remainingPlayerSupportCards else battleManager.remainingPlayerCharacterCards
        val diff = newCards.size - card_list.childCount
        if (diff > 0) { // We need more cards
            for (i in 0..diff - 1) {
                val cardView = GameCardView(context)
                cardView.setPadding(10, 10, 10, 10)
                cardView.onClick { highlightCard(cardView) }
                card_list.addView(cardView)
            }
        } else if (diff < 0) { // We need to remove some cards
            card_list.removeViews(newCards.size, -diff)
        }

        // Set the new cards to the views
        for (i in newCards.indices) {
            val card = newCards[i]
            Dog.d("Card added to game: " + card.name)
            (card_list.getChildAt(i) as GameCardView).card = card
        }

        // Add the "no support" button if necessary
        if (isPlayerCreatureStillAlive && newCards.isNotEmpty()) {
            val playWithoutSupportBtn = Button(context)
            playWithoutSupportBtn.text = "Don't use support card"
            playWithoutSupportBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_close_white_24dp, 0, 0, 0)
            playWithoutSupportBtn.maxWidth = MAX_NO_SUPPORT_CARD_BTN_WIDTH
            playWithoutSupportBtn.onClick {
                selectCardBottomSheetBehavior?.let {
                    it.isHideable = true
                    it.state = BottomSheetBehavior.STATE_HIDDEN
                }
                battleManager.play()
                animateNextStep()
            }
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            params.setMargins(NO_SUPPORT_CARD_BTN_MARGIN, NO_SUPPORT_CARD_BTN_MARGIN, NO_SUPPORT_CARD_BTN_MARGIN, NO_SUPPORT_CARD_BTN_MARGIN)
            card_list.addView(playWithoutSupportBtn, params)
        }

        updateBottomSheetUI()
    }

    private fun updateBottomSheetUI() {
        selectCardBottomSheetBehavior?.let { cardBehavior ->
            selectStrategyBottomSheetBehavior?.let { strategyBehavior ->
                if (battleManager.currentStep == BattleManager.BattleStep.SELECT_STRATEGY) {
                    if (strategyBehavior.isHideable) {
                        strategyBehavior.isHideable = false
                        strategyBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                    }

                    if (!cardBehavior.isHideable) {
                        cardBehavior.isHideable = true
                        cardBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                } else {
                    if (!strategyBehavior.isHideable) {
                        strategyBehavior.isHideable = true
                        strategyBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                    }

                    if (card_list.childCount > 0) {
                        if (cardBehavior.isHideable) {
                            cardBehavior.isHideable = false
                            cardBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                        }
                    } else {
                        if (!cardBehavior.isHideable) {
                            cardBehavior.isHideable = true
                            cardBehavior.state = BottomSheetBehavior.STATE_HIDDEN
                        }
                    }
                }
            }
        }
    }

    companion object {

        val ARG_PLAYER_CARDS = "ARG_PLAYER_CARDS"
        val ARG_LEVEL = "ARG_LEVEL"

        private val STATE_BATTLE_MANAGER = "STATE_BATTLE_MANAGER"
        private val MAX_NO_SUPPORT_CARD_BTN_WIDTH = UiUtils.dpToPixel(210)
        private val NO_SUPPORT_CARD_BTN_MARGIN = UiUtils.dpToPixel(10)

        fun newInstance(level: Level, playerCards: List<Card>): BattleFragment {
            val fragment = BattleFragment()
            val args = Bundle()
            args.putParcelable(ARG_LEVEL, Parcels.wrap(level))
            args.putParcelable(ARG_PLAYER_CARDS, Parcels.wrap(playerCards))
            fragment.arguments = args

            return fragment
        }
    }
}
