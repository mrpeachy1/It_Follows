package com.example.itfollows.models;

public class SnailSprite {
    public String name;
    public String identifier;
    public int thumbnailResId;
    public int gameSpriteResId;

    public SnailSprite(String name, String identifier, int thumbnailResId, int gameSpriteResId) {
        this.name = name;
        this.identifier = identifier;
        this.thumbnailResId = thumbnailResId;
        this.gameSpriteResId = gameSpriteResId;
    }
}