import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

//TODO Settle grudges
// When turn begins, check the book of grudges and attack accordingly
// After that, follow some default strategy

//TODO Default strategy
// If there are empty nodes in range, expand defensively every turn
// If there are no empty nodes in range, fortify borders
// (that means moving soldiers from "safe" nodes to the edges of the empire)
// If 1. we are close to maxsoldiers or
// 2. we are reaching the turn counter 5
// (Counts up by 1 every round in which no soldiers are placed on new node.
// At 5, we don't receive new soldiers anymore) or
// 3. all our nodes are full:
// Do attack weakest node neighboring ours to gain space

/**
 * A helper class to read state and move files from a previous turn.
 */
class PreviousTurnData {
    public List<Integer> counts;
    public List<String> owners;

    public PreviousTurnData() {
        this.counts = new ArrayList<>();
        this.owners = new ArrayList<>();
    }
}


public class MyAgent {

    private int step;
    private String name;
    private List<Integer> counts;
    private List<String> owners;
    private int newSoldiers;
    private int maxSoldiers;
    private int visibilityRange;
    private Random random = new Random();
    private boolean mustOccupyThisTurn;

    public MyAgent(int step, String name) {
        this.step = step;
        this.name = name;
        this.counts = new ArrayList<>();
        this.owners = new ArrayList<>();
        this.visibilityRange = 0; // Default value
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
        // Set the boolean field based on the check.
        this.mustOccupyThisTurn = checkIfMustOccupyNode();
        System.out.println("Must move this turn: " + this.mustOccupyThisTurn);

        List<String> grudges = figureOutGrudges(this.step - 1);
        System.out.println("Grudges from last turn: " + grudges);

        List<String> moves = new ArrayList<>();

        //moves.add("<node index>,<number of soldiers>");

        return moves;
    }

