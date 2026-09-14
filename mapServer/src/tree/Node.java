package tree;

import data.Data;
import java.io.Serializable;

abstract class Node implements Serializable {
    private static int idNodeCount = 0;
    private int idNode;
    private int beginExampleIndex;
    private int endExampleIndex;
    private double variance;

    Node(Data trainingSet, int beginExampleIndex, int endExampleIndex) {
        this.idNode = idNodeCount++;
        this.beginExampleIndex = beginExampleIndex;
        this.endExampleIndex = endExampleIndex;

        double sum = 0;
        double sumOfSquares = 0;
        for (int i = beginExampleIndex; i <= endExampleIndex; i++) {
            double classValue = trainingSet.getClassValue(i);
            sum += classValue;
            sumOfSquares += classValue * classValue;
        }

        int numberOfExamples = endExampleIndex - beginExampleIndex + 1;
        variance = sumOfSquares - (sum * sum) / numberOfExamples;
    }

    int getIdNode() {
        return idNode;
    }

    int getBeginExampleIndex() {
        return beginExampleIndex;
    }

    int getEndExampleIndex() {
        return endExampleIndex;
    }

    double getVariance() {
        return variance;
    }

    abstract int getNumberOfChildren();

    public String toString() {
        return "Nodo: [Examples:" + beginExampleIndex + "-" + endExampleIndex
            + "] variance:" + variance;
    }
}
