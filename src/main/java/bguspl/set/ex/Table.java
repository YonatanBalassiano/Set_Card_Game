package bguspl.set.ex;

import bguspl.set.Env;

import java.util.LinkedList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    protected final List<List<Integer>> tokens;

    Object lockSlotsCards = new Object();



    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;

        //Initialize tokens
        int PlayerSum = env.config.players;
        tokens = new LinkedList<>();
        for (int i = 0; i < PlayerSum; i++) {
            tokens.add(new LinkedList<Integer>());
        }
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
        
        synchronized (lockSlotsCards) {
            // place card in slot and vice versa
            slotToCard[slot] = card;
            cardToSlot[card] = slot;
            env.ui.placeCard(card, slot);
        }
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
        
        synchronized (lockSlotsCards) {
            // remove card from slot and vice versa
            if(slotToCard[slot] != null){
                int card = slotToCard[slot];
                slotToCard[slot] = null;
                cardToSlot[card] = null;
                env.ui.removeCard(slot);
                

                // //Remove all tokens from slot
                // for(List<Integer> token :tokens ){
                //     if(token.contains(slot)){
                //         removeToken(tokens.indexOf(token), slot);
                //     }
                // }
            }
            
        }
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}
        synchronized(lockSlotsCards){
            env.ui.placeToken(player, slot);
            tokens.get(player).add(slot);
        }

        
    }
    

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {return false;}

        synchronized(lockSlotsCards){
            env.ui.removeToken(player, slot);
            int location = tokens.get(player).indexOf(slot);
            if (location != -1 )
                tokens.get(player).remove(location);
        }
        return true;
        
    }

    /**
     * Returns the card in a slot
     * @param slot
     * @return card in slot
     */
    public int getcardBySlot(int slot){
        synchronized (lockSlotsCards) {
            if(slotToCard[slot]==null){return -1;}
            return slotToCard[slot];
        }
    }


    /**
     * Removes all tokens of a player
     * @param player
     */
    protected void removeAllTokens(int player){
        synchronized(lockSlotsCards){}
        for(int slot : tokens.get(player)){
            env.ui.removeToken(player, slot);
        }
        tokens.get(player).clear();
    }


    /**
     * Returns the number of tokens of a player
     * @param player
     * @return Number of tokens of a player
     */
    protected int getTokenSize(int player){
        synchronized(lockSlotsCards){
        return tokens.get(player).size();
        }
    }

    /**
     * Returns a list of all tokens of a player
     * @param player
     * @return List of all tokens of a player
     */
    protected List<Integer> getTokens(int player){
        synchronized(lockSlotsCards){
        return tokens.get(player);
        }
    }
    
    /**
     * Returns true if a player has a token on a slot
     * @param player
     * @param slot
     * @return true if a player has a token on a slot
     */
    protected boolean isToken(int player, int slot){
        synchronized(lockSlotsCards){
        return tokens.get(player).contains(slot);
        }
    }
 
    /**
     * Removes all relevant tokens of all players and removes the card from the table 
     * @param player
     */
    protected void pointToPlayer(int player){
        synchronized(lockSlotsCards){
        for (int i : tokens.get(player)){
            env.ui.removeToken(player, i);
            removeCard(i);
            for(int j = 0; j<tokens.size(); j++){
                if (j!=player && tokens.get(j).contains(i)){
                    removeToken(j, i);
                }
            }
        }
        tokens.get(player).clear();
    }
    }   
}
