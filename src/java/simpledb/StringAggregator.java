package simpledb;

import java.util.*;

/**
 * Knows how to compute some aggregate over a set of StringFields.
 */
public class StringAggregator implements Aggregator {

    private static final long serialVersionUID = 1L;


    private int gbfield, afield;
    private Type gbfieldtype;
    private Op what;
    private HashMap<Field, List<Tuple>> groups;
    private TupleDesc td;
    /**
     * Aggregate constructor
     * @param gbfield the 0-based index of the group-by field in the tuple, or NO_GROUPING if there is no grouping
     * @param gbfieldtype the type of the group by field (e.g., Type.INT_TYPE), or null if there is no grouping
     * @param afield the 0-based index of the aggregate field in the tuple
     * @param what aggregation operator to use -- only supports COUNT
     * @throws IllegalArgumentException if what != COUNT
     */
    public StringAggregator(int gbfield, Type gbfieldtype, int afield, Op what) throws IllegalArgumentException {
        // some code goes here
        if (what != Op.COUNT) {
            throw new IllegalArgumentException("String type does not support this aggregation operation");
        }
        this.gbfield = gbfield;
        this.gbfieldtype = gbfieldtype;
        this.afield = afield;
        this.what = what;
        this.groups = new HashMap<>();
        this.td = null;
    }

    /**
     * Merge a new tuple into the aggregate, grouping as indicated in the constructor
     * @param tup the Tuple containing an aggregate field and a group-by field
     */
    public void mergeTupleIntoGroup(Tuple tup) {
        // some code goes here
        if (td == null) {
            Type[] typeAr = new Type[]{gbfieldtype,
                    Type.INT_TYPE};
            String[] fieldAr = new String[]{gbfield == Aggregator.NO_GROUPING ? "" : tup.getTupleDesc().getFieldName(gbfield),
                    tup.getTupleDesc().getFieldName(afield)};
            td = new TupleDesc(typeAr, fieldAr);
        }

        Field gbkey = tup.getField(gbfield);
        Field akey = tup.getField(afield);

        Tuple t = new Tuple(td);
        t.setField(0, gbkey);
        t.setField(1, akey);

        List<Tuple> akeys = groups.getOrDefault(gbkey, new ArrayList<>());
        akeys.add(t);
        groups.put(gbkey, akeys);
    }

    /**
     * Create a OpIterator over group aggregate results.
     *
     * @return a OpIterator whose tuples are the pair (groupVal,
     *   aggregateVal) if using group, or a single (aggregateVal) if no
     *   grouping. The aggregateVal is determined by the type of
     *   aggregate specified in the constructor.
     */
    public OpIterator iterator() {
        // some code goes here
        return new StringAggregateIterator(groups, td, what, gbfield == Aggregator.NO_GROUPING);
    }

    public static class StringAggregateIterator implements OpIterator {
        TupleDesc td;
        Iterator<Field> it;
        HashMap<Field, List<Tuple>> groups;
        Op what;
        boolean noGroup;

        public StringAggregateIterator(HashMap<Field, List<Tuple>> groups, TupleDesc td, Op what, boolean noGroup) {
            this.groups = groups;
            this.td = td;
            this.what = what;
            this.noGroup = noGroup;
        }

        @Override
        public void open() throws DbException, TransactionAbortedException {
            it = groups.keySet().iterator();
        }

        @Override
        public boolean hasNext() throws DbException, TransactionAbortedException {
            return it.hasNext();
        }

        @Override
        public Tuple next() throws DbException, TransactionAbortedException, NoSuchElementException {
            Field gbkey = it.next();
            List<Tuple> tuples = groups.get(gbkey);
            Tuple result;

            if (noGroup) {
                Type[] typeAr = new Type[]{ td.getFieldType(1) };
                String[] fieldAr = new String[]{ td.getFieldName(1) };
                TupleDesc noGroupTd = new TupleDesc(typeAr, fieldAr);
                result = new Tuple(noGroupTd);

                if (what == Op.COUNT) {
                    result.setField(0, new IntField(tuples.size()));
                } else if (what == Op.SUM) {
                    int sum = 0;
                    for (Tuple t: tuples) {
                        sum += ((IntField)t.getField(1)).getValue();
                    }
                    result.setField(0, new IntField(sum));
                }
            } else {
                result = new Tuple(td);
                result.setField(0, gbkey);

                if (what == Op.COUNT) {
                    result.setField(1, new IntField(tuples.size()));
                } else if (what == Op.SUM) {
                    int sum = 0;
                    for (Tuple t: tuples) {
                        sum += ((IntField)t.getField(1)).getValue();
                    }
                    result.setField(1, new IntField(sum));
                }
            }
            return result;
        }

        @Override
        public void rewind() throws DbException, TransactionAbortedException {
            close();
            open();
        }

        @Override
        public TupleDesc getTupleDesc() {
            return td;
        }

        @Override
        public void close() {
            it = null;
        }
    }

}
