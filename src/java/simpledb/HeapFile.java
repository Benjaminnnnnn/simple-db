package simpledb;

import java.io.*;
import java.util.*;

/**
 * HeapFile is an implementation of a DbFile that stores a collection of tuples
 * in no particular order. Tuples are stored on pages, each of which is a fixed
 * size, and the file is simply a collection of those pages. HeapFile works
 * closely with HeapPage. The format of HeapPages is described in the HeapPage
 * constructor.
 *
 * @see simpledb.HeapPage#HeapPage
 * @author Sam Madden
 */
public class HeapFile implements DbFile {

    private final File f;
    private final TupleDesc td;

    /**
     * Constructs a heap file backed by the specified file.
     *
     * @param f
     *            the file that stores the on-disk backing store for this heap
     *            file.
     */
    public HeapFile(File f, TupleDesc td) {
        // some code goes here
        this.f = f;
        this.td = td;
    }

    /**
     * Returns the File backing this HeapFile on disk.
     *
     * @return the File backing this HeapFile on disk.
     */
    public File getFile() {
        // some code goes here
        return this.f;
    }

    /**
     * Returns an ID uniquely identifying this HeapFile. Implementation note:
     * you will need to generate this tableid somewhere to ensure that each
     * HeapFile has a "unique id," and that you always return the same value for
     * a particular HeapFile. We suggest hashing the absolute file name of the
     * file underlying the heapfile, i.e. f.getAbsoluteFile().hashCode().
     *
     * @return an ID uniquely identifying this HeapFile.
     */
    public int getId() {
        // some code goes here
        return this.f.getAbsoluteFile().hashCode();
    }

    /**
     * Returns the TupleDesc of the table stored in this DbFile.
     *
     * @return TupleDesc of this DbFile.
     */
    public TupleDesc getTupleDesc() {
        // some code goes here
        return this.td;
    }

    // see DbFile.java for javadocs
    public Page readPage(PageId pid) {
        // some code goes here
        try {
            if (pid.getPageNumber() == numPages()) {
                Page page = new HeapPage((HeapPageId) pid, HeapPage.createEmptyPageData());
                writePage(page);
                return page;
            } else {
                RandomAccessFile file = new RandomAccessFile(this.f, "r");
                long offset = (long) BufferPool.getPageSize() * pid.getPageNumber();
                file.seek(offset);
                byte[] data = new byte[BufferPool.getPageSize()];
                file.read(data);
                file.close();
                return new HeapPage((HeapPageId) pid, data);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("File not found");
        }
    }

    // see DbFile.java for javadocs
    public void writePage(Page page) throws IOException {
        // some code goes here
        // not necessary for lab1
        RandomAccessFile file = new RandomAccessFile(this.f, "rw");
        long offset = (long) BufferPool.getPageSize() * page.getId().getPageNumber();
        file.seek(offset);
        byte[] data = page.getPageData();
        file.write(data);
        file.close();
    }

    /**
     * Returns the number of pages in this HeapFile.
     */
    public int numPages() {
        return (int) this.f.length() / BufferPool.getPageSize();
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> insertTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // interact with the bufferpool
        // if there are pages in the heapfile
        // and there are empty slots in the heapfile
        // insertTuple directly
        // otherwise, create a new page and add it into heapfile and insert the tuple
        BufferPool bp = Database.getBufferPool();
        ArrayList<Page> pageList = new ArrayList<>();
        for (int i = 0; i < numPages(); i++) {
            PageId hid = new HeapPageId(getId(), i);
            HeapPage page = (HeapPage) bp.getPage(tid, hid, Permissions.READ_WRITE);
            if (page.getNumEmptySlots() > 0) {
                page.insertTuple(t);
                pageList.add(page);
                return pageList;
            }
        }
        HeapPage page = (HeapPage) bp.getPage(tid, new HeapPageId(getId(), numPages()), Permissions.READ_WRITE);
        page.insertTuple(t);
        pageList.add(page);
        return pageList;
    }

    // see DbFile.java for javadocs
    public ArrayList<Page> deleteTuple(TransactionId tid, Tuple t) throws DbException,
            TransactionAbortedException, FileNotFoundException {
        // some code goes here
        // interact with the bufferpool
        // remove the tuple directly based on the recordId
        ArrayList<Page> pageList = new ArrayList<>();
        PageId pid = t.getRecordId().getPageId();
        BufferPool bp = Database.getBufferPool();
        HeapPage page = (HeapPage) bp.getPage(tid, pid, Permissions.READ_WRITE);
        page.deleteTuple(t);
        return pageList;
    }

    // see DbFile.java for javadocs
    public DbFileIterator iterator(TransactionId tid) {
        // some code goes here
        return new HeapFileIterator(tid,this);
    }

}

