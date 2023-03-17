

package simpledb;

import java.util.*;

public class RWLock {
    Set<TransactionId> holders;
    Map<TransactionId, Boolean> acquirers;
    boolean exclusive;
    private int readRef; // read reference number
    private int writeRef; // write reference number

    public RWLock() {
        holders = new HashSet<TransactionId>();
        acquirers = new HashMap<TransactionId, Boolean>();
        exclusive = false;
        readRef = 0;
        writeRef = 0;
    }

    public void readLock(TransactionId tid) {
        if (holders.contains(tid) && !exclusive) return;
        acquirers.put(tid, false);
        synchronized (this) {
            try {
                while (writeRef != 0) this.wait();
                ++readRef;
                holders.add(tid);
                exclusive = false;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        acquirers.remove(tid);
    }

    public void writeLock(TransactionId tid) {
        if (holders.contains(tid) && exclusive) return;
        if (acquirers.containsKey(tid) && acquirers.get(tid)) return;
        acquirers.put(tid, true);
        synchronized (this) {
            try {
                if (holders.contains(tid)) {
                    while (holders.size() > 1) {
                        this.wait();
                    }
                    readUnlockWithoutNotifyingAll(tid);
                }
                while (readRef != 0 || writeRef != 0) this.wait();
                ++writeRef;
                holders.add(tid);
                exclusive = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        acquirers.remove(tid);
    }

    private void readUnlockWithoutNotifyingAll(TransactionId tid) {
        if (!holders.contains(tid)) return;
        synchronized (this) {
            --readRef;
            holders.remove(tid);
        }
    }

    public void readUnlock(TransactionId tid) {
        if (!holders.contains(tid)) return;
        synchronized (this) {
            --readRef;
            holders.remove(tid);
            notifyAll();
        }
    }

    public void writeUnlock(TransactionId tid) {
        if (!holders.contains(tid)) return;
        if (!exclusive) return;
        synchronized (this) {
            --writeRef;
            holders.remove(tid);
            notifyAll();
        }
    }

    public void unlock(TransactionId tid) {
        if (!exclusive)
            readUnlock(tid);
        else writeUnlock(tid);
    }

    public Set<TransactionId> holders() {
        return holders;
    }

    public boolean isExclusive() {
        return exclusive;
    }

    public boolean getOwners(TransactionId tid) {
        return holders().contains(tid);
    }
}