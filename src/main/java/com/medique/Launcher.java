package com.medique;

/**
 * Standard launcher to circumvent JavaFX runtime module path restrictions
 * when launching directly from a packaged fat JAR.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
