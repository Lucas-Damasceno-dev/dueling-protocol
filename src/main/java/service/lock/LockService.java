package service.lock;

import org.springframework.stereotype.Service;

@Service
public class LockService {

    private volatile boolean isLocked = false;

    public synchronized boolean acquire() {
        if (!isLocked) {
            isLocked = true;
            return true;
        }
        return false;
    }

    public synchronized void release() {
        isLocked = false;
    }
}
