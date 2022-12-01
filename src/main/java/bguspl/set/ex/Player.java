package bguspl.set.ex;

import bguspl.set.Env;

import java.util.ArrayList;
import java.util.List;

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
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
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
    private List<Integer> tokens;

    /**
     * the dealer object
     */
    private Dealer dealer;
     

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;
        this.dealer = dealer;
        tokens = new ArrayList<Integer>();
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
            // TODO implement main player loop
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
            while (!terminate) {
                // TODO implement player key press simulator
                try {
                    synchronized (this) { wait(); }
                } catch (InterruptedException ignored) {}
            }
            System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        if (tokens.contains(slot)){
            table.removeToken(id, slot);
            tokens.remove(slot);
        }
        else{
            table.placeToken(id, slot);
            tokens.add(slot);
            if (tokens.size() == 3){
                int[] cards = new int[3];
                int index = 0;
                for(int tempSlot : tokens){
                    cards[index] = table.getcardBySlot(tempSlot);
                    index ++;
                }

                boolean isSet = env.util.testSet(cards);

                if(isSet){
                    point();
                }
                else{
                    penalty();
                }
                dealer.terminate();
            }
        }
        

        // TODO implement
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() {
        try{
            Thread.sleep(env.config.pointFreezeMillis);
        }
        catch(InterruptedException e){
            System.out.println("Error: " + e);
        }

        env.ui.setFreeze(id, env.config.pointFreezeMillis);


        for (int i : tokens){
            table.removeToken(id, i);
            table.removeCard(i);
        }
        tokens.clear();
        dealer.terminate();

        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);

    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() {
        try{
            Thread.sleep(env.config.penaltyFreezeMillis);
        }
        catch(InterruptedException e){
            System.out.println("Error: " + e);
        }

        env.ui.setFreeze(id, env.config.penaltyFreezeMillis);
        //change the clock countdown
    }

    public int getScore() {
        return score;
    }


}
