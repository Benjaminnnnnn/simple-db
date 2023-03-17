package simpledb;

import java.util.*;

public class LockerManager {

    Map<PageId, RWLock> lockTable;
    Map<TransactionId, Set<TransactionId>> depGraph;
    Map<TransactionId, Set<PageId>> transactionMap;

    public LockerManager() {
        lockTable = new HashMap<PageId, RWLock>();
        depGraph = new HashMap<TransactionId, Set<TransactionId>>();
        transactionMap = new HashMap<TransactionId, Set<PageId>>();
    }

    public void acquireLock(TransactionId tid, PageId pid, Permissions perm) throws TransactionAbortedException {
        RWLock lock;
        if (perm == Permissions.READ_ONLY) {
            synchronized (this) {
                lock = getOrDefaultLock(pid);
                if (lock.getOwners(tid)) return;
                if (!lock.holders().isEmpty() && lock.isExclusive()) {
                    depGraph.put(tid, lock.holders());
                    if (hasDeadLock(tid)) {
                        depGraph.remove(tid);
                        throw new TransactionAbortedException();
                    }
                }
            }
            lock.readLock(tid);
            synchronized (this) {
                depGraph.remove(tid);
                getOrDefaultTransactionPage(tid).add(pid);
            }
        } else if (perm == Permissions.READ_WRITE) {
            synchronized (this) {
                lock = getOrDefaultLock(pid);
                if (lock.isExclusive() && lock.getOwners(tid))
                    return;
                if (!lock.holders().isEmpty()){
                    depGraph.put(tid, lock.holders());
                    if (hasDeadLock(tid)) {
                        depGraph.remove(tid);
                        throw new TransactionAbortedException();
                    }
                }
            }
            lock.writeLock(tid);
            synchronized (this) {
                depGraph.remove(tid);
                getOrDefaultTransactionPage(tid).add(pid);
            }
        }
    }

    public synchronized void unlock(TransactionId tid, PageId pid) {
        if (!lockTable.containsKey(pid)) return;
        RWLock lock = lockTable.get(pid);
        lock.unlock(tid);
        transactionMap.get(tid).remove(pid);
    }

    public synchronized void unlockAll(TransactionId tid) {
        if (!transactionMap.containsKey(tid)) return;
        for (Object pageId: transactionMap.get(tid).toArray()) {
            unlock(tid, ((PageId) pageId));
        }
        transactionMap.remove(tid);
    }

    private RWLock getOrDefaultLock(PageId pageId) {
        if (!lockTable.containsKey(pageId))
            lockTable.put(pageId, new RWLock());
        return lockTable.get(pageId);
    }

    private Set<PageId> getOrDefaultTransactionPage(TransactionId tid) {
        if (!transactionMap.containsKey(tid))
            transactionMap.put(tid, new HashSet<PageId>());
        return transactionMap.get(tid);
    }

    private boolean hasDeadLock(TransactionId tid) {
        // bfs to find cycles in dependency graph
        Set<TransactionId> visited = new HashSet<TransactionId>();
        Queue<TransactionId> q = new LinkedList<TransactionId>();
        visited.add(tid);
        q.offer(tid);
        while (!q.isEmpty()) {
            TransactionId head = q.poll();
            if (!depGraph.containsKey(head)) continue;
            for (TransactionId neighbor: depGraph.get(head)) {
                if (neighbor.equals(head)) continue;

                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    q.offer(neighbor);
                } else {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean holdsLock(TransactionId tid, PageId pid) {
        return transactionMap.containsKey(tid) &&
                transactionMap.get(tid).contains(pid);
    }
}