package xyz.nikitacartes.easyauth.config;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static xyz.nikitacartes.easyauth.utils.EasyLogger.LogError;

public class ConfigFile {
    private String configPath;
    private String header;
    private List<ConfigEntry<?>> entries;

    public ConfigFile() {
        entries = new ArrayList<>();
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public void setEntries(List<ConfigEntry<?>> entries) {
        this.entries = entries;
    }

    public void addEntry(ConfigEntry<?> entry) {
        entries.add(entry);
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    public void writeToFile() {
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(configPath), StandardCharsets.UTF_8)) {
            if (header != null) {
                writer.write(header);
                writer.write("\n");
                writer.write("\n");
            }

            for (ConfigEntry<?> entry : entries) {
                for (String comment : entry.getComments()) {
                    writer.write("# ");
                    writer.write(comment);
                    writer.write("\n");
                }
                if (entry.getLink() != null) {
                    writer.write("# For more information, see ");
                    writer.write(entry.getLink());
                    writer.write("\n");
                }
                writer.write(entry.getKey() + ": " + entry.getValue().toString());
                writer.write("\n");
                writer.write("\n");
            }
            writer.flush();
        } catch (IOException e) {
            LogError("Problem occurred when saving config", e);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        for (ConfigEntry<?> entry : entries) {
            for (String comment : entry.getComments()) {
                sb.append("  # ");
                sb.append(comment);
                sb.append("\n");
            }
            if (entry.getLink() != null) {
                sb.append("  # For more information, see ");
                sb.append(entry.getLink());
                sb.append("\n  ");
            }
            sb.append("  ").append(entry.getKey()).append(": ").append(entry.getValue().toString());
            sb.append("\n");
        }
        sb.append("}");
        return sb.toString();
    }

}
