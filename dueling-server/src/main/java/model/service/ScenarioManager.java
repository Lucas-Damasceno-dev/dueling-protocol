package model.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import model.Card;

import java.io.Serializable;
public class ScenarioManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private Card activeScenario = null;
    private int scenarioDuration = 0;

    @JsonCreator
    public ScenarioManager(
            @JsonProperty("activeScenario") Card activeScenario,
            @JsonProperty("scenarioDuration") int scenarioDuration) {
        this.activeScenario = activeScenario;
        this.scenarioDuration = scenarioDuration;
    }

    public ScenarioManager() {
        this.activeScenario = null;
        this.scenarioDuration = 0;
    }

    public void setActiveScenario(Card card, int duration) {
        this.activeScenario = card;
        this.scenarioDuration = duration;
    }

    public int tickScenario() {
        if (scenarioDuration > 0 && activeScenario != null) {
            scenarioDuration--;
            return activeScenario.getAttack();
        }
        return 0;
    }

    public boolean isScenarioActive() {
        return activeScenario != null && scenarioDuration > 0;
    }

    public Card getActiveScenario() {
        return activeScenario;
    }

    public void clearScenario() {
        activeScenario = null;
        scenarioDuration = 0;
    }
}