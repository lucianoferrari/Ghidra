/* ###
 * IP: GHIDRA
 * REVIEWED: YES
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ghidra.util.datastruct;

import ghidra.util.exception.NoValueException;

/**
 *     Class that implements a hashtable with Object keys and long values.
 */

public class ObjectLongHashtable<T> {

    private ObjectKeyIndexer<T> indexer;    // allocates and manages index values for keys.
    private long[] values;           // array for holding the values.
    private int capacity;              // current capacity


    /**
     * Default constructor creates a table with an initial default capacity.
     */
    public ObjectLongHashtable() {
        this((short)3);
    }

    /**
     * Constructor creates a table with an initial given capacity.  The capacity
     * will be adjusted to the next highest prime in the PRIMES table.
     * @param capacity the initial capacity.
     */
    public ObjectLongHashtable(int capacity) {

        capacity = Prime.nextPrime(capacity);
        this.capacity = capacity;
        indexer = new ObjectKeyIndexer<T>(capacity);
        values = new long[capacity];
    }


    /**
     * Adds a key/value pair to the hashtable. If the key is already in the table,
     * the old value is replaced with the new value.  If the hashtable is already
     * full, the hashtable will attempt to approximately double in size
     * (it will use a prime number), and all the current entries will
     * be rehashed.
     * @param key the key to associate with the given value.
     * @param value the value to associate with the given key.
     * @exception ArrayIndexOutOfBoundsException thrown if the maximum capacity is
     * reached.
     */
    public void put(T key, long value) {

        int index = indexer.put(key);

        // make sure there is room
        if (index >= capacity) {
            grow();
        }

        values[index] = value;
    }

    /**
     * Returns the value for the given key.
     * @param key the key whose associated value is to be returned.
     * @exception NoValueException thrown if there is no value for the given key.
     */
    public long get(T key) throws NoValueException {
        int index = indexer.get(key);
        if (index < 0) {
            throw NoValueException.noValueException;
        }
        return values[index];
    }

    /**
     * Removes a key from the hashtable
     * @param key key to be removed from the hashtable.
     * @return true if key is found and removed, false otherwise.
     */
    public boolean remove(Object key) {
        if (indexer.remove(key) < 0) {
            return false;
        }
        return true;
    }

    /**
     * Remove all entries from the hashtable.
     */
    public void removeAll() {
        indexer.clear();
    }

	/**
	 * Return true if the given key is in the hashtable.
	 * @param key the key whose presence in this map is to be tested.
	 */
	public boolean contains(Object key) {
		return indexer.get(key) >=0;
	}

    /**
     * Return the number of key/value pairs stored in the hashtable.
     */
    public int size() {
        return indexer.getSize();
    }

    /**
     * Returns an array containing all the key objects.
     */
    public T[] getKeys(T[] keyArray) {
        return indexer.getKeys(keyArray);
    }

    /**
     * Resizes the hashtable to allow more entries.
     */
    private void grow() {

        capacity = indexer.getCapacity();

        long[] oldValues = values;

        values = new long[capacity];

        System.arraycopy(oldValues,0,values,0,oldValues.length);
    }

}
