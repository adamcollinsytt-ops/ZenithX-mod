package org.adam.zenithx.skins;

import com.google.gson.JsonObject;

public class CustomSkin {
    private final String id;
    private final String name;
    private final String filePath;
    private final SkinType type;
    private final boolean isLocal;
    private String url;
    private String author;
    private long dateAdded;

    public CustomSkin(String id, String name, String filePath, SkinType type, boolean isLocal) {
        this.id = id;
        this.name = name;
        this.filePath = filePath;
        this.type = type;
        this.isLocal = isLocal;
        this.dateAdded = System.currentTimeMillis();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFilePath() {
        return filePath;
    }

    public SkinType getType() {
        return type;
    }

    public boolean isLocal() {
        return isLocal;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public long getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(long dateAdded) {
        this.dateAdded = dateAdded;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("name", name);
        json.addProperty("filePath", filePath);
        json.addProperty("type", type.name());
        json.addProperty("isLocal", isLocal);
        json.addProperty("dateAdded", dateAdded);
        
        if (url != null) {
            json.addProperty("url", url);
        }
        if (author != null) {
            json.addProperty("author", author);
        }
        
        return json;
    }

    public static CustomSkin fromJson(JsonObject json) {
        CustomSkin skin = new CustomSkin(
                json.get("id").getAsString(),
                json.get("name").getAsString(),
                json.get("filePath").getAsString(),
                SkinType.valueOf(json.get("type").getAsString()),
                json.get("isLocal").getAsBoolean()
        );
        
        if (json.has("url")) {
            skin.setUrl(json.get("url").getAsString());
        }
        if (json.has("author")) {
            skin.setAuthor(json.get("author").getAsString());
        }
        if (json.has("dateAdded")) {
            skin.setDateAdded(json.get("dateAdded").getAsLong());
        }
        
        return skin;
    }

    @Override
    public String toString() {
        return name + " (" + type + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        CustomSkin that = (CustomSkin) obj;
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
