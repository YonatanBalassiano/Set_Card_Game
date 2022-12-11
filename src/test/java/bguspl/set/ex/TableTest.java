package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TableTest {

    Table table;
    private Integer[] slotToCard;
    private Integer[] cardToSlot;
    private List<List<Integer>> tokens;

    @BeforeEach
    void setUp() {

        Properties properties = new Properties();
        properties.put("Rows", "2");
        properties.put("Columns", "2");
        properties.put("FeatureSize", "3");
        properties.put("FeatureCount", "4");
        properties.put("TableDelaySeconds", "0");
        properties.put("PlayerKeys1", "81,87,69,82");
        properties.put("PlayerKeys2", "85,73,79,80");
        MockLogger logger = new MockLogger();
        Config config = new Config(logger, properties);
        slotToCard = new Integer[config.tableSize];
        cardToSlot = new Integer[config.deckSize];

        tokens = new LinkedList<>();


        Env env = new Env(logger, config, new MockUserInterface(), new MockUtil());
        table = new Table(env, slotToCard, cardToSlot, tokens);
    }

    private int fillSomeSlots() {
        slotToCard[1] = 3;
        slotToCard[2] = 5;
        cardToSlot[3] = 1;
        cardToSlot[5] = 2;

        return 2;
    }

    private void fillAllSlots() {
        for (int i = 0; i < slotToCard.length; ++i) {
            slotToCard[i] = i;
            cardToSlot[i] = i;
        }
    }

    private void fillSomeTokens(){
        for (int i = 0; i < 3; ++i) {
            tokens.add(new LinkedList<Integer>());
        }
        table.placeToken(0,1);
        table.placeToken(0,2);
        table.placeToken(1,3);
        table.placeToken(1,4);
        table.placeToken(2,5);
        table.placeToken(2,6);

    }

    private void placeSomeCardsAndAssert() {
        table.placeCard(8, 2);

        assertEquals(8, (int) slotToCard[2]);
        assertEquals(2, (int) cardToSlot[8]);
    }

    @Test
    void countCards_NoSlotsAreFilled() {

        assertEquals(0, table.countCards());
    }

    @Test
    void countCards_SomeSlotsAreFilled() {

        int slotsFilled = fillSomeSlots();
        assertEquals(slotsFilled, table.countCards());
    }

    @Test
    void countCards_AllSlotsAreFilled() {

        fillAllSlots();
        assertEquals(slotToCard.length, table.countCards());
    }

    @Test
    void placeCard_SomeSlotsAreFilled() {

        fillSomeSlots();
        placeSomeCardsAndAssert();
    }

    @Test
    void placeCard_AllSlotsAreFilled() {
        fillAllSlots();
        placeSomeCardsAndAssert();
    }

    @Test
    void removeAllTokens_SomeTokensArePlaced() {
            
            fillSomeTokens();
            table.removeAllTokens(0);
    
            assertEquals(0,tokens.get(0).size() );

    }

    @Test
    void placeToken_SomeTokensArePlaced() {
            
            fillSomeTokens();
            table.placeToken(2, 8);
    
            assertEquals(true,tokens.get(2).contains(8) );
    }

    static class MockUserInterface implements UserInterface {
        @Override
        public void placeCard(int card, int slot) {}
        @Override
        public void removeCard(int slot) {}
        @Override
        public void setCountdown(long millies, boolean warn) {}
        @Override
        public void setElapsed(long millies) {}
        @Override
        public void setScore(int player, int score) {}
        @Override
        public void setFreeze(int player, long millies) {}
        @Override
        public void placeToken(int player, int slot) {}
        @Override
        public void removeTokens() {}
        @Override
        public void removeTokens(int slot) {}
        @Override
        public void removeToken(int player, int slot) {}
        @Override
        public void announceWinner(int[] players) {}
    };

    static class MockUtil implements Util {
        @Override
        public int[] cardToFeatures(int card) {
            return new int[0];
        }

        @Override
        public int[][] cardsToFeatures(int[] cards) {
            return new int[0][];
        }

        @Override
        public boolean testSet(int[] cards) {
            return false;
        }

        @Override
        public List<int[]> findSets(List<Integer> deck, int count) {
            return null;
        }
    }

    static class MockLogger extends Logger {
        protected MockLogger() {
            super("", null);
        }
    }
}
