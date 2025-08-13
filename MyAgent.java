import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class MyAgent {

    private int step;
    private String name;
    private List<Integer> counts;
    private List<String> owners;
    private int newSoldiers;
    private int maxSoldiers;

    public MyAgent(int step, String name) {
        this.step = step;
        this.name = name;
        this.counts = new ArrayList<>();
        this.owners = new ArrayList<>();
    }

    /**
     * Reads the current state of the game from the agent's state file.
     * The file is expected to be located at <agent_name>/<step_number>.txt.
     */
    private void readState() {
        try (BufferedReader reader = new BufferedReader(new FileReader(this.name + "/" + this.step + ".txt"))) {
            String line;

            // Read the first line for soldier counts
            line = reader.readLine();
            String[] countParts = line.split(",");
            for (String part : countParts) {
                // Parse and add to the counts list
                counts.add(Integer.parseInt(part));
            }

            // Read the second line for node ownership
            line = reader.readLine();
            String[] ownerParts = line.split(",");
            for (String part : ownerParts) {
                // Add ownership strings to the owners list
                owners.add(part);
            }

            // Read the third line for the number of new soldiers
            line = reader.readLine();
            this.newSoldiers = Integer.parseInt(line);

            // Read the fourth line for the maximum soldier count per node
            line = reader.readLine();
            this.maxSoldiers = Integer.parseInt(line);

        } catch (IOException e) {
            System.err.println("Error reading the state file: " + e.getMessage());
        }
    }

    private void updateState() {
        List<String> moves = calculateMoves();

        try (FileWriter writer = new FileWriter(this.name + "/move.txt")) {
            // Write each move to a new line in the file
            for (String move : moves) {
                writer.write(move);
                writer.write("\n"); // Each move on a new line
            }
            // The file is automatically closed by the try-with-resources block
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the move file: " + e.getMessage());
        }
    }

    private List<String> calculateMoves() {
        List<String> moves = new ArrayList<>();

        //moves.add("<node index>,<number of soldiers>");

        return moves;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java Agent <step_number> <agent_name>");
            return;
        }

        int stepNumber = Integer.parseInt(args[0]);
        String agentName = args[1];

        MyAgent me = new MyAgent(stepNumber, agentName);
        me.readState();
        me.updateState();
    }

}

