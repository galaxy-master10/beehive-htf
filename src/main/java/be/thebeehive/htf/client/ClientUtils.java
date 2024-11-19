package be.thebeehive.htf.client;

import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage;
import be.thebeehive.htf.library.protocol.server.GameRoundServerMessage.Values;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ZERO;

public class ClientUtils {

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

    public static boolean isDead(Values values) {
        return values.getHealth().compareTo(ZERO) == 0 ||
                values.getCrew().compareTo(ZERO) == 0;
    }

    public static boolean isAlive(Values values) {
        return !isDead(values);
    }

    public static Values getAllActionValues(GameRoundServerMessage.Action action) {
        return action.getValues();
    }

    public static List<Values> getAllEffectValues(List<GameRoundServerMessage.Effect> effects) {
        return effects.stream().map(GameRoundServerMessage.Effect::getValues).collect(Collectors.toList());
    }

    public static List<Long> getActionToExecuteBasedOnEffect(List<Values> effects, List<Long> actionsToBeExecuted, GameRoundServerMessage.Action action) {
        // Calculate the cumulative negative effects
        BigDecimal totalHealthLoss = effects.stream()
                .map(Values::getHealth)
                .filter(health -> health.compareTo(ZERO) < 0)
                .reduce(ZERO, BigDecimal::add);

        BigDecimal totalCrewLoss = effects.stream()
                .map(Values::getCrew)
                .filter(crew -> crew.compareTo(ZERO) < 0)
                .reduce(ZERO, BigDecimal::add);

        // If there is any health loss, check if the action can repair health
        if (totalHealthLoss.compareTo(ZERO) < 0 && action.getValues().getHealth().compareTo(ZERO) > 0) {
            actionsToBeExecuted.add(action.getId());
        }

        // If there is any crew loss, check if the action can recruit crew
        if (totalCrewLoss.compareTo(ZERO) < 0 && action.getValues().getCrew().compareTo(ZERO) > 0) {
            actionsToBeExecuted.add(action.getId());
        }

        return actionsToBeExecuted;
    }
}