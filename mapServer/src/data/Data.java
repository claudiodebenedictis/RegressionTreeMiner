package data;

import database.Column;
import database.DatabaseConnectionException;
import database.DbAccess;
import database.EmptySetException;
import database.Example;
import database.TableData;
import database.TableSchema;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Data {
    private List<Example> data = new ArrayList<Example>();
    private int numberOfExamples;
    private List<Attribute> explanatorySet = new LinkedList<Attribute>();
    private ContinuousAttribute classAttribute;

    public Data(String tableName) throws TrainingDataException {
        DbAccess db = new DbAccess();

        try {
            db.initConnection();
            TableSchema tableSchema = new TableSchema(db, tableName);
            int numberOfAttributes = tableSchema.getNumberOfAttributes();

            if (numberOfAttributes == 0) {
                throw new TrainingDataException("Tabella inesistente");
            }
            if (numberOfAttributes < 2) {
                throw new TrainingDataException(
                    "La tabella deve contenere almeno due colonne"
                );
            }

            Column targetColumn = tableSchema.getColumn(numberOfAttributes - 1);
            if (!targetColumn.isNumber()) {
                throw new TrainingDataException(
                    "L'attributo target deve essere numerico"
                );
            }

            TableData tableData = new TableData(db);
            for (int attributeIndex = 0;
                 attributeIndex < numberOfAttributes - 1;
                 attributeIndex++) {
                Column column = tableSchema.getColumn(attributeIndex);

                if (column.isNumber()) {
                    explanatorySet.add(new ContinuousAttribute(
                        column.getColumnName(),
                        attributeIndex
                    ));
                } else {
                    Set<String> discreteValues = new TreeSet<String>();
                    for (Object value : tableData.getDistinctColumnValues(
                        tableName,
                        column
                    )) {
                        discreteValues.add((String) value);
                    }

                    explanatorySet.add(new DiscreteAttribute(
                        column.getColumnName(),
                        attributeIndex,
                        discreteValues
                    ));
                }
            }

            classAttribute = new ContinuousAttribute(
                targetColumn.getColumnName(),
                numberOfAttributes - 1
            );
            data.addAll(tableData.getTransazioni(tableName));
            numberOfExamples = data.size();
        } catch (DatabaseConnectionException exception) {
            throw new TrainingDataException(
                "Connessione al database fallita",
                exception
            );
        } catch (EmptySetException exception) {
            throw new TrainingDataException("Training set vuoto", exception);
        } catch (SQLException exception) {
            throw new TrainingDataException(
                "Errore durante l'accesso al database",
                exception
            );
        } finally {
            db.closeConnection();
        }
    }

    public int getNumberOfExamples() {
        return numberOfExamples;
    }

    public int getNumberOfExplanatoryAttributes() {
        return explanatorySet.size();
    }

    public Double getClassValue(int exampleIndex) {
        return (Double) data.get(exampleIndex).get(explanatorySet.size());
    }

    public Object getExplanatoryValue(int exampleIndex, int attributeIndex) {
        return data.get(exampleIndex).get(attributeIndex);
    }

    public Attribute getExplanatoryAttribute(int index) {
        return explanatorySet.get(index);
    }

    public ContinuousAttribute getClassAttribute() {
        return classAttribute;
    }

    public String toString() {
        String value = "";
        for (int i = 0; i < numberOfExamples; i++) {
            for (int j = 0; j < explanatorySet.size(); j++) {
                value += data.get(i).get(j) + ",";
            }
            value += data.get(i).get(explanatorySet.size()) + "\n";
        }
        return value;
    }

    public void sort(Attribute attribute, int beginExampleIndex, int endExampleIndex) {
        quicksort(attribute, beginExampleIndex, endExampleIndex);
    }

    private void swap(int i, int j) {
        Example temp = data.get(i);
        data.set(i, data.get(j));
        data.set(j, temp);
    }

    private int partition(DiscreteAttribute attribute, int inf, int sup) {
        int i = inf;
        int j = sup;
        int med = (inf + sup) / 2;
        String x = (String) getExplanatoryValue(med, attribute.getIndex());
        swap(inf, med);

        while (true) {
            while (i <= sup
                && ((String) getExplanatoryValue(i, attribute.getIndex())).compareTo(x) <= 0) {
                i++;
            }

            while (((String) getExplanatoryValue(j, attribute.getIndex())).compareTo(x) > 0) {
                j--;
            }

            if (i < j) {
                swap(i, j);
            } else {
                break;
            }
        }

        swap(inf, j);
        return j;
    }

    private int partition(ContinuousAttribute attribute, int inf, int sup) {
        int i = inf;
        int j = sup;
        int med = (inf + sup) / 2;
        Double x = (Double) getExplanatoryValue(med, attribute.getIndex());
        swap(inf, med);

        while (true) {
            while (i <= sup
                && ((Double) getExplanatoryValue(i, attribute.getIndex())).compareTo(x) <= 0) {
                i++;
            }

            while (((Double) getExplanatoryValue(j, attribute.getIndex())).compareTo(x) > 0) {
                j--;
            }

            if (i < j) {
                swap(i, j);
            } else {
                break;
            }
        }

        swap(inf, j);
        return j;
    }

    private void quicksort(Attribute attribute, int inf, int sup) {
        if (sup >= inf) {
            int pos;
            if (attribute instanceof DiscreteAttribute) {
                pos = partition((DiscreteAttribute) attribute, inf, sup);
            } else {
                pos = partition((ContinuousAttribute) attribute, inf, sup);
            }

            if ((pos - inf) < (sup - pos + 1)) {
                quicksort(attribute, inf, pos - 1);
                quicksort(attribute, pos + 1, sup);
            } else {
                quicksort(attribute, pos + 1, sup);
                quicksort(attribute, inf, pos - 1);
            }
        }
    }

    public static void main(String args[]) throws TrainingDataException {
        Data trainingSet = new Data("provaC");

        System.out.println("Number of examples: " + trainingSet.getNumberOfExamples());
        System.out.println(
            "Number of explanatory attributes: "
                + trainingSet.getNumberOfExplanatoryAttributes()
        );
        System.out.println("Target attribute: " + trainingSet.getClassAttribute().getName());
        System.out.println(trainingSet);

        for (int jColumn = 0;
             jColumn < trainingSet.getNumberOfExplanatoryAttributes();
             jColumn++) {
            System.out.println(
                "ORDER BY " + trainingSet.getExplanatoryAttribute(jColumn).getName()
            );
            trainingSet.sort(
                trainingSet.getExplanatoryAttribute(jColumn),
                0,
                trainingSet.getNumberOfExamples() - 1
            );
            System.out.println(trainingSet);
        }
    }
}
