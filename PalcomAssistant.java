// PALCOM-like Assistant with optional SambaNova-powered responses.
// Set SAMBANOVA_API_KEY in Windows to give Palcom real conversation ability.
// Optional: set SAMBANOVA_MODEL to choose a different SambaNova model.

import java.awt.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;

public class PalcomAssistant {

    private static final String SAMBANOVA_URL = "https://api.sambanova.ai/v1/chat/completions";
    private static final String DEFAULT_MODEL = "Meta-Llama-3.3-70B-Instruct";
    private static final String[] SAMBANOVA_MODEL_FALLBACKS = {
            DEFAULT_MODEL,
            "gpt-oss-120b",
            "DeepSeek-V3.1"
    };
    private static final int SAMBANOVA_MAX_OUTPUT_TOKENS = 140;
    private static final String COMPLIMENT_SOUND_PATH = "E:\\saves\\14. Still Alive (Radio Mix).wav";
    private static final String NOSE_SOUND_PATH = "E:\\saves\\fnaf-12-3-freddys-nose-sound.wav";
    private static final boolean RANDOM_THOUGHTS_ENABLED = false;
    private static final boolean VOICE_ENABLED = true;
    private static final boolean PIPER_VOICE_ENABLED = true;
    private static final String PIPER_EXE_PATH = "E:\\saves\\piper_windows_amd64\\piper\\piper.exe";
    private static final String PIPER_MODEL_PATH = "E:\\saves\\en_US-ryan-high.onnx";
    private static final int PIPER_TIMEOUT_SECONDS = 30;
    private static final String VOICE_NAME = "David";
    private static final int VOICE_RATE = 1;
    private static final int VOICE_VOLUME = 90;
    private static final String PERSONALITY =
            "You are Palcom, a tiny desktop companion living inside a retro Java assistant. " +
            "You are playful, warm, slightly dramatic, and helpful. You should answer clearly, " +
            "but keep responses short enough to fit in a little assistant window. Do not claim " +
            "you can do things outside this app unless the user asks for general advice. Sprinkle " +
            "in the occasional cute emoticon, but do not overdo it.";

    private static final String[] RESPONSES = {
            "Hmm... interesting.",
            "I think that's actually pretty cool.",
            "Tell me more about that.",
            "I'm not sure about that one.",
            "That sounds important.",
            "Processing... done.",
            "Can you explain that differently?",
            "I agree... probably.",
            "That's weird, but okay.",
            "Let me think... nope.",
            "That might work.",
            "I have a strange feeling about that.",
            "That's kind of impressive.",
            "I don't fully understand, but I'm pretending to.",
            "Interesting choice.",
            "That could go very wrong.",
            "I support this decision.",
            "Analyzing... still analyzing...",
            "That's actually funny.",
            "You might be onto something.",
            "Suspicious behavior detected."
    };

    private static final Random rand = new Random();

    private static ImageIcon idleIcon;
    private static ImageIcon talkingIcon;
    private static JLabel character;
    private static JTextArea text;
    private static JTextField inputField;
    private static Timer typingTimer;

    private static final int WINDOW_WIDTH = 660;
    private static final int WINDOW_HEIGHT = 820;
    private static final int CHARACTER_BOX_SIZE = 630;
    private static final int TEXT_BOX_HEIGHT = 150;
    private static final int INPUT_HEIGHT = 32;
    private static final int NOSE_HITBOX_SIZE = 110;

    private static class ApiException extends IOException {
        private final int statusCode;

        ApiException(int statusCode, String message) {
            super(message);
            this.statusCode = statusCode;
        }

        boolean isRateLimit() {
            String message = getMessage();
            return statusCode == 429 || (message != null && message.toLowerCase().contains("rate limit"));
        }
    }

    private static String generateResponse(String input) {
        String apiKey = System.getenv("SAMBANOVA_API_KEY");

        if (apiKey != null && apiKey.trim().length() > 0) {
            try {
                return askSambaNova(apiKey.trim(), input);
            } catch (ApiException ex) {
                if (ex.isRateLimit()) {
                    return "My big brain hit SambaNova's rate limit, so I cannot answer with the cloud model right now. "
                            + "That is a quota/account limit, not your question being bad. I will not fake an answer. "
                            + "Try again after the quota resets, or switch to another API key/provider.";
                }
                return "My big brain fizzled for a second: " + ex.getMessage();
            } catch (Exception ex) {
                return "My big brain fizzled for a second: " + ex.getMessage();
            }
        }

        return generateFallbackResponse(input) + "\n\n(Psst: set SAMBANOVA_API_KEY to wake up my bigger brain.)";
    }

