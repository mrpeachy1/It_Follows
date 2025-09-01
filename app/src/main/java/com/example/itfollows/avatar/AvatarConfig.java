package com.example.itfollows.avatar;

public class AvatarConfig {
    public static final int WIDTH = 16;
    public static final int HEIGHT = 16;

    // 8-color default NES-ish palette (ARGB)
    public int[] palette = new int[]{
            0xFF000000, // 0 black
            0xFFFFFFFF, // 1 white
            0xFF7C7C7C, // 2 gray
            0xFFE84B3C, // 3 red
            0xFFF2D63B, // 4 yellow
            0xFF4DBD33, // 5 green
            0xFF3C6EE8, // 6 blue
            0xFF8E3CE8  // 7 purple
    };

    // Each entry = palette index [0..palette.length-1], -1 = transparent
    public int[] pixels = new int[WIDTH * HEIGHT];

    public AvatarConfig() {
        for (int i = 0; i < pixels.length; i++) pixels[i] = -1; // start fully transparent
        // Seed: simple face (optional)
        dot(7, 6, 1); dot(8, 6, 1); // eyes
        line(6, 10, 9, 10, 3);      // mouth (red)
        fillRect(5, 3, 10, 12, 2);  // light-gray head
    }

    private void dot(int x, int y, int c){ set(x,y,c); }
    private void line(int x0,int y0,int x1,int y1,int c){ for(int x=x0;x<=x1;x++) set(x,y0,c); }
    private void fillRect(int x0,int y0,int x1,int y1,int c){
        for(int y=y0;y<=y1;y++) for(int x=x0;x<=x1;x++) set(x,y,c);
    }

    public void set(int x,int y,int c){
        if (x<0||y<0||x>=WIDTH||y>=HEIGHT) return;
        pixels[y*WIDTH + x] = c;
    }
    public int get(int x,int y){
        if (x<0||y<0||x>=WIDTH||y>=HEIGHT) return -1;
        return pixels[y*WIDTH + x];
    }
}
