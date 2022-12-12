package bguspl.set.ex;
import bguspl.set.Env;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;



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
     * level of difficulty ENUM
     */
    enum Level{
        EASY,
        MEDIUM,
        HIGH
    }

    /**
     * Creates a new dealer object.
     *
     * @param env the game environment object
     * @param table the table object
     * @param players the players
     */
    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        isSetQueue = new LinkedList<Integer>();

        if(env.config.turnTimeoutMillis<0){
            level = Level.EASY;
        }else if(env.config.turnTimeoutMillis==0) {
            level = Level.MEDIUM;}
        else{
            level = Level.HIGH;}

    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        System.out.printf("Info: Thread %s starting.%n", Thread.currentThread().getName());
        startPlayersThreads();        
        while (!shouldFinish()) {
            ClockReset();
            Collections.shuffle(deck);
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(false);
            removeAllCardsFromTable();
            shouldFinish=false;
        }
        announceWinners();
        terminate();
        System.out.printf("Info: Thread %s terminated.%n", Thread.currentThread().getName());
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        boolean extraArgument = true;
        while (!terminate && extraArgument && !shouldFinish) {
            sleepUntilWokenOrTimeout();
            boolean warning = System.currentTimeMillis() >= reshuffleTime - env.config.turnTimeoutWarningMillis;
            updateTimerDisplay(warning);
            placeCardsOnTable();
            
            //enum switch
            switch(level)
            {
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
        System.out.printf("Info: Thread %s play.%n", Thread.currentThread().getName());
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {  
        for(Player player : players){
            player.terminate();
        }
        terminate = true;
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        toggleLockOn();
        List<Integer> tableSlots = IntStream.rangeClosed(0, env.config.tableSize-1).boxed().collect(Collectors.toList());  
        Collections.shuffle(tableSlots);
        for(int i : tableSlots){
            if(table.slotToCard[i]==null){
                if(deck.size()!=0){
                    table.placeCard(deck.get(0),i);
                    deck.remove(deck.get(0));
                }
            }
        }
        toggleLockOff();
    }

    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        try {
            Thread.sleep(env.config.tableDelayMillis);
                } catch (InterruptedException e) {
            // ignore
        }
    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        switch(level){
            case EASY:
                break;
            case MEDIUM:
                env.ui.setElapsed(System.currentTimeMillis() - reshuffleTime);
                break;
            case HIGH:
                long secondTillTimeout = reshuffleTime - System.currentTimeMillis();
                env.ui.setCountdown(secondTillTimeout, reset);
                break;
        }
    }

    /**
     * Returns all the cards from the table to the deck.
     */
    private void removeAllCardsFromTable() {
        toggleLockOn();
        for(int i = 0; i<env.config.tableSize;i++){
            if(table.slotToCard[i]!=null){
                deck.add(table.slotToCard[i]);
                table.removeCard(i);
            }
        }
        for(Player player : players){
            table.removeAllTokens(player.id);
        }
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        int maxScore = 0;
        int maxSum = 0; 
        
        for(Player player : players){
            if(player.getScore()>maxScore){
                maxScore = player.getScore();
            }
        }

        for(Player player : players){
            if(player.getScore()==maxScore){
                maxSum ++;
            }
        }

        int[] winners = new int[maxSum];
        for(Player player : players){
            if(player.getScore()==maxScore){
                winners[maxSum-1] = player.id;
                maxSum --;
            }
        }

        env.ui.announceWinner(winners);
    }

    /**
     * Start the threads of the players.
     */
    private void startPlayersThreads() {
        for (Player player : players) {
            new Thread(player,"Player"+player.id).start();
        }
    }

    /**
     * Reset the timer.
     */
    protected void ClockReset(){
        switch(level){
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
    protected boolean isSet(int[] cards){
        synchronized(isSetQueueLock){
            isSetQueue.add(cards[cards.length-1]);
        }

        //TRY TO GET THE LOCK
        synchronized(isSetQueueLock){
        while(isSetQueue.peek()!= cards[3]){
            try{isSetQueueLock.wait();}catch(Exception e){}
        }
    }
        int[] tempCards = new int[cards.length-1];
        for(int i = 0; i<cards.length-1;i++){
            tempCards[i] = table.getcardBySlot(cards[i]);
        }
        boolean isSet = env.util.testSet(tempCards);
        if(isSet){shouldFinish = shouldFinish();}
        return isSet;
    }

    /**
     * synchronized method for isSet tests (fairly)
     * @post next object in the queue can try to check for Set
     */
    protected void unlockIsSet(){
        synchronized(isSetQueueLock){
            isSetQueue.remove();
            isSetQueueLock.notifyAll();
        }
    }

    /**
     * set a freeze to a player
     * lock the player prom place and remove tokens on the table
     * @param player  the player we freeze
     * @param millis the time we freeze the player
     */
    public void setFreeze(long millis, Player player){
        player.peneltyLock.set(true);
        long freezeTimeOut = System.currentTimeMillis() + millis;
        while(System.currentTimeMillis()<=freezeTimeOut){
            env.ui.setFreeze(player.id, freezeTimeOut - System.currentTimeMillis());
            try{player.playerThread.sleep(env.config.tableDelayMillis);}
            catch(Exception e){}
        }
        env.ui.setFreeze(player.id, freezeTimeOut - System.currentTimeMillis());
        player.peneltyLock.set(false);
    }


    /**
     * lock all players from placing and removing tokens on the table
     * @post all players are locked from placing and removing tokens on the table
     */
    public void toggleLockOn(){
        for(Player player : players){
            player.tableLock.set(true);
        }
    }


    /**
     * unlock all players from placing and removing tokens on the table
     * @post all players are unlocked from placing and removing tokens on the table
     */
    public void toggleLockOff(){
        for(Player player : players){
            player.tableLock.set(false);
        }
    }



    //test purpuses only
    public long getReshuffleTime(){
        return reshuffleTime;
    }

    //test purpuses only
    public void placeCardForTest(int id, int slot){
        table.placeCard(id, slot);
    }
}

