package repository;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import model.Player;
import java.io.*;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerRepositoryJson implements PlayerRepository {
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final String basePath = "players/";
    
    private static final Logger logger = LoggerFactory.getLogger(PlayerRepositoryJson.class);

    @Override
    public void save(Player player) {
        try {
            File dir = new File(basePath);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    logger.error("Não foi possível criar o diretório: {}", basePath);
                    return;
                }
            }
            FileWriter writer = new FileWriter(basePath + player.getId() + ".json");
            gson.toJson(player, writer);
            writer.close();
            logger.debug("Jogador {} salvo com sucesso", player.getId());
        } catch (IOException e) {
            logger.error("Erro ao salvar jogador {}: {}", player.getId(), e.getMessage(), e);
        }
    }

    @Override
    public Optional<Player> findById(String id) {
        try {
            File file = new File(basePath + id + ".json");
            if (!file.exists()) {
                logger.debug("Arquivo de jogador {} não encontrado", id);
                return Optional.empty();
            }
            FileReader reader = new FileReader(file);
            Player player = gson.fromJson(reader, Player.class);
            reader.close();
            logger.debug("Jogador {} carregado com sucesso", id);
            return Optional.ofNullable(player);
        } catch (Exception e) {
            logger.error("Erro ao carregar jogador {}: {}", id, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public void update(Player player) {
        save(player);
    }
}

