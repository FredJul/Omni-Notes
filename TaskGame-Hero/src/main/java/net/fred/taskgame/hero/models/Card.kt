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

package net.fred.taskgame.hero.models

import android.support.annotation.DrawableRes
import android.util.SparseBooleanArray
import net.fred.taskgame.hero.R
import net.fred.taskgame.hero.logic.BattleManager
import net.frju.androidquery.annotation.DbField
import net.frju.androidquery.annotation.DbModel
import net.frju.androidquery.gen.Q
import org.parceler.Parcel
import java.util.*

@Parcel(Parcel.Serialization.BEAN)
@DbModel(databaseProvider = LocalDatabaseProvider::class)
class Card : Cloneable {

    interface FightAction {
        fun applyDamageFromOpponent(current: Card, opponent: Card)
    }

    interface SupportAction {
        fun executeSupportAction(manager: BattleManager, fromEnemyPointOfView: Boolean)
    }

    enum class Type {
        CREATURE, SUPPORT
    }

    @DbField(primaryKey = true)
    var id = INVALID_ID

    @DbField
    var isObtained: Boolean = false

    @DbField
    var isInDeck: Boolean = false

    var type = Type.CREATURE
    var name = ""
    var desc = ""
    var neededSlots: Int = 0
    var attack: Int = 0
    var defense: Int = 0
    var useWeapon: Boolean = false
    var useMagic: Boolean = false
    @DrawableRes
    var iconResId = INVALID_ID
    var price: Int = 0

    @Transient
    var fightAction: FightAction
        @org.parceler.Transient get
        @org.parceler.Transient set

    @Transient
    var supportAction: SupportAction? = null
        @org.parceler.Transient get
        @org.parceler.Transient set

    init {
        fightAction = object : FightAction {
            override fun applyDamageFromOpponent(current: Card, opponent: Card) {
                current.defense -= opponent.attack
            }
        }
    }

    public override fun clone(): Card {
        val card = Card()
        card.id = id
        card.type = type
        card.name = name
        card.desc = desc
        card.neededSlots = neededSlots
        card.attack = attack
        card.defense = defense
        card.useWeapon = useWeapon
        card.useMagic = useMagic
        card.iconResId = iconResId
        card.price = price
        card.isObtained = isObtained
        card.isInDeck = isInDeck
        card.fightAction = fightAction
        card.supportAction = supportAction
        return card
    }

