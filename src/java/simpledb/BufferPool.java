package simpledb;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * BufferPool manages the reading and writing of pages into memory from
 * disk. Access methods call into it to retrieve pages, and it fetches
 * pages from the appropriate location.
 * <p>
 * The BufferPool is also responsible for locking;  when a transaction fetches
 * a page, BufferPool checks that the transaction has the appropriate
 * locks to read/write the page.
 *
 * @Threadsafe, all fields are final
 */
public class BufferPool {
    /** Bytes per page, including header. */
    private static final int DEFAULT_PAGE_SIZE = 4096;

    private static int pageSize = DEFAULT_PAGE_SIZE;

    /** Default number of pages passed to the constructor. This is used by
     other classes. BufferPool should use the numPages argument to the
     constructor instead. */
    public static final int DEFAULT_PAGES = 50;

    private Map<PageId, Page> cachedPages;
    int numPages;
    private LockerManager lockerManager;

    /**
     * Creates a BufferPool that caches up to numPages pages.
     *
     * @param numPages maximum number of pages in this buffer pool.
     */
    public BufferPool(int numPages) {
        // some code goes here
        this.numPages = numPages;
        this.cachedPages = new ConcurrentHashMap<>();
        this.lockerManager = new LockerManager();
    }

    public static int getPageSize() {
        return pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void setPageSize(int pageSize) {
        BufferPool.pageSize = pageSize;
    }

    // THIS FUNCTION SHOULD ONLY BE USED FOR TESTING!!
    public static void resetPageSize() {
        BufferPool.pageSize = DEFAULT_PAGE_SIZE;
    }

    /**
     * Retrieve the specified page with the associated permissions.
     * Will acquire a lock and may block if that lock is held by another
     * transaction.
     * <p>
     * The retrieved page should be looked up in the buffer pool.  If it
     * is present, it should be returned.  If it is not present, it should
     * be added to the buffer pool and returned.  If there is insufficient
     * space in the buffer pool, a page should be evicted and the new page
     * should be added in its place.
     *
     * @param tid the ID of the transaction requesting the page
     * @param pid the ID of the requested page
     * @param perm the requested permissions on the page
     */
    public Page getPage(TransactionId tid, PageId pid, Permissions perm)
            throws TransactionAbortedException, DbException, FileNotFoundException {
        // some code goes here
        lockerManager.acquireLock(tid, pid, perm);
        if (this.cachedPages.containsKey(pid)) {
            return cachedPages.get(pid);
        }
        int tableid = pid.getTableId();
        // if a new page is going to be cached which will exceed the default page size,
        // evict pages to make sure current size is smaller than the default page size
        Page newCachedPage = Database.getCatalog().getDatabaseFile(tableid).readPage(pid);
        while (this.cachedPages.size() >= numPages) {
            evictPage();
        }
        this.cachedPages.put(pid, newCachedPage);
        return newCachedPage;
    }

    /**
     * Releases the lock on a page.
     * Calling this is very risky, and may result in wrong behavior. Think hard
     * about who needs to call this and why, and why they can run the risk of
     * calling it.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param pid the ID of the page to unlock
     */
    public void releasePage(TransactionId tid, PageId pid) {
        // some code goes here
        // not necessary for lab1|lab2
        lockerManager.unlock(tid, pid);
    }

    /**
     * Release all locks associated with a given transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     */
    public void transactionComplete(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        transactionComplete(tid, true);
    }

    /** Return true if the specified transaction has a lock on the specified page */
    public boolean holdsLock(TransactionId tid, PageId p) {
        // some code goes here
        // not necessary for lab1|lab2
        return lockerManager.holdsLock(tid, p);
    }

    /**
     * Commit or abort a given transaction; release all locks associated to
     * the transaction.
     *
     * @param tid the ID of the transaction requesting the unlock
     * @param commit a flag indicating whether we should commit or abort
     */
    public void transactionComplete(TransactionId tid, boolean commit)
            throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        // if commit == true, commit
        // if commit == false, abort

        // when commit, flush dirty pages associated with the transaction to disk
        // when abort, revert any changes made by transaction by restoring the page to its on-disk state
        if (commit) {
            flushPages(tid);
        } else {
            restorePages(tid);
        }
        lockerManager.unlockAll(tid);

    }

    /**
     * Add a tuple to the specified table on behalf of transaction tid.  Will
     * acquire a write lock on the page the tuple is added to and any other
     * pages that are updated (Lock acquisition is not needed for lab2).
     * May block if the lock(s) cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid the transaction adding the tuple
     * @param tableId the table to add the tuple to
     * @param t the tuple to add
     */
    public void insertTuple(TransactionId tid, int tableId, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        // mark every modified page as dirty and insert the tuple calling heapfile
        HeapFile heapFile = (HeapFile) Database.getCatalog().getDatabaseFile(tableId);
        ArrayList<Page> pageList = heapFile.insertTuple(tid, t);
        for (Page page : pageList) {
            PageId pid = page.getId();
            page.markDirty(true, tid);
            this.cachedPages.put(pid, page);
        }
    }

