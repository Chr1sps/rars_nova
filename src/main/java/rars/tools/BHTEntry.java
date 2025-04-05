
package rars.tools;

/**
 * Represents a single entry of the Branch History Table.
 * <p>
 * The entry holds the information about former branch predictions and outcomes.
 * The number of past branch outcomes can be configured and is called the
 * history.
 * The semantics of the history of size <i>n</i> is as follows.
 * The entry will change its prediction, if it mispredicts the branch <i>n</i>
 * times in series.
 * The prediction of the entry can be obtained by the
 * {@link BHTEntry#getPrediction()} method.
 * Feedback of taken or not taken branches is provided to the entry via the
 * {@link BHTEntry#updatePrediction(boolean)} method.
 * This causes the history and the prediction to be updated.
 * <p>
 * Additionally the entry keeps track about how many times the prediction was
 * correct or incorrect.
 * The statistics can be obtained by the methods
 * {@link BHTEntry#getStatsPredCorrect()},
 * {@link BHTEntry#getStatsPredIncorrect()} and
 * {@link BHTEntry#getStatsPredPrecision()}.
 *
 * @author ingo.kofler@itec.uni-klu.ac.at
 */
public final class BHTEntry {

    /**
     * the history of the BHT entry. Each boolean value signals if the branch was
     * taken or not. The value at index n-1 represents the most recent branch
     * outcome.
     */
    private final boolean[] myHistory;

    private boolean currentPrediction;
    private int incorrectPredictionsCount;
    private int correctPredictionsCount;

    /**
     * Constructs a BHT entry with a given history size.
     * <p>
     * The size of the history can only be set via the constructor and cannot be
     * changed afterwards.
     *
     * @param historySize
     *     number of past branch outcomes to remember
     * @param initVal
     *     the initial value of the entry (take or do not take)
     */
    public BHTEntry(final int historySize, final boolean initVal) {
        this.currentPrediction = initVal;
        this.myHistory = new boolean[historySize];

        for (int i = 0; i < historySize; i++) {
            this.myHistory[i] = initVal;
        }
        this.correctPredictionsCount = 0;
        this.incorrectPredictionsCount = 0;
    }

    /**
     * Returns the branch prediction based on the history.
     *
     * @return true if prediction is to take the branch, false otherwise
     */
    public boolean getPrediction() {
        return this.currentPrediction;
    }

    /**
     * Updates the entry's history and prediction.
     * This method provides feedback for a prediction.
     * The history and the statistics are updated accordingly.
     * Based on the updated history a new prediction is calculated
     *
     * @param branchTaken
     *     signals if the branch was taken (true) or not (false)
     */
    public void updatePrediction(final boolean branchTaken) {

        // update history
        for (int i = 0; i < this.myHistory.length - 1; i++) {
            this.myHistory[i] = this.myHistory[i + 1];
        }
        this.myHistory[this.myHistory.length - 1] = branchTaken;

        // if the prediction was correct, update stats and keep prediction
        if (branchTaken == this.currentPrediction) {
            this.correctPredictionsCount++;
        } else {
            this.incorrectPredictionsCount++;

            // check if the prediction should change
            boolean changePrediction = true;

            for (final boolean taken : this.myHistory) {
                if (taken != branchTaken) {
                    changePrediction = false;
                    break;
                }
            }

            if (changePrediction) {
                this.currentPrediction = !this.currentPrediction;
            }

        }
    }

    /**
     * Get the absolute number of mispredictions.
     *
     * @return number of incorrect predictions (mispredictions)
     */
    public int getStatsPredIncorrect() {
        return this.incorrectPredictionsCount;
    }

    /**
     * Get the absolute number of correct predictions.
     *
     * @return number of correct predictions
     */
    public int getStatsPredCorrect() {
        return this.correctPredictionsCount;
    }

    /**
     * Get the percentage of correct predictions.
     *
     * @return the percentage of correct predictions
     */
    public double getStatsPredPrecision() {
        final int sum = this.incorrectPredictionsCount + this.correctPredictionsCount;
        return (sum == 0) ? 0 : this.correctPredictionsCount * 100.0 / sum;
    }

    /**
     * Builds a string representation of the BHT entry's history.
     * The history is a sequence of flags that signal if the branch was taken (T) or
     * not taken (NT).
     *
     * @return a string representation of the BHT entry's history
     */
    public String getHistoryAsStr() {
        final StringBuilder result = new StringBuilder();

        for (int i = 0; i < this.myHistory.length; i++) {
            if (i > 0) {
                result.append(", ");
            }
            result.append(this.myHistory[i] ? "T" : "NT");
        }
        return result.toString();
    }

    /**
     * Returns a string representation of the BHT entry's current prediction.
     * The prediction can be either to TAKE or do NOT TAKE the branch.
     *
     * @return a string representation of the BHT entry's current prediction
     */
    public String getPredictionAsStr() {
        return this.currentPrediction ? "TAKE" : "NOT TAKE";
    }
}
