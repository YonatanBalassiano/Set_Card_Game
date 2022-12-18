package bguspl.set.ex;

import bguspl.set.Env;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.lang.model.util.SimpleElementVisitor14;
import javax.xml.transform.Templates;

/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;

    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    /**
     * check if there are no more sets available
     */
    boolean shouldFinish = false;

    /**
     * queue of isSet calls
     */
    private Queue<Integer> isSetQueue;

    /**
     * level of difficulty
     */
    private Level level;

    /**
     * isSet queue lock
     */
    private Object isSetQueueLock = new Object();

    /**
     * represents the number of milliseconds in a second
     */
    private final long SECOND = 1000;

    /**
     * represent the number of milliseconds in a 0.1 second
     */
    private final long TEN_MILL = 10;

        /**
     * represent the number of milliseconds in a 0.1 second
     */
    private final int ZERO = 0;

    /**
     * level of difficulty ENUM
     */
    enum Level {
        EASY,
        MEDIUM,
        HIGH
    }

    /**
     * Creates a new dealer object.
     *
     * @param env     the game environment object
     * @param table   the table object
     * @param players the players
     */
    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        isSetQueue = new LinkedList<Integer>();

        if (env.config.turnTimeoutMillis < 0) {
            level = Level.EASY;
        } else if (env.config.turnTimeoutMillis == 0) {
            level = Level.MEDIUM;
        } else {
            level = Level.HIGH;
        }

    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
        startPlayersThreads();
        while (!shouldFinish()) {
            Collections.shuffle(deck);
            placeCardsOnTable();
            ClockReset();
            timerLoop();
            updateTimerDisplay();
            removeAllCardsFromTable();
            shouldFinish = false;
        }
        announceWinners();
        terminate();
        System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did
     * not time out.
     */
    private void timerLoop() {
        boolean extraArgument = true;
        while (!terminate && extraArgument && !shouldFinish) {
            placeCardsOnTable();
            boolean warning = System.currentTimeMillis() >= reshuffleTime - env.config.turnTimeoutWarningMillis;
            updateTimerDisplay();
            sleepUntilWokenOrTimeout(warning);

            // enum switch
            switch (level) {
                case EASY:
                    extraArgument = table.isSetOnTable();
                    break;
                case MEDIUM:
                    extraArgument = table.isSetOnTable();
                    break;
                case HIGH:
                    extraArgument = System.currentTimeMillis() < reshuffleTime;
                    break;
            }
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        for (int i = players.length - 1; i >= ZERO; i--) {
            players[i].terminate();
            players[i].keyLockOff();
            try {
                players[i].playerThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == ZERO;
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        toggleLockOn();
        List<Integer> tableSlots = IntStream.rangeClosed(ZERO, env.config.tableSize - 1).boxed()
                .collect(Collectors.toList());
        Collections.shuffle(tableSlots);
        for (int i : tableSlots) {
            if (table.slotToCard[i] == null) {
                if (deck.size() != ZERO) {
                    table.placeCard(deck.get(ZERO), i);
                    deck.remove(ZERO);
                }
            }
        }
        toggleLockOff();
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some
     * purpose.
     */
    private void sleepUntilWokenOrTimeout(boolean warning) {
        long sleepTime = warning ? TEN_MILL : SECOND/TEN_MILL;
        try {
            synchronized(this){wait(sleepTime);}
        } catch (InterruptedException ignore) {}
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay() {
        switch (level) {
            case EASY:
                break;
            case MEDIUM:
                env.ui.setElapsed(System.currentTimeMillis() - reshuffleTime);
                break;
            case HIGH:
                long millisTillTimeout = Math.max(reshuffleTime - System.currentTimeMillis(), 0);
                
                boolean warning = System.currentTimeMillis() >= reshuffleTime - env.config.turnTimeoutWarningMillis;

                env.ui.setCountdown(millisTillTimeout, warning);
                break;
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        toggleLockOn();
        for (int i = 0; i < env.config.tableSize; i++) {
            synchronized(table.lockSlotsCards){
            if (table.slotToCard[i] != null) {
                deck.add(table.slotToCard[i]);
                table.removeCard(i);
            }
        }
    }
        for (Player player : players) {
            table.removeAllTokens(player.id);
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        int maxScore = ZERO;
        int maxSum = ZERO;
        int sumScore = ZERO;

        for (Player player : players) {
            sumScore += player.score();
            if (player.score() > maxScore) {
                maxScore = player.score();
            }
        }

        for (Player player : players) {
            if (player.score() == maxScore) {
                maxSum++;
            }
        }

        int[] winners = new int[maxSum];
        for (Player player : players) {
            if (player.score() == maxScore) {
                winners[maxSum - 1] = player.id;
                maxSum--;
            }
        }


        System.out.println(sumScore);
        env.ui.announceWinner(winners);
    }

    /**
     * Start the threads of the players.
     */
    private void startPlayersThreads() {
        for (Player player : players) {
            new Thread(player, "Player" + player.id).start();
        }
    }

    /**
     * Reset the timer.
     */
    protected void ClockReset() {
        switch (level) {
            case EASY:
                break;
            case MEDIUM:
                reshuffleTime = System.currentTimeMillis();
                break;
            case HIGH:
                reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis;
                break;
        }
    }

    /**
     * Check if the given cards form a set.
     *
     * @param cards the cards to check.
     * @return true iff the cards form a set.
     */
    synchronized protected boolean isSet(int player) {
        
        int[] tempCards = new int[env.config.featureSize];

        boolean isSet = true;

        if(table.getTokenSize(player)!=env.config.featureSize) return false;

        for (int i = 0; i < env.config.featureSize; i++) {
            tempCards[i] = table.getcardBySlot(table.getTokens(player).get(i));}
        
        isSet = table.isAllCardsOnTable(tempCards);


        if(isSet){isSet= env.util.testSet(tempCards);}
        if (isSet) {
            shouldFinish = shouldFinish();
            table.pointToPlayer(player);
        }

        return isSet;
    }

    /**
     * synchronized method for isSet tests (fairly)
     * 
     * @post next object in the queue can try to check for Set
     */
    protected void unlockIsSet() {
        synchronized (isSetQueueLock) {
            // isSetQueue.remove();
            isSetQueueLock.notifyAll();
        }
    }

    /**
     * set a freeze to a player
     * lock the player prom place and remove tokens on the table
     * 
     * @param player the player we freeze
     * @param millis the time we freeze the player
     */
    public void setFreeze(long millis, Player player) {
        // player.peneltyLock.set(true);
        synchronized(this){notifyAll();}
        long freezeTimeOut = System.currentTimeMillis() + millis;

        while (System.currentTimeMillis() <= freezeTimeOut) {
            env.ui.setFreeze(player.id, freezeTimeOut - System.currentTimeMillis());

            long sleepTime = System.currentTimeMillis() < freezeTimeOut
                    ? Math.min((freezeTimeOut-System.currentTimeMillis() % SECOND),(reshuffleTime - System.currentTimeMillis()) % SECOND)
                    : 0;

            updateTimerDisplay();
            
            try {
                player.playerThread.sleep(sleepTime);
            } catch (Exception e) {
            }
        }

        env.ui.setFreeze(player.id, ZERO);
        // player.peneltyLock.set(false);
    }

    /**
     * lock all players from placing and removing tokens on the table
     * 
     * @post all players are locked from placing and removing tokens on the table
     */
    public void toggleLockOn() {
        for (Player player : players) {
            player.tableLock.set(true);
        }
    }

    /**
     * unlock all players from placing and removing tokens on the table
     * 
     * @post all players are unlocked from placing and removing tokens on the table
     */
    public void toggleLockOff() {
        for (Player player : players) {
            player.tableLock.set(false);
        }
    }

    // test purpuses only
    public long getReshuffleTime() {
        return reshuffleTime;
    }

    // test purpuses only
    public void placeCardForTest(int id, int slot) {
        table.placeCard(id, slot);
    }
}