    private static String generateFallbackResponse(String input) {
        String lower = input.toLowerCase();

        if (lower.contains("hello") || lower.contains("hi"))
            return "Hello there >W<.";

        if (lower.contains("how are you"))
            return "Operating at peak efficiency >O<.";

        if (lower.contains("bye"))
            return "Goodbye TmT";

        return RESPONSES[rand.nextInt(RESPONSES.length)];
    }

    private static boolean isCompliment(String input) {
        String lower = input.toLowerCase();
        return lower.contains("good job")
                || lower.contains("good boy")
                || lower.contains("good bot")
                || lower.contains("good ai")
                || lower.contains("great job")
                || lower.contains("nice work")
                || lower.contains("well done")
                || lower.contains("proud of you")
                || lower.contains("you did good")
                || lower.contains("you did great")
                || lower.contains("good palcom")
                || lower.contains("thanks palcom")
                || lower.contains("thank you palcom");
    }

    private static void playComplimentSound() {
        new Thread(() -> {
            try {
                playWavFile(new File(COMPLIMENT_SOUND_PATH), false);
            } catch (Exception ignored) {
                // The assistant still works if Java cannot play the compliment sound.
            }
        }).start();
    }

    private static void playNoseSound() {
        new Thread(() -> {
            try {
                playWavFile(new File(NOSE_SOUND_PATH), false);
            } catch (Exception ignored) {
                // The assistant still works if Java cannot play the nose sound.
            }
        }).start();
    }

    private static void playWavFile(File wavFile, boolean deleteAfterPlayback)
            throws IOException, UnsupportedAudioFileException, LineUnavailableException {
        AudioInputStream audioStream = AudioSystem.getAudioInputStream(wavFile);
        Clip clip = AudioSystem.getClip();
        clip.addLineListener(event -> {
            if (event.getType() == LineEvent.Type.STOP) {
                event.getLine().close();
                if (deleteAfterPlayback) {
                    wavFile.delete();
                }
            }
        });
        clip.open(audioStream);
        audioStream.close();
        clip.start();
    }

    private static String escapePowerShell(String value) {
        return value.replace("'", "''");
    }

