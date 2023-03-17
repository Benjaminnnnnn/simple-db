package simpledb;
/**
 * Class representing a type in SimpleDB.
 * Types are static objects defined by this class; hence, the Type
 * constructor is private.
 */

public enum LockType {
    SHARED_LOCK,
    EXCLUSIVE_LOCK,
}
