package com.maxdemarzi;

import org.roaringbitmap.RoaringBitmap;

//// TODO: 1/5/17 Replace with https://github.com/gpunti/RoaringBitmap/tree/thread-safety once merged

public class ThreadSafeRoaringBitmap {
    final RoaringBitmap roaringBitmap;

    public ThreadSafeRoaringBitmap() {
        roaringBitmap = new RoaringBitmap();
    }

    public synchronized void clear() {
        roaringBitmap.clear();

    }

    public synchronized void add(int location) {
        roaringBitmap.add(location);
    }

    public synchronized boolean contains(int location) {
        return roaringBitmap.contains(location);
    }

    public synchronized boolean checkedAdd(int location) {
        return roaringBitmap.checkedAdd(location);
    }

}
