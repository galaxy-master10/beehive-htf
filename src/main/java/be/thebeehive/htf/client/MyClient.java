package be.thebeehive.htf.client;

import be.thebeehive.htf.library.HtfClient;
import be.thebeehive.htf.library.HtfClientListener;
import be.thebeehive.htf.library.protocol.client.SelectActionsClientMessage;
import be.thebeehive.htf.library.protocol.server.ErrorServerMessage;
import be.thebeehive.htf.library.protocol.server.GameEndedServerMessage;
import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage;
import be.thebeehive.htf.library.protocol.server.WarningServerMessage;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

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

        // Determine the cumulative negative effects
        BigDecimal healthLoss = new BigDecimal(0);
        BigDecimal healthMaxLoss = new BigDecimal(0);
        BigDecimal crewloss = new BigDecimal(0);
        BigDecimal crewMaxLoss = new BigDecimal(0);
        for (GameRoundServerMessage.Values effect : valueEffects) {
            healthLoss.add(effect.getHealth());
            healthMaxLoss.add(effect.getMaxHealth());
            crewloss.add(effect.getCrew());
            crewMaxLoss.add(effect.getMaxCrew());
        }

        System.out.println("Expected health loss: " + healthLoss);
        System.out.println("Expected max health loss: " + healthMaxLoss);
        System.out.println("Expected crew loss: " + crewloss);
        System.out.println("Expected max crew loss: " + crewMaxLoss);

        // 3. Analyze available actions
        List<GameRoundServerMessage.Action> valuesActions = msg.getActions();
        System.out.println("Available Actions:");
        valuesActions.stream().map(GameRoundServerMessage.Action::getValues).map(GameRoundServerMessage.Values::toString).forEach(System.out::println);

        // Prioritize actions
        for (GameRoundServerMessage.Action action : valuesActions) {
            // Example: Check if the action can repair health or recruit crew
            GameRoundServerMessage.Values values = action.getValues();
            BigDecimal healthGain = values.getHealth();
            BigDecimal maxHealthGain = values.getMaxHealth();
            BigDecimal crewGain = values.getCrew();
            BigDecimal maxCrewGain = values.getMaxCrew();

            // Prioritize actions that address the most severe expected losses
            if (healthLoss.intValue() > 0 && healthGain.intValue() > 0) {
                actionsToBeExecuted.add(action.getId()); // Choose an action that heals
                break;
            } else if (crewloss.intValue() > 0 && crewGain.intValue() > 0) {
                actionsToBeExecuted.add(action.getId()); // Choose an action that recruits crew
                break;
            }
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
