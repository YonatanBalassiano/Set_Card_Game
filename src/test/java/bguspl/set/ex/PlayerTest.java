package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerTest {

    Player player;
    @Mock
    Util util;
    @Mock
    private UserInterface ui;
    @Mock
    private Table table;
    @Mock
    private Dealer dealer;
    @Mock
    private Logger logger;

    void assertInvariants() {
        assertTrue(player.id >= 0);
        assertTrue(player.score() >= 0);
    }

    @BeforeEach
    void setUp() {
        // purposely do not find the configuration files (use defaults here).
        Env env = new Env(logger, new Config(logger, (String) null), ui, util);
        table = new Table(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
        player = new Player(env, dealer, table, 0, false);
        assertInvariants();
    }

    private int fillSomeSlots() {
        table.slotToCard[1] = 3;
        table.slotToCard[2] = 5;
        table.cardToSlot[3] = 1;
        table.cardToSlot[5] = 2;
        return 2;
    }

    @AfterEach
    void tearDown() {
        assertInvariants();
    }

    @Test
    void point() {

        // force table.countCards to return 3


        //------------NEED HELP HERE----------------
        // when(table.countCards()).thenReturn(3); // this part is just for demonstration
        // -----------------------------------------

        
        // calculate the expected score for later
        int expectedScore = player.score() + 1;

        // call the method we are testing
        player.point();

        // check that the score was increased correctly
        assertEquals(expectedScore, player.score());

        // check that ui.setScore was called with the player's id and the correct score
        verify(ui).setScore(eq(player.id), eq(expectedScore));
    }

    @Test
    void keyPressed(){
        fillSomeSlots();
        player.keyPressed(0);
        player.keyPressed(2);

        //there is no card on slot 0
        assertEquals(false, table.isToken(player.id, 0));

        //add token from slot 2
        assertEquals(true, table.isToken(player.id, 2));
    }

    @Test 
    void penaltyTest(){
        int scoreBefore = player.score();
        player.penalty();
        assertEquals(scoreBefore, player.score());

    }
}