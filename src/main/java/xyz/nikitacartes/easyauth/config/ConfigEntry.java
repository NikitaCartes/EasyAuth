package xyz.nikitacartes.easyauth.config;

import java.util.List;

public class ConfigEntry<T> {
    private final List<String> comments;
    private final String key;
    private final T value;
    private final String link;

    public ConfigEntry(String key, T value, List<String> comment, String link) {
        this.comments = comment;
        this.key = key;
        this.value = value;
        this.link = link;
    }

    public ConfigEntry(String key, T value, List<String> comment) {
        this.comments = comment;
        this.key = key;
        this.value = value;
        this.link = null;
    }

    public ConfigEntry(String key, T value, String comment, String link) {
        this.comments = List.of(comment);
        this.key = key;
        this.value = value;
        this.link = link;
    }

    public ConfigEntry(String key, T value, String comment) {
        this.comments = List.of(comment);
        this.key = key;
        this.value = value;
        this.link = null;
    }

    public ConfigEntry(String key, T value) {
        this.key = key;
        this.value = value;
        this.comments = List.of();
        this.link = null;
    }

    public List<String> getComments() {
        return comments;
    }

    public String getKey() {
        return key;
    }

    public T getValue() {
        return value;
    }

    public String getLink() {
        return link;
    }
}
