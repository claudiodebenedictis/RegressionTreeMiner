package tree;

import Server.UnknownValueException;
import data.Attribute;
import data.ContinuousAttribute;
import data.Data;
import data.DiscreteAttribute;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.TreeSet;

public class RegressionTree implements Serializable {
    private static final long serialVersionUID = 3910770211665569798L;

    Node root;
    RegressionTree childTree[];

    RegressionTree() {
    }

    public RegressionTree(Data trainingSet) {
        learnTree(
            trainingSet,
            0,
            trainingSet.getNumberOfExamples() - 1,
            trainingSet.getNumberOfExamples() * 10 / 100
        );
    }

    public void salva(String nomeFile) throws FileNotFoundException, IOException {
        try (
            FileOutputStream fileOutputStream = new FileOutputStream(nomeFile);
            ObjectOutputStream outputStream = new ObjectOutputStream(fileOutputStream)
        ) {
            outputStream.writeObject(this);
        }
    }

    public static RegressionTree carica(String nomeFile)
        throws FileNotFoundException, IOException, ClassNotFoundException {
        try (
            FileInputStream fileInputStream = new FileInputStream(nomeFile);
            ObjectInputStream inputStream = new ObjectInputStream(fileInputStream)
        ) {
            return (RegressionTree) inputStream.readObject();
        }
    }

    boolean isLeaf(
        Data trainingSet,
        int begin,
        int end,
        int numberOfExamplesPerLeaf
    ) {
        return end - begin + 1 <= numberOfExamplesPerLeaf;
    }

    private SplitNode determineBestSplitNode(Data trainingSet, int begin, int end) {
        TreeSet<SplitNode> ts = new TreeSet<SplitNode>();

        for (int i = 0; i < trainingSet.getNumberOfExplanatoryAttributes(); i++) {
            Attribute attribute = trainingSet.getExplanatoryAttribute(i);
            SplitNode currentNode;

            if (attribute instanceof DiscreteAttribute) {
                currentNode = new DiscreteNode(
                    trainingSet,
                    begin,
                    end,
                    (DiscreteAttribute) attribute
                );
            } else {
                currentNode = new ContinuousNode(
                    trainingSet,
                    begin,
                    end,
                    (ContinuousAttribute) attribute
                );
            }

            ts.add(currentNode);
        }

        SplitNode bestSplitNode = ts.first();
        trainingSet.sort(bestSplitNode.getAttribute(), begin, end);
        return bestSplitNode;
    }

    void learnTree(
        Data trainingSet,
        int begin,
        int end,
        int numberOfExamplesPerLeaf
    ) {
        if (isLeaf(trainingSet, begin, end, numberOfExamplesPerLeaf)) {
            root = new LeafNode(trainingSet, begin, end);
        } else {
            root = determineBestSplitNode(trainingSet, begin, end);

            if (root.getNumberOfChildren() > 1) {
                childTree = new RegressionTree[root.getNumberOfChildren()];
                SplitNode splitNode = (SplitNode) root;

                for (int i = 0; i < root.getNumberOfChildren(); i++) {
                    childTree[i] = new RegressionTree();
                    childTree[i].learnTree(
                        trainingSet,
                        splitNode.getSplitInfo(i).getBeginindex(),
                        splitNode.getSplitInfo(i).getEndIndex(),
                        numberOfExamplesPerLeaf
                    );
                }
            } else {
                root = new LeafNode(trainingSet, begin, end);
            }
        }
    }

    public Double predictClass(ObjectInputStream in, ObjectOutputStream out)
        throws IOException, ClassNotFoundException, UnknownValueException {
        if (root instanceof LeafNode) {
            return ((LeafNode) root).getPredictedClassValue();
        } else {
            out.writeObject("QUERY");
            out.writeObject(((SplitNode) root).formulateQuery());
            out.flush();
            int risp = (Integer) in.readObject();

            if (risp < 0 || risp >= root.getNumberOfChildren()) {
                throw new UnknownValueException(
                    "The answer should be an integer between 0 and "
                        + (root.getNumberOfChildren() - 1) + "!"
                );
            }

            return childTree[risp].predictClass(in, out);
        }
    }

    public void printTree() {
        System.out.println("********* TREE **********\n");
        System.out.println(toString());
        System.out.println("*************************\n");
    }

    public void printRules() {
        System.out.println("********* RULES **********\n");
        printRules("");
        System.out.println("*************************\n");
    }

    void printRules(String current) {
        if (root instanceof LeafNode) {
            System.out.println(
                current + " ==> Class=" + ((LeafNode) root).getPredictedClassValue()
            );
        } else {
            SplitNode splitNode = (SplitNode) root;

            for (int i = 0; i < childTree.length; i++) {
                SplitNode.SplitInfo splitInfo = splitNode.getSplitInfo(i);
                String condition = splitNode.getAttribute().getName()
                    + splitInfo.getComparator() + splitInfo.getSplitValue();
                String rule = current.length() == 0
                    ? condition
                    : current + " AND " + condition;
                childTree[i].printRules(rule);
            }
        }
    }

    public String toString() {
        String tree = root.toString() + "\n";

        if (!(root instanceof LeafNode)) {
            for (int i = 0; i < childTree.length; i++) {
                tree += childTree[i];
            }
        }

        return tree;
    }
}