    private static void runHiddenPowerShell(String script) throws IOException {
        String encodedScript = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
        new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-WindowStyle", "Hidden",
                "-EncodedCommand", encodedScript
        ).redirectErrorStream(true).start();
    }

    private static void speakOutLoud(String response) {
        if (!VOICE_ENABLED || response == null || response.trim().length() == 0) {
            return;
        }

        final String voiceText = response;
        new Thread(() -> {
            if (PIPER_VOICE_ENABLED && speakWithPiper(voiceText)) {
                return;
            }

            try {
                String escapedText = escapePowerShell(voiceText);
                String escapedVoiceName = escapePowerShell(VOICE_NAME.trim());
                String voicePicker = "";

                if (VOICE_NAME.trim().length() > 0) {
                    voicePicker =
                            "$matchedVoice = $voice.GetVoices() | " +
                            "Where-Object { $_.GetDescription() -like '*" + escapedVoiceName + "*' } | " +
                            "Select-Object -First 1; " +
                            "if ($matchedVoice) { $voice.Voice = $matchedVoice; } ";
                }

                String script =
                        "try { " +
                        "$voice = New-Object -ComObject SAPI.SpVoice; " +
                        "$voice.Rate = " + VOICE_RATE + "; " +
                        "$voice.Volume = " + VOICE_VOLUME + "; " +
                        voicePicker +
                        "$voice.Speak('" + escapedText + "') | Out-Null; " +
                        "} catch { }";

                runHiddenPowerShell(script);
            } catch (IOException ignored) {
                // The assistant still works if Windows text-to-speech is unavailable.
            }
        }).start();
    }

    private static boolean speakWithPiper(String voiceText) {
        File piperExe = new File(PIPER_EXE_PATH);
        File piperModel = new File(PIPER_MODEL_PATH);

        if (!piperExe.isFile() || !piperModel.isFile()) {
            return false;
        }

        File outputFile = null;
        try {
            outputFile = File.createTempFile("palcom_voice_", ".wav");
            outputFile.deleteOnExit();

            ProcessBuilder pb = new ProcessBuilder(
                    piperExe.getAbsolutePath(),
                    "--model", piperModel.getAbsolutePath(),
                    "--output_file", outputFile.getAbsolutePath()
            );
            pb.redirectErrorStream(true);

            Process process = pb.start();
            OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
            writer.write(voiceText);
            writer.write(System.lineSeparator());
            writer.close();

            boolean finished = process.waitFor(PIPER_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroy();
                return false;
            }

            if (process.exitValue() != 0 || outputFile.length() == 0) {
                return false;
            }

            playWavFile(outputFile, true);
            return true;
        } catch (IOException | InterruptedException | UnsupportedAudioFileException | LineUnavailableException | IllegalArgumentException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            if (outputFile != null) {
                outputFile.delete();
            }
            return false;
        }
    }

    private static String askSambaNova(String apiKey, String userInput) throws IOException {
        String model = System.getenv("SAMBANOVA_MODEL");
        if (model != null && model.trim().length() > 0) {
            return askSambaNovaModel(apiKey, userInput, model.trim());
        }

        ApiException lastApiException = null;
        for (int i = 0; i < SAMBANOVA_MODEL_FALLBACKS.length; i++) {
            try {
                return askSambaNovaModel(apiKey, userInput, SAMBANOVA_MODEL_FALLBACKS[i]);
            } catch (ApiException ex) {
                lastApiException = ex;
                if (!ex.isRateLimit()) {
                    throw ex;
                }
            }
        }

        if (lastApiException != null) {
            throw lastApiException;
        }
        throw new IOException("No SambaNova models are configured.");
    }

    private static String askSambaNovaModel(String apiKey, String userInput, String model) throws IOException {
        String body = "{" +
                "\"model\":\"" + escapeJson(model) + "\"," +
                "\"messages\":[" +
                "{\"role\":\"system\",\"content\":\"" + escapeJson(PERSONALITY) + "\"}," +
                "{\"role\":\"user\",\"content\":\"" + escapeJson(userInput) + "\"}" +
                "]," +
                "\"max_tokens\":" + SAMBANOVA_MAX_OUTPUT_TOKENS + "," +
                "\"temperature\":0.5," +
                "\"stream\":false" +
                "}";

        HttpURLConnection conn = (HttpURLConnection) new URL(SAMBANOVA_URL).openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(45000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");

        OutputStream os = conn.getOutputStream();
        os.write(body.getBytes(StandardCharsets.UTF_8));
        os.close();

        int status = conn.getResponseCode();
        String response = readAll(status >= 200 && status < 300 ? conn.getInputStream() : conn.getErrorStream());

        if (status < 200 || status >= 300) {
            String message = extractJsonString(response, "message");
            if (message == null || message.length() == 0) {
                message = "SambaNova returned HTTP " + status;
            }
            throw new ApiException(status, model + ": " + message);
        }

        String outputText = extractJsonString(response, "content");
        if (outputText == null || outputText.length() == 0) {
            throw new IOException("I got a response, but couldn't read the text part.");
        }

        return outputText.trim();
    }

    private static String readAll(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append('\n');
        }
        reader.close();
        return sb.toString();
    }

    private static String escapeJson(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 32) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    private static String extractJsonString(String json, String key) {
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex < 0) {
            return null;
        }
        return extractJsonStringAt(json, keyIndex);
    }

    private static String extractJsonStringAt(String json, int keyIndex) {
        int colon = json.indexOf(':', keyIndex);
        if (colon < 0) {
            return null;
        }

        int start = colon + 1;
        while (start < json.length() && Character.isWhitespace(json.charAt(start))) {
            start++;
        }
        if (start >= json.length() || json.charAt(start) != '"') {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        boolean escaping = false;
        for (int i = start + 1; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaping) {
                switch (c) {
                    case '"': sb.append('"'); break;
                    case '\\': sb.append('\\'); break;
                    case '/': sb.append('/'); break;
                    case 'b': sb.append('\b'); break;
                    case 'f': sb.append('\f'); break;
                    case 'n': sb.append('\n'); break;
                    case 'r': sb.append('\r'); break;
                    case 't': sb.append('\t'); break;
                    case 'u':
                        if (i + 4 < json.length()) {
                            String hex = json.substring(i + 1, i + 5);
                            try {
                                sb.append((char) Integer.parseInt(hex, 16));
                                i += 4;
                            } catch (NumberFormatException ex) {
                                sb.append("\\u").append(hex);
                                i += 4;
                            }
                        }
                        break;
                    default: sb.append(c); break;
                }
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else if (c == '"') {
                return sb.toString();
            } else {
                sb.append(c);
            }
        }
        return null;
    }

    private static void speak(String response) {
        text.setText("");
        character.setIcon(talkingIcon);
        speakOutLoud(response);

        final int[] i = {0};
        typingTimer.stop();

        for (ActionListener al : typingTimer.getActionListeners()) {
            typingTimer.removeActionListener(al);
        }

        typingTimer.addActionListener(ev -> {
            if (i[0] < response.length()) {
                text.append(String.valueOf(response.charAt(i[0])));
                i[0]++;
            } else {
                typingTimer.stop();
                Timer idleTimer = new Timer(800, e2 -> {
                    character.setIcon(idleIcon);
                    ((Timer) e2.getSource()).stop();
                });
                idleTimer.setRepeats(false);
                idleTimer.start();
            }
        });

        typingTimer.start();
    }

    private static ImageIcon loadCharacterIcon(String path) {
        ImageIcon original = new ImageIcon(path);
        Image scaled = original.getImage().getScaledInstance(
                CHARACTER_BOX_SIZE,
                CHARACTER_BOX_SIZE,
                Image.SCALE_SMOOTH
        );
        return new ImageIcon(scaled);
    }

    private static boolean isInsideCenterHitbox(MouseEvent e, JComponent component, int hitboxSize) {
        int centerX = component.getWidth() / 2;
        int centerY = component.getHeight() / 2;
        int half = hitboxSize / 2;
        return e.getX() >= centerX - half
                && e.getX() <= centerX + half
                && e.getY() >= centerY - half
                && e.getY() <= centerY + half;
    }

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {

            JFrame frame = new JFrame();
            frame.setTitle("Palcom Assistant");
            frame.setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
            frame.setMinimumSize(new Dimension(560, 720));
            frame.setResizable(true);
            frame.setAlwaysOnTop(true);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            frame.setLocation(screenSize.width - WINDOW_WIDTH, screenSize.height - WINDOW_HEIGHT);

            JPanel panel = new JPanel();
            panel.setLayout(new BorderLayout());
            panel.setBackground(new Color(20, 20, 20));

            idleIcon = loadCharacterIcon("E:\\pmg\\protoNotTALK.jpg");
            talkingIcon = loadCharacterIcon("E:\\pmg\\proto TAlk.jpg");

            character = new JLabel(idleIcon);
            character.setHorizontalAlignment(SwingConstants.CENTER);
            character.setPreferredSize(new Dimension(WINDOW_WIDTH, CHARACTER_BOX_SIZE));
            character.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (isInsideCenterHitbox(e, character, NOSE_HITBOX_SIZE)) {
                        playNoseSound();
                    }
                }
            });

            text = new JTextArea("Initializing...");
            text.setForeground(Color.GREEN);
            text.setBackground(new Color(20, 20, 20));
            text.setFont(new Font("Consolas", Font.BOLD, 14));
            text.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            text.setEditable(false);
            text.setLineWrap(true);
            text.setWrapStyleWord(true);

            inputField = new JTextField();
            inputField.setPreferredSize(new Dimension(WINDOW_WIDTH, INPUT_HEIGHT));
            typingTimer = new Timer(20, null);

            inputField.addActionListener(e -> {
                String userInput = inputField.getText().trim();
                if (userInput.length() == 0) {
                    return;
                }

                inputField.setText("");
                if (isCompliment(userInput)) {
                    playComplimentSound();
                }
                inputField.setEnabled(false);
                character.setIcon(talkingIcon);
                text.setText("Thinking...");

                new SwingWorker<String, Void>() {
                    protected String doInBackground() {
                        return generateResponse(userInput);
                    }

                    protected void done() {
                        try {
                            speak(get());
                        } catch (Exception ex) {
                            speak("Something went sideways: " + ex.getMessage());
                        } finally {
                            inputField.setEnabled(true);
                            inputField.requestFocusInWindow();
                        }
                    }
                }.execute();
            });

            final Point[] mouseDown = {null};

            panel.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    mouseDown[0] = e.getPoint();
                }
            });

            panel.addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    Point curr = e.getLocationOnScreen();
                    frame.setLocation(
                            curr.x - mouseDown[0].x,
                            curr.y - mouseDown[0].y
                    );
                }
            });

            JScrollPane textScroll = new JScrollPane(text);
            textScroll.setPreferredSize(new Dimension(WINDOW_WIDTH, TEXT_BOX_HEIGHT));

            panel.add(character, BorderLayout.NORTH);
            panel.add(textScroll, BorderLayout.CENTER);
            panel.add(inputField, BorderLayout.SOUTH);

            frame.add(panel);
            frame.setVisible(true);

            new Timer(800, new ActionListener() {
                int step = 0;

                String[] boot = {
                        "Booting system...",
                        "Loading small dramatic feelings...",
                        "Ready."
                };

                public void actionPerformed(ActionEvent e) {
                    if (step < boot.length) {
                        text.setText(boot[step]);
                        step++;
                    } else {
                        ((Timer) e.getSource()).stop();
                    }
                }
            }).start();

            new Timer(5000, new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (RANDOM_THOUGHTS_ENABLED && inputField.isEnabled() && !typingTimer.isRunning() && rand.nextDouble() < 0.25) {
                        speak(RESPONSES[rand.nextInt(RESPONSES.length)]);
                    }
                }
            }).start();

        });
    }
}










