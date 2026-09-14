package tree;

import data.Attribute;
import data.ContinuousAttribute;
import data.Data;
import java.util.ArrayList;
import java.util.List;

public class ContinuousNode extends SplitNode {
    public ContinuousNode(
        Data trainingSet,
        int beginExampleIndex,
        int endExampleIndex,
        ContinuousAttribute attribute
    ) {
        super(trainingSet, beginExampleIndex, endExampleIndex, attribute);
    }

    void setSplitInfo(
        Data trainingSet,
        int beginExampleIndex,
        int endExampleIndex,
        Attribute attribute
    ) {
        Double currentSplitValue = (Double) trainingSet.getExplanatoryValue(
            beginExampleIndex,
            attribute.getIndex()
        );
        double bestInfoVariance = 0;
        List<SplitInfo> bestMapSplit = null;

        for (int i = beginExampleIndex + 1; i <= endExampleIndex; i++) {
            Double value = (Double) trainingSet.getExplanatoryValue(
                i,
                attribute.getIndex()
            );
            if (value.doubleValue() != currentSplitValue.doubleValue()) {
                double localVariance = new LeafNode(
                    trainingSet,
                    beginExampleIndex,
                    i - 1
                ).getVariance();
                double candidateSplitVariance = localVariance;
                localVariance = new LeafNode(
                    trainingSet,
                    i,
                    endExampleIndex
                ).getVariance();
                candidateSplitVariance += localVariance;

                if (bestMapSplit == null) {
                    bestMapSplit = new ArrayList<SplitInfo>();
                    bestMapSplit.add(new SplitInfo(
                        currentSplitValue,
                        beginExampleIndex,
                        i - 1,
                        0,
                        "<="
                    ));
                    bestMapSplit.add(new SplitInfo(
                        currentSplitValue,
                        i,
                        endExampleIndex,
                        1,
                        ">"
                    ));
                    bestInfoVariance = candidateSplitVariance;
                } else if (candidateSplitVariance < bestInfoVariance) {
                    bestInfoVariance = candidateSplitVariance;
                    bestMapSplit.set(0, new SplitInfo(
                        currentSplitValue,
                        beginExampleIndex,
                        i - 1,
                        0,
                        "<="
                    ));
                    bestMapSplit.set(1, new SplitInfo(
                        currentSplitValue,
                        i,
                        endExampleIndex,
                        1,
                        ">"
                    ));
                }
                currentSplitValue = value;
            }
        }

        mapSplit = bestMapSplit;

        if (mapSplit.get(1).beginIndex == mapSplit.get(1).getEndIndex()) {
            mapSplit.remove(1);
        }
    }

    int testCondition(Object value) {
        Double continuousValue = (Double) value;
        Double splitValue = (Double) mapSplit.get(0).getSplitValue();

        if (continuousValue.compareTo(splitValue) <= 0) {
            return 0;
        }
        return 1;
    }

    public String toString() {
        return "CONTINUOUS " + super.toString();
    }
}
