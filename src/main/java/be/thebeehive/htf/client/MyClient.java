package be.thebeehive.htf.client;

import be.thebeehive.htf.library.HtfClient;
import be.thebeehive.htf.library.HtfClientListener;
import be.thebeehive.htf.library.protocol.client.SelectActionsClientMessage;
import be.thebeehive.htf.library.protocol.server.ErrorServerMessage;
import be.thebeehive.htf.library.protocol.server.GameEndedServerMessage;
import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage;
import be.thebeehive.htf.library.protocol.server.WarningServerMessage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static java.math.BigDecimal.ZERO;

public class MyClient implements HtfClientListener {

    /**
     * You tried to perform an action that is not allowed.
     * An error occurred, and we are unable to recover from this.
     * You will also be disconnected.
     */
    @Override
    public void onErrorServerMessage(HtfClient client, ErrorServerMessage msg) throws Exception {

    }

    /**
     * The game finished. Did you win?
     */
    @Override
    public void onGameEndedServerMessage(HtfClient client, GameEndedServerMessage msg) throws Exception {

    }

    /**
     * A new round has started.
     * You must reply within 1 second!
     */
    @Override
    public void onGameRoundServerMessage(HtfClient client, GameRoundServerMessage msg) throws Exception {
        List<Long> actionsToBeExecuted = new ArrayList<>();

        // 1. Get spaceship stats
        GameRoundServerMessage.Spaceship ourSpaceship = msg.getOurSpaceship();
        GameRoundServerMessage.Values spaceshipStats = ourSpaceship.getValues();
        BigDecimal currentHealth = spaceshipStats.getHealth();
        BigDecimal maxHealth = spaceshipStats.getMaxHealth();
        BigDecimal currentCrew = spaceshipStats.getCrew();
        BigDecimal maxCrew = spaceshipStats.getMaxCrew();

        // 2. Analyze incoming effects
        List<GameRoundServerMessage.Values> valueEffects = ClientUtils.getAllEffectValues(msg.getEffects());
        System.out.println("Effects:");
        valueEffects.stream().map(GameRoundServerMessage.Values::toString).forEach(System.out::println);

        // Determine the cumulative negative effects
        BigDecimal healthLoss = valueEffects.stream()
                .map(GameRoundServerMessage.Values::getHealth)
                .filter(health -> health.compareTo(ZERO) < 0)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal crewLoss = valueEffects.stream()
                .map(GameRoundServerMessage.Values::getCrew)
                .filter(crew -> crew.compareTo(ZERO) < 0)
                .reduce(ZERO, BigDecimal::add);

        System.out.println("Expected health loss: " + healthLoss);
        System.out.println("Expected crew loss: " + crewLoss);

        // 3. Analyze available actions
        List<GameRoundServerMessage.Action> valuesActions = msg.getActions();
        System.out.println("Available Actions:");
        valuesActions.stream().map(GameRoundServerMessage.Action::getValues).map(GameRoundServerMessage.Values::toString).forEach(System.out::println);

        // Prioritize actions
        for (GameRoundServerMessage.Action action : valuesActions) {
            // Example: Check if the action can repair health or recruit crew
            actionsToBeExecuted = ClientUtils.getActionToExecuteBasedOnEffect(valueEffects, actionsToBeExecuted, action);
        }

        actionsToBeExecuted.forEach(System.out::println);

        client.send(new SelectActionsClientMessage(msg.getRoundId(), actionsToBeExecuted));
    }

    /**
     * You tried to perform an action that is not allowed.
     * An error occurred but you can still play along.
     * You will NOT be disconnected.
     */
    @Override
    public void onWarningServerMessage(HtfClient client, WarningServerMessage msg) throws Exception {

    }
}