    companion object {

        val CREATURE_TROLL = 0
        val CREATURE_TROLL_2 = 1
        val CREATURE_TROLL_3 = 2
        val CREATURE_TROLL_4 = 3
        val CREATURE_SKELETON = 10
        val CREATURE_SKELETON_2 = 11
        val CREATURE_SKELETON_3 = 12
        val CREATURE_ENCHANTED_TREE = 20
        val CREATURE_ENCHANTED_TREE_2 = 21
        val CREATURE_SYLPH = 30
        val CREATURE_SYLPH_2 = 31
        val CREATURE_SYLPH_3 = 32
        val CREATURE_SNAKE = 40
        val CREATURE_SNAKE_2 = 41
        val CREATURE_ZOMBIE = 50
        val CREATURE_ZOMBIE_2 = 51
        val CREATURE_MERMAN = 60
        val CREATURE_MERMAN_2 = 61
        val CREATURE_MERMAN_3 = 62
        val CREATURE_MERMAN_4 = 63
        val CREATURE_EMPTY_ARMOR = 70
        val CREATURE_EMPTY_ARMOR_2 = 71
        val CREATURE_EMPTY_ARMOR_3 = 72
        val CREATURE_EMPTY_ARMOR_4 = 73
        val CREATURE_GRUNT = 80
        val CREATURE_GRUNT_2 = 81
        val CREATURE_GRUNT_3 = 82
        val CREATURE_GRUNT_4 = 83
        val CREATURE_LICH = 90
        val CREATURE_LICH_2 = 91
        val CREATURE_LICH_3 = 92
        val CREATURE_LICH_4 = 93
        val CREATURE_SPECTRE = 100
        val CREATURE_SPECTRE_2 = 101
        val CREATURE_SPECTRE_3 = 102
        val CREATURE_SPECTRE_4 = 103
        val CREATURE_SPECTRE_5 = 104

        val SUPPORT_POWER_POTION = 10000
        val SUPPORT_ADD_WEAPON = 10001
        val SUPPORT_WEAPON_EROSION = 10002
        val SUPPORT_FREEDOM = 10003
        val SUPPORT_CONFUSION = 10004
        val SUPPORT_SURPRISE = 10005
        val SUPPORT_MEDICAL_ATTENTION = 10006
        val SUPPORT_SWITCH_POTION = 10007

        val INVALID_ID = 0

        val allCardsMap = LinkedHashMap<Int, Card>()

        val obtainedCardList: MutableList<Card>
            get() {
                return Card.allCardsMap.values.filter(Card::isObtained).toMutableList()
            }

        val nonObtainedCardList: MutableList<Card>
            get() = getNonObtainedCardList(Level.correspondingDeckSlots)

        fun getNonObtainedCardList(totalDeckSlots: Int): MutableList<Card> {
            val nonObtainedList = Card.allCardsMap.values.filterTo(ArrayList<Card>()) {
                // Do not display all card immediately
                !it.isObtained && it.neededSlots <= totalDeckSlots / 2
            }
            return nonObtainedList
        }

        val deckCardList: List<Card>
            get() {
                val deckCardList = Card.allCardsMap.values.filterTo(ArrayList<Card>(), Card::isInDeck)
                return deckCardList
            }

        fun populate() {
            allCardsMap.clear()

            val obtainedList = SparseBooleanArray()
            val inDeckList = SparseBooleanArray()
            for (card in Q.Card.select().query().toArray()) {
                obtainedList.append(card.id, card.isObtained)
                inDeckList.append(card.id, card.isInDeck)
            }


            /** */
            /****** CREATURE CARDS  */
            /** */

            var card = generateDefaultCreatureCard(CREATURE_MERMAN, 1, obtainedList, inDeckList)
            card.price = 0 // First one is free
            card.name = "Merman"
            card.attack = 1
            card.defense = 1
            card.iconResId = R.drawable.merman
            card.desc = "Mermans are famous for their courage, even if it's not always enough to save their lives\n ● Resistant to magic: -1 received damage"
            card.fightAction = object : FightAction {
                override fun applyDamageFromOpponent(current: Card, opponent: Card) {
                    if (opponent.useMagic) {
                        current.defense -= if (opponent.attack >= 1) opponent.attack - 1 else opponent.attack
                    } else {
                        current.defense -= opponent.attack
                    }
                }
            }
            checkCreatureCard(card)

            card = generateDefaultCreatureCard(CREATURE_SYLPH, 1, obtainedList, inDeckList)
            card.name = "Sylph"
            card.attack = 1
            card.defense = 2
            card.useMagic = true
            card.iconResId = R.drawable.sylph
            card.desc = "Looks kind and peaceful, but her basic wind magic can surprise you\n ● Weak against weapons: +1 received damage"
            card.fightAction = object : FightAction {
                override fun applyDamageFromOpponent(current: Card, opponent: Card) {
                    if (opponent.useWeapon) {
                        current.defense -= opponent.attack + 1
                    } else {
                        current.defense -= opponent.attack
                    }
                }
            }
            checkCreatureCard(card)

            card = generateDefaultCreatureCard(CREATURE_TROLL, 2, obtainedList, inDeckList)
            card.isObtained = true // the only card you get for free at the beginning
            card.isInDeck = inDeckList.size() == 0 || inDeckList.get(card.id) // by default, add it
            card.name = "Baby Troll"
            card.attack = 2
            card.defense = 4
            card.iconResId = R.drawable.troll
            card.desc = "Troll babies always try to eat everything and doesn't really care if it's human or not\n ● Weak against magic: +1 received damage"
            card.fightAction = object : FightAction {
                override fun applyDamageFromOpponent(current: Card, opponent: Card) {
                    if (opponent.useMagic) {
                        current.defense -= opponent.attack + 1
                    } else {
                        current.defense -= opponent.attack
                    }
                }
            }
            checkCreatureCard(card)

            card = generateDefaultCreatureCard(CREATURE_SKELETON, 2, obtainedList, inDeckList)
            card.name = "Skeleton Archer"
            card.attack = 1
            card.defense = 6
            card.useWeapon = true
            card.iconResId = R.drawable.skeleton
            card.desc = "Deads are not totally dead, and they strangely know how to send arrows in your face\n ● Resistant to magic: -1 received damage"
            card.fightAction = object : FightAction {
                override fun applyDamageFromOpponent(current: Card, opponent: Card) {
                    if (opponent.useMagic) {
                        current.defense -= if (opponent.attack >= 1) opponent.attack - 1 else opponent.attack
                    } else {
                        current.defense -= opponent.attack
                    }
                }
            }
            checkCreatureCard(card)

            card = generateDefaultCreatureCard(CREATURE_SYLPH_2, 3, obtainedList, inDeckList)
            card.name = "Charming Sylph"
            card.attack = 2
            card.defense = 7
            card.useMagic = true
            card.iconResId = R.drawable.sylph_2
            card.desc = "Will you dare hit a beautiful lady?\n ● Weak against weapons: +1 received damage"
            card.fightAction = object : FightAction {
                override fun applyDamageFromOpponent(current: Card, opponent: Card) {
                    if (opponent.useWeapon) {
                        current.defense -= opponent.attack + 1
                    } else {
                        current.defense -= opponent.attack
                    }
                }
            }
            checkCreatureCard(card)

            card = generateDefaultCreatureCard(CREATURE_GRUNT, 3, obtainedList, inDeckList)
            card.name = "Grunt"
            card.attack = 3
            card.defense = 5
            card.iconResId = R.drawable.grunt
            card.desc = "Half human, half beast. Killing someone is a natural law for them and they don't perceive that as a problem."
            checkCreatureCard(card)

            card = generateDefaultCreatureCard(CREATURE_ENCHANTED_TREE, 3, obtainedList, inDeckList)
            card.name = "Enchanted Tree"
            card.attack = 3
            card.defense = 6
            card.iconResId = R.drawable.enchanted_tree
            card.desc = "Nature is beautiful, except maybe when it tries to kill you\n ● Weak against weapons: +2 received damage"
            card.fightAction = object : FightAction {
                override fun applyDamageFromOpponent(current: Card, opponent: Card) {
                    if (opponent.useWeapon) {
                        current.defense -= opponent.attack + 2
                    } else {
                        current.defense -= opponent.attack
                    }
                }
            }
            checkCreatureCard(card)

            card = generateDefaultCreatureCard(CREATURE_LICH, 4, obtainedList, inDeckList)
            card.name = "Lich"
            card.attack = 5
            card.defense = 6
            card.useMagic = true
            card.iconResId = R.drawable.lich
            card.desc = "Ancient mage who found a way to not be affected by the time anymore"
            checkCreatureCard(card)

            card = generateDefaultCreatureCard(CREATURE_EMPTY_ARMOR, 4, obtainedList, inDeckList)
            card.name = "Empty armor"
            card.attack = 3
            card.defense = 11
            card.useWeapon = true
            card.iconResId = R.drawable.empty_armor
            card.desc = "Looks empty and harmless, but don't turn your back on it or you may regret it"
            checkCreatureCard(card)

            card = generateDefaultCreatureCard(CREATURE_SPECTRE, 5, obtainedList, inDeckList)
            card.name = "Spectre"
            card.attack = 6
            card.price = card.neededSlots * 50 + 100
            card.defense = 10
            card.useMagic = true
            card.iconResId = R.drawable.spectre
            card.desc = "It's never good when nightmare creatures are becoming reality and attack you"
            checkCreatureCard(card)

            card = generateDefaultCreatureCard(CREATURE_ZOMBIE, 6, obtainedList, inDeckList)
            card.name = "Zombie"
            card.attack = 4
            card.defense = 14
            card.iconResId = R.drawable.zombie
            card.desc = "Why dead people cannot live like everyone else?"
            checkCreatureCard(card)

            card = generateDefaultCreatureCard(CREATURE_SNAKE, 6, obtainedList, inDeckList)
            card.name = "M. Python"
            card.attack = 7
            card.defense = 9
            card.useWeapon = true
            card.iconResId = R.drawable.snake
            card.desc = "They are fast and, like Brian, always look at the bright side of life\n ● Weak against magic: +3 received damage"
            card.fightAction = object : FightAction {
                override fun applyDamageFromOpponent(current: Card, opponent: Card) {
                    if (opponent.useMagic) {
                        current.defense -= opponent.attack + 3
                    } else {
                        current.defense -= opponent.attack
                    }
                }
            }
            checkCreatureCard(card)

            card = generateDefaultCreatureCard(CREATURE_TROLL_2, 7, obtainedList, inDeckList)
            card.name = "Slinger Troll"
            card.attack = 4
            card.defense = 17
            card.useWeapon = true
            card.iconResId = R.drawable.troll_2
            card.desc = "Always play 'Rock' in rock-paper-scissor game\n ● Resistant to weapon: -2 received damage\n ● Weak against magic: +3 received damage"
            card.fightAction = object : FightAction {
                override fun applyDamageFromOpponent(current: Card, opponent: Card) {
                    if (opponent.useMagic) {
                        current.defense -= opponent.attack + 3
                    } else if (opponent.useWeapon) {
                        current.defense -= if (opponent.attack - 2 > 0) opponent.attack - 2 else 0
                    } else {
                        current.defense -= opponent.attack
                    }
                }
            }
            checkCreatureCard(card)

            card = generateDefaultCreatureCard(CREATURE_GRUNT_2, 7, obtainedList, inDeckList)
            card.name = "Crossbowman Grunt"
            card.attack = 9
            card.defense = 11
            card.useWeapon = true
            card.iconResId = R.drawable.grunt_2
            card.desc = "Born with less muscles than others, he compensates with a good manipulation of a crossbow"
            checkCreatureCard(card)


            /** */
            /****** SUPPORT CARDS  */
            /** */

            card = generateDefaultSupportCard(SUPPORT_MEDICAL_ATTENTION, 2, obtainedList, inDeckList)
            card.name = "Medical Attention"
            card.iconResId = R.drawable.medical_attention
            card.desc = "Ok it's a summoned creature, but does that means you should be heartless?\n ● +4 defense if wounded"
            card.supportAction = object : SupportAction {
                override fun executeSupportAction(manager: BattleManager, fromEnemyPointOfView: Boolean) {
                    val player = manager.getLastUsedPlayerCreatureCard(fromEnemyPointOfView)
                    //TODO: does not take in account the previous defense increase
                    val defenseDiff = allCardsMap[player!!.id]!!.defense - player.defense
                    if (defenseDiff > 0) {
                        player.defense += Math.min(4, defenseDiff)
                    }
                }
            }

            card = generateDefaultSupportCard(SUPPORT_ADD_WEAPON, 3, obtainedList, inDeckList)
            card.name = "Battle axe"
            card.iconResId = R.drawable.axe
            card.desc = "The best way to gain respect from your enemy is by putting an axe in his face\n ● +6 attack if he doesn't use weapon nor magic"
            card.supportAction = object : SupportAction {
                override fun executeSupportAction(manager: BattleManager, fromEnemyPointOfView: Boolean) {
                    val player = manager.getLastUsedPlayerCreatureCard(fromEnemyPointOfView)
                    if (!player!!.useWeapon && !player.useMagic) {
                        player.attack += 6
                    }
                }
            }

            card = generateDefaultSupportCard(SUPPORT_WEAPON_EROSION, 3, obtainedList, inDeckList)
            card.name = "Weapon erosion"
            card.iconResId = R.drawable.erode_weapon
            card.desc = "Your enemy weapon starts to run into pieces. Serves him damned right!\n ● -5 attack if he uses a weapon"
            card.supportAction = object : SupportAction {
                override fun executeSupportAction(manager: BattleManager, fromEnemyPointOfView: Boolean) {
                    val enemy = manager.getLastUsedEnemyCreatureCard(fromEnemyPointOfView)
                    if (enemy!!.useWeapon) {
                        enemy.attack -= 5
                        if (enemy.attack < 0) {
                            enemy.attack = 0
                        }
                    }
                }
            }

            card = generateDefaultSupportCard(SUPPORT_POWER_POTION, 4, obtainedList, inDeckList)
            card.name = "(Fake) Potion of invincibility"
            card.iconResId = R.drawable.red_potion
            card.desc = "It's only syrup, but placebo effect makes your creature feel invincible\n ● Multiply attack by 2\n ● Divide defense by 1.3"
            card.supportAction = object : SupportAction {
                override fun executeSupportAction(manager: BattleManager, fromEnemyPointOfView: Boolean) {
                    val player = manager.getLastUsedPlayerCreatureCard(fromEnemyPointOfView)
                    player!!.attack *= 2
                    player!!.defense = (player.defense / 1.3).toInt()
                    if (player.defense <= 0) {
                        player.defense = 1
                    }
                }
            }

            card = generateDefaultSupportCard(SUPPORT_SURPRISE, 4, obtainedList, inDeckList)
            card.name = "Surprise!"
            card.iconResId = R.drawable.surprise
            card.desc = "Forget honor and attack the enemy from behind, it's more effective\n ● If you kill the enemy this turn, you'll not receive any damage"
            card.supportAction = object : SupportAction {
                override fun executeSupportAction(manager: BattleManager, fromEnemyPointOfView: Boolean) {
                    val player = manager.getLastUsedPlayerCreatureCard(fromEnemyPointOfView)
                    val enemy = manager.getLastUsedEnemyCreatureCard(fromEnemyPointOfView)
                    if (enemy!!.defense <= player!!.attack) {
                        player.defense += enemy.attack
                    }
                }
            }

            card = generateDefaultSupportCard(SUPPORT_CONFUSION, 5, obtainedList, inDeckList)
            card.name = "Confusion"
            card.iconResId = R.drawable.confusion
            card.desc = "Confuse your enemy with a sneaky but efficient lie\n ● Enemy is confused and skips one turn"
            card.supportAction = object : SupportAction {
                override fun executeSupportAction(manager: BattleManager, fromEnemyPointOfView: Boolean) {
                    manager.stunEnemy(fromEnemyPointOfView)
                }
            }

            card = generateDefaultSupportCard(SUPPORT_FREEDOM, 6, obtainedList, inDeckList)
            card.name = "Freedom"
            card.iconResId = R.drawable.unshackled
            card.desc = "Free your creature from your control. It will charge the enemy with all his forces, and profit of the breach to run away.\n ● Multiply attack by 3\n ● Your creature run away after 1 round"
            card.supportAction = object : SupportAction {
                override fun executeSupportAction(manager: BattleManager, fromEnemyPointOfView: Boolean) {
                    val player = manager.getLastUsedPlayerCreatureCard(fromEnemyPointOfView)
                    player!!.defense = 0 //TODO: not true, but does the job for now
                    player.attack *= 3
                }
            }

            card = generateDefaultSupportCard(SUPPORT_SWITCH_POTION, 7, obtainedList, inDeckList)
            card.name = "Switch potion"
            card.iconResId = R.drawable.purple_potion
            card.desc = "Your creature switch it's attack/defense level with his opponent's ones. How does the potion work? Well, it's a secret."
            card.supportAction = object : SupportAction {
                override fun executeSupportAction(manager: BattleManager, fromEnemyPointOfView: Boolean) {
                    val player = manager.getLastUsedPlayerCreatureCard(fromEnemyPointOfView)
                    val enemy = manager.getLastUsedEnemyCreatureCard(fromEnemyPointOfView)

                    val playerAttack = player!!.attack
                    val playerDefense = player.defense
                    player.attack = enemy!!.attack
                    player.defense = enemy.defense
                    enemy.attack = playerAttack
                    enemy.defense = playerDefense
                }
            }
        }

        private fun generateDefaultCreatureCard(id: Int, neededSlots: Int, obtainedList: SparseBooleanArray, inDeckList: SparseBooleanArray): Card {
            val card = Card()
            card.id = id
            card.isObtained = obtainedList.get(card.id)
            card.isInDeck = inDeckList.get(card.id)
            card.neededSlots = neededSlots
            card.price = card.neededSlots * 50

            allCardsMap.put(card.id, card)

            return card
        }

        private fun generateDefaultSupportCard(id: Int, neededSlots: Int, obtainedList: SparseBooleanArray, inDeckList: SparseBooleanArray): Card {
            val card = Card()
            card.id = id
            card.type = Type.SUPPORT
            card.isObtained = obtainedList.get(card.id)
            card.isInDeck = inDeckList.get(card.id)
            card.neededSlots = neededSlots
            card.price = card.neededSlots * 50

            allCardsMap.put(card.id, card)

            return card
        }

        private fun checkCreatureCard(card: Card) {
            // rules are:
            // - points to split are equals to 3*slots +- 30%
            // - defense need to be greater than 1.2*attack
            // - attack is more important than defense, so big attackers should be penalised

            val acceptableSum = card.neededSlots * 3
            val margin = Math.round(acceptableSum / 100f * 30f)
            if (card.attack > Math.round(card.defense / 1.2) || card.attack + card.defense > acceptableSum + margin || card.attack + card.defense < acceptableSum - margin) {
                throw IllegalStateException("Card " + card.name + " does not respect rules")
            }
        }
    }
}
