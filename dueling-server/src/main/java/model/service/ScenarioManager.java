package model.service;

import controller.GameFacade;
import model.Card;
import model.Player;

import java.io.Serializable;
public class ScenarioManager implements Serializable {
    private static final long serialVersionUID = 1L;
    private Card activeScenario = null;
    private int scenarioDuration = 0;

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