package simpledb;

import java.io.FileNotFoundException;
import java.util.*;

public class HeapFileIterator implements DbFileIterator {
    private final TransactionId tid;
    private final HeapFile heapFile;

    private int pageno = 0;
    private Iterator<Tuple> pageIterator = null;

    HeapFileIterator(TransactionId tid, HeapFile hf) {
        this.tid = tid;
        this.heapFile = hf;
    }

    public void open() throws DbException, TransactionAbortedException {
        pageno = 0;
        try {
            pageIterator = getHeapPage(pageno).iterator();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private HeapPage getHeapPage(int pageNo)
            throws DbException, TransactionAbortedException, FileNotFoundException {
        if (pageNo < 0 || pageNo >= heapFile.numPages()) {
            return null;
        }
        return (HeapPage) Database.getBufferPool()
                .getPage(tid, new HeapPageId(heapFile.getId(), pageNo), Permissions.READ_ONLY);
    }

    public boolean hasNext() throws DbException, TransactionAbortedException {
        if (pageIterator == null || pageno >= heapFile.numPages()) {
            return false;
        }
        while (!pageIterator.hasNext()) {
            pageno += 1;
            if (pageno >= heapFile.numPages()) {
                return false;
            }
            try {
                pageIterator = getHeapPage(pageno).iterator();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public Tuple next() throws DbException, TransactionAbortedException,
            NoSuchElementException {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return pageIterator.next();
    }

    public void rewind() throws DbException, TransactionAbortedException {
        open();
    }

    public void close() {
        pageno = 0;
        pageIterator = null;
    }
}