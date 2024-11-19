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
import java.util.Comparator;
import java.util.List;

import static java.math.BigDecimal.ZERO;

public class MyClient implements HtfClientListener {

    /**
     * An error occurred, and we are unable to recover from this.
     * You will also be disconnected.
     */
    @Override
    public void onErrorServerMessage(HtfClient client, ErrorServerMessage msg) throws Exception {
        // Handle error messages if necessary
        System.err.println("Error from server: " + msg);
    }

    /**
     * The game finished. Did you win?
     */
    @Override
    public void onGameEndedServerMessage(HtfClient client, GameEndedServerMessage msg) throws Exception {
        // Handle game end messages if necessary
        System.out.println("Game ended. " + msg);
    }

    /**
     * A new round has started.
     * You must reply within 1 second!
     */
    @Override
    public void onGameRoundServerMessage(HtfClient client, GameRoundServerMessage msg) throws Exception {
        List<Long> actionsToBeExecuted = new ArrayList<>();

        // Step 1: Analyze incoming effects
        List<GameRoundServerMessage.Values> effectValues = ClientUtils.getAllEffectValues(msg.getEffects());

        System.out.println("Effects received for this round:");
        for (GameRoundServerMessage.Values effect : effectValues) {
            System.out.println(effect);
        }

        // Calculate total negative health and crew effects
        BigDecimal totalHealthLossBD = effectValues.stream()
                .map(GameRoundServerMessage.Values::getHealth)
                .filter(health -> health.compareTo(ZERO) < 0)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal totalCrewLossBD = effectValues.stream()
                .map(GameRoundServerMessage.Values::getCrew)
                .filter(crew -> crew.compareTo(ZERO) < 0)
                .reduce(ZERO, BigDecimal::add);

        double totalHealthLoss = totalHealthLossBD.abs().doubleValue(); // Positive value representing loss
        double totalCrewLoss = totalCrewLossBD.abs().doubleValue(); // Positive value representing loss

        System.out.println("Total Health Loss: " + totalHealthLoss);
        System.out.println("Total Crew Loss: " + totalCrewLoss);

        // Step 2: Evaluate available actions
        List<GameRoundServerMessage.Action> availableActions = new ArrayList<>(msg.getActions());

        System.out.println("Available actions for this round:");
        for (GameRoundServerMessage.Action action : availableActions) {
            System.out.println(action);
        }

        // Score each action
        List<ScoredAction> scoredActions = new ArrayList<>();
        for (GameRoundServerMessage.Action action : availableActions) {
            double score = ClientUtils.scoreAction(action, totalHealthLoss, totalCrewLoss);
            scoredActions.add(new ScoredAction(action, score));
        }

        // Sort actions based on score descending
        scoredActions.sort(Comparator.comparingDouble(ScoredAction::getScore).reversed());

        // Step 3: Select optimal actions
        double remainingHealthLoss = totalHealthLoss;
        double remainingCrewLoss = totalCrewLoss;
        double currentHealthGain = 0;
        double currentCrewGain = 0;

        boolean prioritizeMaxHealth = msg.getRound() < 20; // Prioritize max health upgrades in the early game
        boolean ensureResourcesForCheckpoint = true; // Always ensure enough resources for the next checkpoint

        for (ScoredAction scoredAction : scoredActions) {
            GameRoundServerMessage.Action action = scoredAction.getAction();

            // Calculate potential mitigation
            double healthMitigation = Math.min(action.getValues().getHealth().doubleValue(), remainingHealthLoss);
            double crewMitigation = Math.min(action.getValues().getCrew().doubleValue(), remainingCrewLoss);

            // Only consider actions that mitigate at least one type of loss or provide significant gain
            if (healthMitigation > 0 || crewMitigation > 0 || action.getValues().getHealth().doubleValue() > 0 || action.getValues().getCrew().doubleValue() > 0) {
                // In the early rounds, prioritize increasing max health to prepare for later large hits
                if (prioritizeMaxHealth && action.getValues().getMaxHealth().doubleValue() > 0) {
                    actionsToBeExecuted.add(action.getId());
                    System.out.println("Selected Action ID (MaxHealth Priority): " + action.getId() +
                            " | Max Health Gain: " + action.getValues().getMaxHealth().doubleValue() +
                            " | Score: " + scoredAction.getScore());
                    continue;
                }

                // Ensure enough resources before next checkpoint
                if (ensureResourcesForCheckpoint) {
                    GameRoundServerMessage.Values checkpointValues = msg.getNextCheckpoint().getValues();
                    BigDecimal resultingHealth = action.getValues().getHealth().add(msg.getOurSpaceship().getValues().getHealth());
                    BigDecimal resultingCrew = action.getValues().getCrew().add(msg.getOurSpaceship().getValues().getCrew());
                    BigDecimal resultingMaxHealth = action.getValues().getMaxHealth().add(msg.getOurSpaceship().getValues().getMaxHealth());
                    BigDecimal resultingMaxCrew = action.getValues().getMaxCrew().add(msg.getOurSpaceship().getValues().getMaxCrew());

                    if (resultingHealth.compareTo(checkpointValues.getHealth()) >= 0 &&
                            resultingCrew.compareTo(checkpointValues.getCrew()) >= 0 &&
                            resultingMaxHealth.compareTo(ZERO) > 0 && resultingMaxCrew.compareTo(ZERO) > 0) {
                        actionsToBeExecuted.add(action.getId());
                        System.out.println("Selected Action ID (Ensure Resources for Checkpoint): " + action.getId() +
                                " | Health Gain: " + action.getValues().getHealth() +
                                " | Crew Gain: " + action.getValues().getCrew() +
                                " | Max Health Gain: " + action.getValues().getMaxHealth() +
                                " | Max Crew Gain: " + action.getValues().getMaxCrew() +
                                " | Score: " + scoredAction.getScore());
                        continue;
                    }
                }

                // Avoid actions with negative impact on max health or max crew if possible
                if (action.getValues().getMaxHealth().doubleValue() < 0 || action.getValues().getMaxCrew().doubleValue() < 0) {
                    System.out.println("Skipping Action ID: " + action.getId() + " due to negative impact on max health or max crew.");
                    continue;
                }

                actionsToBeExecuted.add(action.getId());

                // Update remaining losses
                remainingHealthLoss -= healthMitigation;
                remainingCrewLoss -= crewMitigation;
                currentHealthGain += action.getValues().getHealth().doubleValue();
                currentCrewGain += action.getValues().getCrew().doubleValue();

                System.out.println("Selected Action ID: " + action.getId() +
                        " | Health Mitigation: " + healthMitigation +
                        " | Crew Mitigation: " + crewMitigation +
                        " | Health Gain: " + action.getValues().getHealth().doubleValue() +
                        " | Crew Gain: " + action.getValues().getCrew().doubleValue() +
                        " | Max Health Gain: " + action.getValues().getMaxHealth().doubleValue() +
                        " | Max Crew Gain: " + action.getValues().getMaxCrew().doubleValue() +
                        " | Score: " + scoredAction.getScore());

                // If all losses are mitigated, break
                if (remainingHealthLoss <= 0 && remainingCrewLoss <= 0) {
                    break;
                }
            }
        }

        // Step 4: Handle any remaining losses by selecting least harmful actions
        if (remainingHealthLoss > 0 || remainingCrewLoss > 0) {
            for (ScoredAction scoredAction : scoredActions) {
                GameRoundServerMessage.Action action = scoredAction.getAction();
                if (!actionsToBeExecuted.contains(action.getId())) {
                    // Avoid actions with negative impact on max health or max crew if possible
                    if (action.getValues().getMaxHealth().doubleValue() < 0 || action.getValues().getMaxCrew().doubleValue() < 0) {
                        System.out.println("Skipping Additional Action ID: " + action.getId() + " due to negative impact on max health or max crew.");
                        continue;
                    }
                    // Even if the action doesn't fully mitigate, select it to reduce losses
                    actionsToBeExecuted.add(action.getId());
                    System.out.println("Additional Action ID: " + action.getId() +
                            " | Score: " + scoredAction.getScore());

                    // Update remaining losses if possible
                    double healthMitigation = Math.min(action.getValues().getHealth().doubleValue(), remainingHealthLoss);
                    double crewMitigation = Math.min(action.getValues().getCrew().doubleValue(), remainingCrewLoss);

                    remainingHealthLoss -= healthMitigation;
                    remainingCrewLoss -= crewMitigation;
                    currentHealthGain += action.getValues().getHealth().doubleValue();
                    currentCrewGain += action.getValues().getCrew().doubleValue();

                    if (remainingHealthLoss <= 0 && remainingCrewLoss <= 0) {
                        break;
                    }
                }
            }
        }

        // Step 5: Decide whether to execute the selected actions or take no action
        double totalActionScore = scoredActions.stream()
                .filter(sa -> actionsToBeExecuted.contains(sa.getAction().getId()))
                .mapToDouble(ScoredAction::getScore)
                .sum();

        boolean laterGame = msg.getRound() >= 20;
        if (laterGame) {
            // In later rounds, avoid actions that could significantly reduce max health or max crew
            actionsToBeExecuted.removeIf(actionId -> {
                GameRoundServerMessage.Action action = availableActions.stream()
                        .filter(a -> a.getId() == actionId)
                        .findFirst()
                        .orElse(null);
                if (action == null) return false;
                return action.getValues().getMaxHealth().doubleValue() < 0 || action.getValues().getMaxCrew().doubleValue() < 0;
            });
        }

        // Introduce a minimum score threshold for executing actions
        double minimumScoreThreshold = 50.0;
        if (totalActionScore < minimumScoreThreshold && (currentHealthGain <= totalHealthLoss || currentCrewGain <= totalCrewLoss)) {
            // If the net benefit is not positive and the current gains do not compensate the losses, or if the total score is below the threshold, opt to take no actions
            actionsToBeExecuted.clear();
            System.out.println("No beneficial actions found or score below threshold. Taking no actions to avoid negative impacts.");
        } else {
            System.out.println("Total Action Score: " + totalActionScore);
            System.out.println("Actions to be executed: " + actionsToBeExecuted);
        }

        // Step 6: Send selected actions
        client.send(new SelectActionsClientMessage(msg.getRoundId(), actionsToBeExecuted));
    }

    /**
     * A warning occurred but you can still play along.
     * You will NOT be disconnected.
     */
    @Override
    public void onWarningServerMessage(HtfClient client, WarningServerMessage msg) throws Exception {
        // Handle warning messages if necessary
        System.out.println("Warning from server: " + msg);
    }

    /**
     * Helper class to associate an action with its score.
     */
    private static class ScoredAction {
        private final GameRoundServerMessage.Action action;
        private final double score;

        public ScoredAction(GameRoundServerMessage.Action action, double score) {
            this.action = action;
            this.score = score;
        }

        public GameRoundServerMessage.Action getAction() {
            return action;
        }

        public double getScore() {
            return score;
        }
    }
}