    /**
     * Remove the specified tuple from the buffer pool.
     * Will acquire a write lock on the page the tuple is removed from and any
     * other pages that are updated. May block if the lock(s) cannot be acquired.
     *
     * Marks any pages that were dirtied by the operation as dirty by calling
     * their markDirty bit, and adds versions of any pages that have
     * been dirtied to the cache (replacing any existing versions of those pages) so
     * that future requests see up-to-date pages.
     *
     * @param tid the transaction deleting the tuple.
     * @param t the tuple to delete
     */
    public void deleteTuple(TransactionId tid, Tuple t)
            throws DbException, IOException, TransactionAbortedException {
        // some code goes here
        // not necessary for lab1
        // mark every modified page as dirty and delete the tuple calling heapfile
        PageId pid = t.getRecordId().pid;
        HeapFile heapFile = (HeapFile) Database.getCatalog().getDatabaseFile(pid.getTableId());
        ArrayList<Page> pageList = heapFile.deleteTuple(tid, t);
        for (Page page : pageList) {
            PageId ppid = page.getId();
            page.markDirty(true, tid);
            this.cachedPages.put(ppid, page);
        }
    }

    /**
     * Flush all dirty pages to disk.
     * NB: Be careful using this routine -- it writes dirty data to disk so will
     *     break simpledb if running in NO STEAL mode.
     */
    public synchronized void flushAllPages() throws IOException {
        // some code goes here
        // not necessary for lab1
        for (Page page : this.cachedPages.values()) {
            flushPage(page.getId());
            page.markDirty(false, null);
            page.setBeforeImage();
        }
    }

    /** Remove the specific page id from the buffer pool.
     Needed by the recovery manager to ensure that the
     buffer pool doesn't keep a rolled back page in its
     cache.

     Also used by B+ tree files to ensure that deleted pages
     are removed from the cache so they can be reused safely
     */
    public synchronized void discardPage(PageId pid) {
        // some code goes here
        // not necessary for lab1
        this.cachedPages.remove(pid);
    }

    /**
     * Flushes a certain page to disk
     * @param pid an ID indicating the page to flush
     */
    private synchronized  void flushPage(PageId pid) throws IOException {
        // some code goes here
        // not necessary for lab1
        HeapPage currPage = (HeapPage) this.cachedPages.get(pid);
        if (currPage == null) throw new IOException();
        HeapFile heapFile = (HeapFile) Database.getCatalog().getDatabaseFile(pid.getTableId());
        LogFile logFile = Database.getLogFile();
        if (currPage.isDirty() != null) {
            logFile.logWrite(currPage.isDirty(), currPage.getBeforeImage(), currPage);
            logFile.force();
        }
        // no force
        heapFile.writePage(currPage);
    }

    /** Write all pages of the specified transaction to disk.
     */
    public synchronized  void flushPages(TransactionId tid) throws IOException {
        // some code goes here
        // not necessary for lab1|lab2
        Iterator<Entry<PageId, Page>> iterator = this.cachedPages.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<PageId, Page> entry = iterator.next();
            PageId pid = entry.getKey();
            Page p = entry.getValue();
            if (p.isDirty() != null && p.isDirty().equals(tid)) {
                flushPage(pid);
                p.markDirty(false, null);
                p.setBeforeImage();
            }
        }
    }

    public synchronized void restorePages(TransactionId tid) throws IOException {
        Iterator<Entry<PageId, Page>> iterator = this.cachedPages.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<PageId, Page> entry = iterator.next();
            PageId pid = entry.getKey();
            Page p = entry.getValue();
            if (p.isDirty() != null && p.isDirty().equals(tid)) {
                cachedPages.put(pid, p.getBeforeImage());
            }
        }
    }

    /**
     * Discards a page from the buffer pool.
     * Flushes the page to disk to ensure dirty pages are updated on disk.
     */
    private synchronized  void evictPage() throws DbException {
        Iterator<Entry<PageId, Page>> iterator = this.cachedPages.entrySet().iterator();
        while (iterator.hasNext()) {
            if (this.cachedPages.size() < numPages) break;
            Entry<PageId, Page> entry = iterator.next();
            PageId pid = entry.getKey();
            Page p = entry.getValue();

            // steal: flush any page to disk
            if (p.isDirty() == null) {
                discardPage(pid);
            }
        }
        if (cachedPages.size() >= numPages) {
            throw new DbException("no space db");
        }
    }
}