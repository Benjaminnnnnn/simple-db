package simpledb;

import java.io.Serializable;
import java.util.*;

/**
 * TupleDesc describes the schema of a tuple.
 */
public class TupleDesc implements Serializable {

    /**
     * A help class to facilitate organizing the information of each field
     * */
    public static class TDItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * The type of the field
         * */
        public final Type fieldType;

        /**
         * The name of the field
         * */
        public final String fieldName;

        public TDItem(Type t, String n) {
            this.fieldName = n;
            this.fieldType = t;
        }

        public String toString() {
            return fieldName + "(" + fieldType + ")";
        }
    }

    /**
     * @return
     *        An iterator which iterates over all the field TDItems
     *        that are included in this TupleDesc
     * */
    public Iterator<TDItem> iterator() {
        // some code goes here
        return this.iterator();
    }

    private static final long serialVersionUID = 1L;

    public List<TDItem> itemAr;
    private int size = 0;
    /**
     * Create a new TupleDesc with typeAr.length fields with fields of the
     * specified types, with associated named fields.
     *
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     * @param fieldAr
     *            array specifying the names of the fields. Note that names may
     *            be null.
     */
    public TupleDesc(Type[] typeAr, String[] fieldAr) {
        // some code goes here
        // typeAr: [String, Int]
        // fieldArr: ["age", 22]
        itemAr = new ArrayList<>();
        for (int i = 0; i < typeAr.length; i++) {
            itemAr.add(new TDItem(typeAr[i], fieldAr[i]));
//            size = typeAr.length;
            size += typeAr[i].getLen();
        }
    }

    /**
     * Constructor. Create a new tuple desc with typeAr.length fields with
     * fields of the specified types, with anonymous (unnamed) fields.
     *
     * @param typeAr
     *            array specifying the number of and types of fields in this
     *            TupleDesc. It must contain at least one entry.
     */
    public TupleDesc(Type[] typeAr) {
        // some code goes here
        itemAr = new ArrayList<>();
        for (int i = 0; i < typeAr.length; i++) {
            itemAr.add(new TDItem(typeAr[i], ""));
            size += typeAr[i].getLen();
        }
    }

    public TupleDesc() {
        itemAr = new ArrayList<>();
    }

    /**
     * @return the number of fields in this TupleDesc
     */
    public int numFields() {
        // some code goes here
        return this.itemAr.size();
    }

    /**
     * Gets the (possibly null) field name of the ith field of this TupleDesc.
     *
     * @param i
     *            index of the field name to return. It must be a valid index.
     * @return the name of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public String getFieldName(int i) throws NoSuchElementException {
        // some code goes here
        if (i < 0 || i >= this.itemAr.size()) {
            throw new NoSuchElementException("The index " + i + " is not valid");
        }
        return this.itemAr.get(i).fieldName;
    }

    /**
     * Gets the type of the ith field of this TupleDesc.
     *
     * @param i
     *            The index of the field to get the type of. It must be a valid
     *            index.
     * @return the type of the ith field
     * @throws NoSuchElementException
     *             if i is not a valid field reference.
     */
    public Type getFieldType(int i) throws NoSuchElementException {
        // some code goes here
        if (i < 0 || i >= this.itemAr.size()) {
            throw new NoSuchElementException("The index " + i + " is not valid");
        }
        return this.itemAr.get(i).fieldType;
    }

    /**
     * Find the index of the field with a given name.
     *
     * @param name
     *            name of the field.
     * @return the index of the field that is first to have the given name.
     * @throws NoSuchElementException
     *             if no field with a matching name is found.
     */
    public int fieldNameToIndex(String name) throws NoSuchElementException {
        // some code goes here
        int fieldIndex = -1;
        for (int i = 0; i < this.itemAr.size(); i++) {
            if (this.itemAr.get(i).fieldName.equals(name)) {
                fieldIndex = i;
                break;
            }
        }
        if (fieldIndex == -1) {
            throw new NoSuchElementException(name + "is not valid");
        }
        return fieldIndex;
    }

    /**
     * @return The size (in bytes) of tuples corresponding to this TupleDesc.
     *         Note that tuples from a given TupleDesc are of a fixed size.
     */
    public int getSize() {
        // some code goes here
        return this.size;
    }

    /**
     * Merge two TupleDescs into one, with td1.numFields + td2.numFields fields,
     * with the first td1.numFields coming from td1 and the remaining from td2.
     *
     * @param td1
     *            The TupleDesc with the first fields of the new TupleDesc
     * @param td2
     *            The TupleDesc with the last fields of the TupleDesc
     * @return the new TupleDesc
     */
    public static TupleDesc merge(TupleDesc td1, TupleDesc td2) {
        // some code goes here
        TupleDesc td3 = new TupleDesc();
        td3.add(td1.itemAr);
        td3.add(td2.itemAr);
        return td3;
    }

    public void add(List<TDItem> arr) {
        for (TDItem item : arr) {
            this.itemAr.add(item);
            this.size += item.fieldType.getLen();
        }
    }

    /**
     * Compares the specified object with this TupleDesc for equality. Two
     * TupleDescs are considered equal if they have the same number of items
     * and if the i-th type in this TupleDesc is equal to the i-th type in o
     * for every i.
     *
     * @param o
     *            the Object to be compared for equality with this TupleDesc.
     * @return true if the object is equal to this TupleDesc.
     */

    public boolean equals(Object o) {
        // some code goes here
        if (!(o instanceof TupleDesc)) {
            return false;
        }
        TupleDesc td2 = (TupleDesc) o;
        if (this.size != td2.size || this.itemAr.size() != td2.itemAr.size()) {
            return false;
        }
        for (int i = 0; i < this.itemAr.size(); i++) {
            TDItem item1 = this.itemAr.get(i);
            TDItem item2 = td2.itemAr.get(i);
            if (!item1.fieldType.equals(item2.fieldType)) {
                return false;
            }
        }
        return true;
    }

    public int hashCode() {
        // If you want to use TupleDesc as keys for HashMap, implement this so
        // that equal objects have equals hashCode() results
        throw new UnsupportedOperationException("unimplemented");
    }

    /**
     * Returns a String describing this descriptor. It should be of the form
     * "fieldType[0](fieldName[0]), ..., fieldType[M](fieldName[M])", although
     * the exact format does not matter.
     *
     * @return String describing this descriptor.
     */
    public String toString() {
        // some code goes here
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.itemAr.size(); i++) {
            sb.append(this.itemAr.get(i).fieldType.toString() + "[" + i + "](" + this.itemAr.get(i).fieldName + "[" + i + "]),");
        }
        return sb.toString().substring(0, sb.length() - 1);
    }
}
