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

    public static List<Long> getActionToExecuteBasedOnEffect(List<Values> effects, GameRoundServerMessage.Action action) {
        List<Long> actionsToBeExecuted = new ArrayList<>(); // Use a mutable list
        // Loop over the effects
        for (Values effect : effects) {
            // Check if the effect is negative
            if (effect.getHealth().intValue() < 0) {
                // Check if the action can repair health
                if (action.getValues().getHealth().intValue() > 0) {
                    // Add the action to the list of actions to be executed
                    actionsToBeExecuted.add(action.getId());
                }
            } else if (effect.getCrew().intValue() < 0) {
                // Check if the action can recruit crew
                if (action.getValues().getCrew().intValue() > 0) {
                    // Add the action to the list of actions to be executed
                    actionsToBeExecuted.add(action.getId());
                }
            }
        }
        return actionsToBeExecuted;

    }
}