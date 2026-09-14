package tree;

import data.Attribute;
import data.Data;
import data.DiscreteAttribute;

class DiscreteNode extends SplitNode {
    public DiscreteNode(
        Data trainingSet,
        int beginExampleIndex,
        int endExampleIndex,
        DiscreteAttribute attribute
    ) {
        super(trainingSet, beginExampleIndex, endExampleIndex, attribute);
    }

    void setSplitInfo(
        Data trainingSet,
        int beginExampleIndex,
        int endExampleIndex,
        Attribute attribute
    ) {
        int child = 0;
        int splitBeginIndex = beginExampleIndex;
        Object splitValue = trainingSet.getExplanatoryValue(
            beginExampleIndex,
            attribute.getIndex()
        );

        for (int i = beginExampleIndex + 1; i <= endExampleIndex; i++) {
            Object currentValue = trainingSet.getExplanatoryValue(i, attribute.getIndex());
            if (!currentValue.equals(splitValue)) {
                mapSplit.add(new SplitInfo(
                    splitValue,
                    splitBeginIndex,
                    i - 1,
                    child
                ));
                child++;
                splitBeginIndex = i;
                splitValue = currentValue;
            }
        }

        mapSplit.add(new SplitInfo(
            splitValue,
            splitBeginIndex,
            endExampleIndex,
            child
        ));
    }

    int testCondition(Object value) {
        for (int i = 0; i < mapSplit.size(); i++) {
            if (mapSplit.get(i).getSplitValue().equals(value)) {
                return i;
            }
        }
        return -1;
    }

    public String toString() {
        return "DISCRETE " + super.toString();
    }
}
