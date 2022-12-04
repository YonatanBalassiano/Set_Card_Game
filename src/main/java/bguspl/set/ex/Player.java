package bguspl.set.ex;

import bguspl.set.Env;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import javax.management.ObjectName;
import javax.print.attribute.Size2DSyntax;
import javax.xml.validation.Validator;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    protected Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate
     * key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    /**
     * the tokens that the player has collected.
     */
    private volatile List<Integer> tokens;
    public volatile boolean lock = false;

    /**
     * the dealer object
     */
    private Dealer dealer;

    private Object lockKeyPress = new Object();
    private AtomicBoolean keyLock = new AtomicBoolean(false);


    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided
     *               manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer = dealer;
        tokens = new LinkedList<Integer>();
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
        if (!human) createArtificialIntelligence();
        
        while (!terminate) {
            while(keyLock.get() == false){
                try {
                    Thread.sleep(env.config.tableDelayMillis);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
                System.out.println(tokens.size());
                if (tokens.size() == 3){
                        int[] cards = new int[tokens.size()+1];
                        int index = 0;
                        for(int tempSlot : tokens){
                            cards[index] = tempSlot;
                            index ++;
                        }
                        cards[3] = id;
                        System.out.println(Thread.currentThread().getName());
                        boolean isSet = dealer.isSet(cards, playerThread);
                        if(isSet){
                            point();
                        }
                        else{
                            penalty();
                        }
                }
                keyLock.set(false);
            }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of
     * this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it
     * is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
            while (!terminate) {
                try {
                    synchronized (this) {
                        wait();
                    }
                } catch (InterruptedException ignored) {
                }
                int slot = (int) ((Math.random() * (12 - 0)));
                keyPressed(slot);
            }
            System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        terminate = true;
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        if (!lock) {
            if (tokens.contains(slot)) {
                if (table.removeToken(id, slot))
                    tokens.remove(tokens.indexOf(slot));
            } else {
                if (tokens.size() < 3) {
                    tokens.add(slot);
                    table.placeToken(id, slot);
                    keyLock.set(true);
                }
            }
        }
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        System.out.println(Thread.currentThread().getName() + "point");

        for (int i : tokens) {
            table.removeToken(id, i);
            table.removeCard(i);
            dealer.removeTokensToOtherPlayers(i, id);
        }
        env.ui.setScore(id, ++score);
        dealer.setFreeze(env.config.pointFreezeMillis, this);

        tokens.clear();
        dealer.ClockReset();

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        dealer.unlockIsSet();
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        System.out.println(Thread.currentThread().getName() + "penalty");
        dealer.unlockIsSet();
        dealer.setFreeze(env.config.penaltyFreezeMillis, this);
    }

    /**
     * Returns the current score of the player.
     */
    public int getScore() {
        return score;
    }

    /**
     * This method is called when the set has been done.
     */
    public void resetTokens() {
        tokens.clear();
    }

    /**
     * This method is called when a key is released.
     * 
     * @param slot
     */
    public void removeToken(int slot) {
        int index = tokens.indexOf(slot);
        if (index != -1) {
            tokens.remove(index);
        }
    }

}