    /**
     * Reads the agent's internal memory file, 'bookOfGrudges.txt',
     * and returns a list of its contents.
     *
     * @return A List of strings, where each string is a line from the file.
     */
    private List<String> readGrudges() {
        List<String> grudges = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(this.name + "/bookOfGrudges.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                grudges.add(line);
            }
        } catch (IOException e) {
            // file might not exist yet (first turn)
        }
        return grudges;
    }

    /**
     * Writes the given list of grudges to the agent's internal memory file.
     * This overwrites any previous contents of the file.
     *
     * @param grudges The list of strings to write to the file.
     */
    private void writeGrudges(List<String> grudges) {
        try (FileWriter writer = new FileWriter(this.name + "/bookOfGrudges.txt")) {
            for (String grudge : grudges) {
                writer.write(grudge);
                writer.write("\n");
            }
        } catch (IOException e) {
            System.err.println("An error occurred while writing to the grudges file: " + e.getMessage());
        }
    }

    /**
     * Calculates and returns a list of grudges from the previous turn.
     * A grudge is defined as a soldier loss on a node previously owned by the agent.
     *
     * @param previousStep The step number of the previous turn.
     * @return A list of strings, each representing a grudge in the format "enemy_id,soldiers_lost".
     */
    private List<String> figureOutGrudges(int previousStep) {
        List<String> grudgeList = new ArrayList<>();
        Map<String, Integer> grudges = new HashMap<>();

        // We can't figure out grudges on the first turn
        if (previousStep <= 0) {
            return grudgeList;
        }

        PreviousTurnData prevData = readPreviousTurnData(previousStep);
        Map<Integer, Integer> previousMoves = readPreviousMoves();

        // Iterate through each node on the ring
        for (int i = 0; i < this.owners.size(); i++) {
            // Check if the node was owned by us in the previous turn
            if (i < prevData.owners.size() && "Y".equals(prevData.owners.get(i))) {
                int previousSoldiers = prevData.counts.get(i);
                int movedSoldiers = previousMoves.getOrDefault(i, 0);
                int expectedSoldiers = previousSoldiers + movedSoldiers;

                // Check if we lost the node
                if (!"Y".equals(this.owners.get(i))) {
                    // Node was lost, so we lost all expected soldiers on it
                    int soldiersLost = expectedSoldiers;
                    String enemyId = this.owners.get(i); // The new owner is the culprit

                    if (!"U".equals(enemyId) && !"N".equals(enemyId)) {
                        grudges.put(enemyId, grudges.getOrDefault(enemyId, 0) + soldiersLost);
                    }
                }
                // Check if we lost soldiers on a node we still own
                else if (this.counts.get(i) < expectedSoldiers) {
                    int soldiersLost = expectedSoldiers - this.counts.get(i);
                    String enemyId = findAttackerNeighbor(prevData.owners, i, this.visibilityRange);
                    if (enemyId != null) {
                        grudges.put(enemyId, grudges.getOrDefault(enemyId, 0) + soldiersLost);
                    }
                }
            }
        }

        // Convert the map of grudges to the required string format
        for (Map.Entry<String, Integer> entry : grudges.entrySet()) {
            grudgeList.add(entry.getKey() + "," + entry.getValue());
        }

        return grudgeList;
    }

    /**
     * Reads the state file for the specified previous step.
     *
     * @param step The step number of the file to read.
     * @return A PreviousTurnData object containing the state.
     */
    private PreviousTurnData readPreviousTurnData(int step) {
        PreviousTurnData data = new PreviousTurnData();
        try (BufferedReader reader = new BufferedReader(new FileReader(this.name + "/" + step + ".txt"))) {
            data.counts = java.util.Arrays.stream(reader.readLine().split(","))
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
            data.owners = java.util.Arrays.stream(reader.readLine().split(","))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            // Log or handle the error appropriately
        }
        return data;
    }

    /**
     * Reads the move file from the previous turn and parses the moves.
     *
     * @return A map of node indices to soldier changes.
     */
    private Map<Integer, Integer> readPreviousMoves() {
        Map<Integer, Integer> moves = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(this.name + "/move.txt"))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 2) {
                    int node = Integer.parseInt(parts[0].trim());
                    int soldiers = Integer.parseInt(parts[1].trim());
                    moves.put(node, soldiers);
                }
            }
        } catch (IOException e) {
            // This is expected on the first turn or if the file is empty.
        }
        return moves;
    }

    /**
     * Finds a random enemy from a list of owners within a given visibility range.
     *
     * @param previousOwners A list of all owners from the previous turn.
     * @param nodeIndex The index of the node that was attacked.
     * @param visibilityRange The agent's visibility range.
     * @return The ID of a randomly selected enemy, or null if no enemies are found.
     */
    private String findAttackerNeighbor(List<String> previousOwners, int nodeIndex, int visibilityRange) {
        List<String> enemies = new ArrayList<>();
        int ringSize = previousOwners.size();

        // Construct the list of owners within visibility range
        for (int j = -visibilityRange; j <= visibilityRange; j++) {
            int index = (nodeIndex + j + ringSize) % ringSize;
            String owner = previousOwners.get(index);
            if (owner.length() == 1 && Character.isLowerCase(owner.charAt(0))) {
                enemies.add(owner);
            }
        }

        if (enemies.isEmpty()) {
            return null;
        } else {
            return enemies.get(random.nextInt(enemies.size()));
        }
    }

    /**
     * Determines the visibility range from the initial state file.
     *
     * @return The determined visibility range.
     * @throws IOException if the file cannot be read.
     */
    private int determineVisRange() throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(this.name + "/0.txt"))) {
            String line = reader.readLine();
            if (line == null) {
                return 0;
            }
            String[] parts = line.split(",");
            int firstInvisibleIndex = -1;

            // The visibility range is the number of nodes visible in one direction
            // from the agent's starting node (index 0). We find the index
            // of the first -1 value, and the range is that index minus one.
            for (int i = 1; i < parts.length; i++) {
                if (Integer.parseInt(parts[i].trim()) == -1) {
                    firstInvisibleIndex = i;
                    break;
                }
            }
            if (firstInvisibleIndex != -1) {
                return firstInvisibleIndex - 1;
            } else {
                // The whole ring is visible. Return half the ring size.
                return parts.length / 2;
            }
        }
    }

    /**
     * Checks if the agent has failed to occupy a new node in the last four turns
     * by comparing the owner strings from previous turns.
     *
     * @return True if the agent has not expanded for four turns, false otherwise.
     */
    private boolean checkIfMustOccupyNode() {
        if (this.step < 5) {
            return false;
        }

        // Check for expansion in the last 4 turns
        for (int i = 1; i <= 4; i++) {
            List<String> currentOwners = getOwnersAtStep(this.step - i + 1);
            List<String> previousOwners = getOwnersAtStep(this.step - i);

            if (currentOwners.isEmpty() || previousOwners.isEmpty()) {
                continue; // Can't compare, so skip
            }

            // Iterate through each node and check for a new acquisition
            for (int j = 0; j < currentOwners.size(); j++) {
                if ("Y".equals(currentOwners.get(j)) && !"Y".equals(previousOwners.get(j))) {
                    // Found a new node, so we don't need to move this turn
                    return false;
                }
            }
        }

        return true; // No expansion found in the last 4 turns
    }

    /**
     * Helper method to get the list of owners at a specific step from the state file.
     *
     * @param step The step number to check.
     * @return A List of strings representing node ownership, or an empty list if the file cannot be read.
     */
    private List<String> getOwnersAtStep(int step) {
        List<String> owners = new ArrayList<>();
        if (step < 0) {
            return owners;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(this.name + "/" + step + ".txt"))) {
            // Skip the first line (soldier counts)
            reader.readLine();
            String ownersLine = reader.readLine();
            if (ownersLine != null) {
                owners = java.util.Arrays.asList(ownersLine.split(","));
            }
        } catch (IOException e) {
            System.err.println("Error reading state file for step " + step + ": " + e.getMessage());
        }
        return owners;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java MyAgent <step_number> <agent_name>");
            return;
        }

        int stepNumber = Integer.parseInt(args[0]);
        String agentName = args[1];

        MyAgent me = new MyAgent(stepNumber, agentName);
        try {
            me.visibilityRange = me.determineVisRange();
        } catch (IOException e) {
            System.err.println("Could not determine visibility range: " + e.getMessage());
        }

        me.readState();
        me.updateState();
    }
}
