package com.example.itfollows.avatar;

public final class AvatarPresets {
    private AvatarPresets(){}

    /** Primary default the game should use if the user hasn't customized anything. */
    public static AvatarConfig defaultPreset() {
        AvatarConfig a = new AvatarConfig();
        clear(a);

        // Palette indices for readability
        final int K = 0; // black
        final int W = 1; // white
        final int G = 2; // gray (skin/head base)
        final int R = 3; // red (mouth)
        final int Y = 4; // yellow (accent)
        final int GR= 5; // green
        final int B = 6; // blue (hood border)
        final int P = 7; // purple (cheek)

        // Head base (rounded rectangle)
        fillRect(a, 4, 3, 11, 12, G);
        // round corners by clearing a few pixels
        set(a,4,3,-1); set(a,11,3,-1); set(a,4,12,-1); set(a,11,12,-1);

        // Hair line
        fillRect(a, 5, 3, 10, 4, K);

        // Eyes (2x2 whites with black pupils)
        fillRect(a, 6, 6, 7, 7, W);
        fillRect(a, 8, 6, 9, 7, W);
        set(a,6,7,K); set(a,9,7,K); // pupils

        // Nose (subtle)
        set(a,8,8,K);

        // Mouth
        fillRect(a, 6, 10, 9, 10, R);

        // Cheeks (tiny blush)
        set(a,5,8,P); set(a,10,8,P);

        // Simple blue hoodie frame
        // Left and right sides
        fillRect(a, 3, 6, 3, 11, B);
        fillRect(a, 12, 6, 12, 11, B);
        // Bottom arc
        fillRect(a, 4, 13, 11, 13, B);

        // Tiny yellow badge on hoodie
        set(a,10,12,Y);

        return a;
    }

    /** Optional fun alternatives if you want to expose more presets later. */
    public static AvatarConfig robotPreset() {
        AvatarConfig a = new AvatarConfig();
        clear(a);
        final int K=0,W=1,G=2,R=3,Y=4,GR=5,B=6,P=7;

        // Square white head
        fillRect(a, 4, 4, 11, 11, W);
        // Border
        rect(a,4,4,11,11,K);
        // Eyes (blue bars)
        fillRect(a, 6, 7, 7, 7, B);
        fillRect(a, 8, 7, 9, 7, B);
        // Mouth (thin)
        fillRect(a, 6, 9, 9, 9, G);
        // Antenna
        set(a,8,3,K); set(a,8,2,Y);
        return a;
    }

    // ---------- helpers ----------
    private static void set(AvatarConfig a,int x,int y,int c){
        a.set(x,y,c);
    }
    private static void clear(AvatarConfig a){
        for (int i=0;i<a.pixels.length;i++) a.pixels[i] = -1;
    }
    private static void fillRect(AvatarConfig a,int x0,int y0,int x1,int y1,int c){
        for(int y=y0;y<=y1;y++) for(int x=x0;x<=x1;x++) a.set(x,y,c);
    }
    private static void rect(AvatarConfig a,int x0,int y0,int x1,int y1,int c){
        for(int x=x0;x<=x1;x++){ a.set(x,y0,c); a.set(x,y1,c); }
        for(int y=y0;y<=y1;y++){ a.set(x0,y,c); a.set(x1,y,c); }
    }
}
