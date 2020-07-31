package dev.spaceseries.spacechat.storage;

import dev.spaceseries.spacechat.configuration.Config;
import dev.spaceseries.spacechat.storage.impl.mysql.MysqlStorage;
import dev.spaceseries.spacechat.storage.impl.yaml.YamlStorage;
import lombok.Getter;

public class StorageManager {

    /**
     * The current storage medium
     */
    @Getter
    private IStorage current;

    /**
     * Initializes storage
     */
    public StorageManager() {
        // get active storage type
        String using = Config.USE.get(Config.get());

        // if type, etc....
        if (using.equalsIgnoreCase("yaml")) {
            current = new YamlStorage();
        } else if (using.equalsIgnoreCase("mysql")) {
            current = new MysqlStorage();
        }
    }
}