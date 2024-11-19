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
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class MyClient implements HtfClientListener {

    /**
     * You tried to perform an action that is not allowed.
     * An error occurred, and we are unable to recover from this.
     * You will also be disconnected.
     */
    @Override
    public void onErrorServerMessage(HtfClient client, ErrorServerMessage msg) throws Exception {
        System.out.println(msg.getMsg());
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
        List<Long> actionsToBeExecuted = Collections.emptyList();

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

        if ( valueEffects.isEmpty() ) {
            if ( maxHealth.compareTo(currentHealth) > 0 ) {
                actionsToBeExecuted.addAll(getActionsToIncreaseHealth(msg.getActions()));
            }
            if ( maxCrew.compareTo(currentCrew) > 0 ) {
                actionsToBeExecuted.addAll(getActionsToIncreaseCrew(msg.getActions()));
            }
            if ( maxHealth.compareTo(currentHealth) == 0 ) {
                actionsToBeExecuted.addAll(getActionsToIncreaseMaxHealth(msg.getActions()));
            }
            if (maxCrew.compareTo(currentCrew) == 0 ) {
                actionsToBeExecuted.addAll(getActionsToIncreaseMaxCrew(msg.getActions()));
            }
        } else {
            for (GameRoundServerMessage.Action action : msg.getActions()) {
                actionsToBeExecuted.addAll(ClientUtils.getActionToExecuteBasedOnEffect(valueEffects, action));
            }
        }


        actionsToBeExecuted.forEach(System.out::println);

        client.send(new SelectActionsClientMessage(msg.getRoundId(), actionsToBeExecuted));
    }

    private List<Long> getActionsByCondition(
            List<GameRoundServerMessage.Action> actions,
            Function<GameRoundServerMessage.Values, BigDecimal> conditionExtractor) {
        List<Long> foundActions = new ArrayList<>();

        for (GameRoundServerMessage.Action action : actions) {
            GameRoundServerMessage.Values values = action.getValues();

            // Compare the extracted BigDecimal value to zero
            if (conditionExtractor.apply(values).compareTo(BigDecimal.ZERO) > 0) {
                foundActions.add(action.getId());
            }
        }

        return foundActions;
    }

    private List<Long> getActionsToIncreaseMaxCrew(List<GameRoundServerMessage.Action> actions) {
        return getActionsByCondition(actions, GameRoundServerMessage.Values::getMaxCrew);
    }

    private List<Long> getActionsToIncreaseMaxHealth(List<GameRoundServerMessage.Action> actions) {
        return getActionsByCondition(actions, GameRoundServerMessage.Values::getMaxHealth);
    }

    private List<Long> getActionsToIncreaseCrew(List<GameRoundServerMessage.Action> actions) {
        return getActionsByCondition(actions, GameRoundServerMessage.Values::getCrew);
    }

    private List<Long> getActionsToIncreaseHealth(List<GameRoundServerMessage.Action> actions) {
        return getActionsByCondition(actions, GameRoundServerMessage.Values::getHealth);
    }


    private BigDecimal checkMaxHealthToHealth(BigDecimal currentHealth, BigDecimal maxHealth) {
        if (currentHealth == null || maxHealth == null) {
            throw new IllegalArgumentException("Health values must be non-null.");
        }
        // Calculate the difference: maxHealth - currentHealth
        return maxHealth.subtract(currentHealth);
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
