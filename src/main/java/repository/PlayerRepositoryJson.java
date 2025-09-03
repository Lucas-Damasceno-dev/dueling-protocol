package repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Player;
import java.io.*;
import java.util.Optional;

public class PlayerRepositoryJson implements PlayerRepository {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final String basePath = "players/";

    @Override
    public void save(Player player) {
        try {
            File dir = new File(basePath);
            if (!dir.exists()) dir.mkdirs();
            FileWriter writer = new FileWriter(basePath + player.getId() + ".json");
            gson.toJson(player, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Optional<Player> findById(String id) {
        try {
            File file = new File(basePath + id + ".json");
            if (!file.exists()) return Optional.empty();
            FileReader reader = new FileReader(file);
            Player player = gson.fromJson(reader, Player.class);
            reader.close();
            return Optional.of(player);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public void update(Player player) {
        save(player);
    }
}

