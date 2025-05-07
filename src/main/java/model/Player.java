package model;
import lombok.Data;
import lombok.Setter;

@Data
public class Player {
    private String id;
    private String name;
    private int money;
    private static final int STARTING_MONEY = 1500; // Standard Monopoly starting money
    private int position = 0; // Starting position
    @Setter
    private boolean hasRolledThisTurn = false;

    public Player(String id, String name) {
        this.id = id;
        this.name = name;
        this.money = STARTING_MONEY;
    }

    public void addMoney(int amount) {
        this.money += amount;
    }

    public void subtractMoney(int amount) {
        this.money -= amount;
    }

    public boolean hasRolledThisTurn() {
        return hasRolledThisTurn;
    }
}