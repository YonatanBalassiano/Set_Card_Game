package bguspl.set.ex;


import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import bguspl.set.Config;
import bguspl.set.Env;
import bguspl.set.UserInterface;
import bguspl.set.Util;

@ExtendWith(MockitoExtension.class)
class DealerTest {

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
    private Player[] players;





    void assertInvariants() {
        assertTrue(player.id >= 0);
        assertTrue(player.score() >= 0);
    }

    @BeforeEach
    void setUp() {
        // purposely do not find the configuration files (use defaults here).
        Env env = new Env(logger, new Config(logger, ""), ui, util);
        table = new Table(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
        player = new Player(env, dealer, table, 0, false);
        players = new Player[1];
        players[0] = player;
        dealer = new Dealer(env, table, players);
        assertInvariants();
    }
    
    @AfterEach
    void tearDown() {
        assertInvariants();
    }

    @Test
    void resetClock(){
        dealer.ClockReset();
        assertTrue(dealer.getReshuffleTime()>=System.currentTimeMillis()+59000);
    }

    @Test
    void testSetFreeze(){
        //test if the freeze time is set correctly
        dealer.setFreeze(3000, player);
        verify(ui).setFreeze(player.id,3000);


    }

}        
