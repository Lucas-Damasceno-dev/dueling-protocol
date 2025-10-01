package service.lock;

import org.springframework.stereotype.Service;

import org.springframework.context.annotation.Profile;

@Profile("server")
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
