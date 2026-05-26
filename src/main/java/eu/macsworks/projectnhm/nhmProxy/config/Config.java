package eu.macsworks.projectnhm.nhmProxy.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

public class Config {

    private final Path file;
    private final Properties props = new Properties();

    public Config(Path dataDirectory, String fileName, Map<String, String> defaults) throws IOException {
        Files.createDirectories(dataDirectory);
        this.file = dataDirectory.resolve(fileName);

        if (Files.notExists(file)) {
            defaults.forEach(props::setProperty);
            try (OutputStream out = Files.newOutputStream(file)) {
                props.store(out, fileName);
            }
        } else {
            try (InputStream in = Files.newInputStream(file)) {
                props.load(in);
            }
            boolean updated = false;
            for (Map.Entry<String, String> entry : defaults.entrySet()) {
                if (!props.containsKey(entry.getKey())) {
                    props.setProperty(entry.getKey(), entry.getValue());
                    updated = true;
                }
            }
            if (updated) {
                try (OutputStream out = Files.newOutputStream(file)) {
                    props.store(out, fileName);
                }
            }
        }
    }

    public String getString(String key) {
        return props.getProperty(key);
    }

    public int getInt(String key) {
        return Integer.parseInt(props.getProperty(key));
    }

    public boolean getBoolean(String key) {
        return Boolean.parseBoolean(props.getProperty(key));
    }
}