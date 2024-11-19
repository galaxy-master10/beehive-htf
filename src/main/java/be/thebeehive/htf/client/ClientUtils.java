package be.thebeehive.htf.client;

import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage;
import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage.Values;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;

public class ClientUtils {

    /**
     * Sums two Values objects, ensuring that health and crew do not exceed their maximums
     * or drop below zero.
     *
     * @param original  The original Values.
     * @param newValues The Values to add.
     * @return A new Values object representing the sum.
     */
    public static Values sumValues(Values original, Values newValues) {
        BigDecimal maxHealth = original.getMaxHealth().add(newValues.getMaxHealth());
        BigDecimal maxCrew = original.getMaxCrew().add(newValues.getMaxCrew());

        if (maxHealth.compareTo(ZERO) < 0) {
            maxHealth = ZERO;
        }

        if (maxCrew.compareTo(ZERO) < 0) {
            maxCrew = ZERO;
        }

        BigDecimal health = original.getHealth().add(newValues.getHealth());
        BigDecimal crew = original.getCrew().add(newValues.getCrew());

        if (health.compareTo(maxHealth) > 0) {
            health = maxHealth;
        }

        if (health.compareTo(ZERO) < 0) {
            health = ZERO;
        }

        if (crew.compareTo(maxCrew) > 0) {
            crew = maxCrew;
        }

        if (crew.compareTo(ZERO) < 0) {
            crew = ZERO;
        }

        Values sum = new Values();
        sum.setHealth(health);
        sum.setMaxHealth(maxHealth);
        sum.setCrew(crew);
        sum.setMaxCrew(maxCrew);

        return sum;
    }

    /**
     * Checks if the spaceship is dead based on its current Values.
     *
     * @param values The current Values of the spaceship.
     * @return True if dead, else false.
     */
    public static boolean isDead(Values values) {
        return values.getHealth().compareTo(ZERO) == 0 ||
                values.getCrew().compareTo(ZERO) == 0;
    }

    /**
     * Checks if the spaceship is alive based on its current Values.
     *
     * @param values The current Values of the spaceship.
     * @return True if alive, else false.
     */
    public static boolean isAlive(Values values) {
        return !isDead(values);
    }

    /**
     * Retrieves all Values from a list of Actions.
     *
     * @param actions The list of Actions.
     * @return A list of Values from the Actions.
     */
    public static List<Values> getAllActionValues(List<GameRoundServerMessage.Action> actions) {
        return actions.stream()
                .map(GameRoundServerMessage.Action::getValues)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all Values from a list of Effects.
     *
     * @param effects The list of Effects.
     * @return A list of Values from the Effects.
     */
    public static List<Values> getAllEffectValues(List<GameRoundServerMessage.Effect> effects) {
        return effects.stream()
                .map(GameRoundServerMessage.Effect::getValues)
                .collect(Collectors.toList());
    }

    /**
     * Scores an action based on its ability to mitigate negative effects and minimize new negative side effects.
     *
     * @param action          The action to score.
     * @param totalHealthLoss The total health loss to mitigate.
     * @param totalCrewLoss   The total crew loss to mitigate.
     * @return The calculated score for the action.
     */
    public static double scoreAction(GameRoundServerMessage.Action action, double totalHealthLoss, double totalCrewLoss) {
        double score = 0.0;

        // Positive impacts
        double healthGain = action.getValues().getHealth().doubleValue();
        double crewGain = action.getValues().getCrew().doubleValue();

        score += healthGain;
        score += crewGain;

        // Negative side effects
        double maxHealthChange = action.getValues().getMaxHealth().doubleValue();
        double maxCrewChange = action.getValues().getMaxCrew().doubleValue();

        // Penalize actions that reduce maxHealth or maxCrew
        if (maxHealthChange < 0) {
            score += maxHealthChange * 2; // Heavier penalty
        }
        if (maxCrewChange < 0) {
            score += maxCrewChange * 2; // Heavier penalty
        }

        // Optional: Penalize actions that decrease current health or crew
        if (action.getValues().getHealth().doubleValue() < 0) {
            score += action.getValues().getHealth().doubleValue() * 1.5; // Mild penalty
        }
        if (action.getValues().getCrew().doubleValue() < 0) {
            score += action.getValues().getCrew().doubleValue() * 1.5; // Mild penalty
        }

        return score;
    }
}
