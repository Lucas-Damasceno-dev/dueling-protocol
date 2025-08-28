package model;

import java.util.List;

public interface CardPack {
    String getName();
    int getCost();
    List<Card> open();
}