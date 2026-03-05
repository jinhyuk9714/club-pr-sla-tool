package com.club.sla.sla;

public interface SchedulerLockService {

  boolean tryLock(String lockName);

  void unlock(String lockName);
}
