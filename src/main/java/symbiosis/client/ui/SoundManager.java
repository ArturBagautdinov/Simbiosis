package symbiosis.client.ui;

import javafx.scene.media.AudioClip;

public class SoundManager {

    private static AudioClip moveClip;
    private static AudioClip actionClip;
    private static AudioClip winClip;

    static {
        try {
            var moveUrl = SoundManager.class.getResource("/sound/move.wav");
            if (moveUrl != null) {
                moveClip = new AudioClip(moveUrl.toExternalForm());
            }
        } catch (Exception ignored) {}

        try {
            var actionUrl = SoundManager.class.getResource("/sound/action.wav");
            if (actionUrl != null) {
                actionClip = new AudioClip(actionUrl.toExternalForm());
            }
        } catch (Exception ignored) {}

        try {
            var winUrl = SoundManager.class.getResource("/sound/win.wav");
            if (winUrl != null) {
                winClip = new AudioClip(winUrl.toExternalForm());
            }
        } catch (Exception ignored) {}
    }

    public static void playMove() {
        if (moveClip != null) {
            moveClip.play();
        }
    }

    public static void playAction() {
        if (actionClip != null) {
            actionClip.play();
        }
    }

    public static void playWin() {
        if (winClip != null) {
            winClip.play();
        }
    }
}
