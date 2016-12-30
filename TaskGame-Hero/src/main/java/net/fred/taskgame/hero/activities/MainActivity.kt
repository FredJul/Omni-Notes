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

package net.fred.taskgame.hero.activities

import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.net.Uri
import android.os.Bundle
import android.support.annotation.RawRes
import android.support.v7.app.AppCompatActivity
import android.text.TextUtils
import android.view.View
import net.fred.taskgame.hero.R
import net.fred.taskgame.hero.fragments.*
import net.fred.taskgame.hero.models.Card
import net.fred.taskgame.hero.models.Level
import net.fred.taskgame.hero.utils.TaskGameUtils
import net.fred.taskgame.hero.utils.UiUtils

class MainActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private var currentSoundStreamId: Int = 0

    @RawRes
    private var currentMusicResId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            val fragmentTransaction = supportFragmentManager.beginTransaction()
            fragmentTransaction.add(R.id.fragment_container, LevelSelectionFragment(), LevelSelectionFragment::class.java.name).commit()
        }

        val attributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        soundPool = SoundPool.Builder().setAudioAttributes(attributes).setMaxStreams(2).build()
        SOUND_ENTER_BATTLE = soundPool!!.load(this, R.raw.enter_battle, 1)
        SOUND_FIGHT = soundPool!!.load(this, R.raw.fight, 1)
        SOUND_FIGHT_WEAPON = soundPool!!.load(this, R.raw.fight_weapon, 1)
        SOUND_FIGHT_MAGIC = soundPool!!.load(this, R.raw.fight_magic, 1)
        SOUND_USE_SUPPORT = soundPool!!.load(this, R.raw.use_support, 1)
        SOUND_DEATH = soundPool!!.load(this, R.raw.death, 1)
        SOUND_VICTORY = soundPool!!.load(this, R.raw.victory, 1)
        SOUND_DEFEAT = soundPool!!.load(this, R.raw.defeat, 1)
        SOUND_CHANGE_CARD = soundPool!!.load(this, R.raw.change_card, 1)
        SOUND_NEW_CARD = soundPool!!.load(this, R.raw.new_card, 1)
        SOUND_IMPOSSIBLE_ACTION = soundPool!!.load(this, R.raw.impossible_action, 1)

        if (!TaskGameUtils.isAppInstalled(this)) {
            AlertDialog.Builder(this)
                    .setTitle("TaskGame application needed")
                    .setMessage("This game is totally free and use TaskGame points to progress. Without this application installed, you cannot play to it. Do you want to install it now?")
                    .setPositiveButton(android.R.string.yes) { dialog, id ->
                        val appPackageName = "net.fred.taskgame"
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)))
                        } catch (anfe: ActivityNotFoundException) {
                            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)))
                        }
                    }.setNegativeButton(android.R.string.no) { dialog, id -> dialog.cancel() }.setOnCancelListener { finish() }.show()
        }
    }

    override fun onStop() {
        super.onStop()

        stopMusic()
        stopSound()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    fun startBattle(level: Level) {
        val deckCards = Card.deckCardList

        val hasCreatureCardInDeck = deckCards.any { it.type == Card.Type.CREATURE }
        if (!hasCreatureCardInDeck) {
            AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert)
                    .setTitle("Invalid deck")
                    .setMessage("Please select at least one creature card in your deck")
                    .setPositiveButton(android.R.string.ok) { dialog, i -> dialog.dismiss() }
                    .show()
            return
        }

        val transaction = supportFragmentManager.beginTransaction()
        UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN)
        if (!TextUtils.isEmpty(level.getStartStory(this))) {
            transaction.replace(R.id.fragment_container, StoryFragment.newInstance(level, false), StoryFragment::class.java.name).addToBackStack(null).commitAllowingStateLoss()
        } else {
            playSound(SOUND_ENTER_BATTLE)
            transaction.replace(R.id.fragment_container, BattleFragment.newInstance(level, deckCards), BattleFragment::class.java.name).addToBackStack(null).commitAllowingStateLoss()
        }
    }

    fun buyCards() {
        val transaction = supportFragmentManager.beginTransaction()
        UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN)
        transaction.replace(R.id.fragment_container, BuyCardsFragment(), BuyCardsFragment::class.java.name).addToBackStack(null).commitAllowingStateLoss()
    }

    fun composeDeck() {
        val transaction = supportFragmentManager.beginTransaction()
        UiUtils.animateTransition(transaction, UiUtils.TransitionType.TRANSITION_FADE_IN)
        transaction.replace(R.id.fragment_container, ComposeDeckFragment(), ComposeDeckFragment::class.java.name).addToBackStack(null).commitAllowingStateLoss()
    }

    fun stopMusic() {
        if (mediaPlayer != null) {
            mediaPlayer!!.release()
            mediaPlayer = null
        }
        currentMusicResId = 0
    }

    fun playMusic(@RawRes soundResId: Int) {
        if (soundResId == 0 || mediaPlayer != null && mediaPlayer!!.isPlaying && currentMusicResId == soundResId) {
            return
        }

        currentMusicResId = soundResId
        stopMusic()

        mediaPlayer = MediaPlayer.create(this, soundResId)
        mediaPlayer!!.setOnCompletionListener {
            // setLooping(true) is buggy on my Nexus5X, does not really understand why... hence this workaround
            playMusic(soundResId)
        }

        mediaPlayer!!.start()
    }

    fun playSound(soundId: Int) {
        currentSoundStreamId = soundPool!!.play(soundId, 0.5f, 0.5f, 1, 0, 1f)
    }

    fun stopSound() {
        if (soundPool != null && currentSoundStreamId != 0) {
            soundPool!!.stop(currentSoundStreamId)
        }
    }

    companion object {

        var SOUND_ENTER_BATTLE: Int = 0
        var SOUND_FIGHT: Int = 0
        var SOUND_FIGHT_WEAPON: Int = 0
        var SOUND_FIGHT_MAGIC: Int = 0
        var SOUND_USE_SUPPORT: Int = 0
        var SOUND_DEATH: Int = 0
        var SOUND_VICTORY: Int = 0
        var SOUND_DEFEAT: Int = 0
        var SOUND_CHANGE_CARD: Int = 0
        var SOUND_NEW_CARD: Int = 0
        var SOUND_IMPOSSIBLE_ACTION: Int = 0
    }
}
