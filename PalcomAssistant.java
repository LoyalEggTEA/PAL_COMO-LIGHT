
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class PalcomAssistant {

    private static final String[] RESPONSES = {
            "Hmm... interesting.",
            "I think that's cool.",
            "Tell me more.",
            "I'm not sure about that.",
            "That sounds important.",
            "Processing... done.",
            "Can you explain differently?",
            "I agree... probably.",
            "That's weird, but okay.",
            "Let me think... nope."
    };

    private static String generateResponse(String input) {
        input = input.toLowerCase();

        if (input.contains("hello") || input.contains("hi")) {
            return "Hello there.";
        }
        if (input.contains("how are you")) {
            return "Operating within normal parameters.";
        }
        if (input.contains("bye")) {
            return "Goodbye.";
        }

        return RESPONSES[new Random().nextInt(RESPONSES.length)];
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("PALCOM-lite");
            frame.setSize(220, 140);
            frame.setUndecorated(true);
            frame.setAlwaysOnTop(true);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setLocation(screenSize.width - 240, screenSize.height - 180);

            JPanel panel = new JPanel();
            panel.setBackground(new Color(0, 0, 0, 180));
            panel.setLayout(new BorderLayout());

            JLabel text = new JLabel("Hi. I'm PALCOM-lite.");
            text.setForeground(Color.WHITE);
            text.setFont(new Font("Monospaced", Font.PLAIN, 12));
            text.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

            JTextField inputField = new JTextField();

            inputField.addActionListener(e -> {
                String userInput = inputField.getText();
                text.setText(generateResponse(userInput));
                inputField.setText("");
            });

            panel.add(text, BorderLayout.CENTER);
            panel.add(inputField, BorderLayout.SOUTH);

            frame.add(panel);
            frame.setVisible(true);

            new Timer(5000, new ActionListener() {
                Random rand = new Random();

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (rand.nextDouble() < 0.3) {
                        text.setText(RESPONSES[rand.nextInt(RESPONSES.length)]);
                    }
                }
            }).start();
        });
    }
}
