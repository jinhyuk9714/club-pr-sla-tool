package com.club.sla.sla;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.springframework.stereotype.Service;

@Service
public class PostgresAdvisoryLockService implements SchedulerLockService {

  private static final String TRY_LOCK_SQL = "select pg_try_advisory_lock(?)";
  private static final String UNLOCK_SQL = "select pg_advisory_unlock(?)";

  private final DataSource dataSource;
  private final Map<String, Connection> activeLockConnections = new ConcurrentHashMap<>();

  public PostgresAdvisoryLockService(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public boolean tryLock(String lockName) {
    if (activeLockConnections.containsKey(lockName)) {
      return false;
    }

    Connection connection = null;
    try {
      connection = dataSource.getConnection();
      long lockKey = toLockKey(lockName);
      boolean locked = queryBoolean(connection, TRY_LOCK_SQL, lockKey);
      if (!locked) {
        closeQuietly(connection);
        return false;
      }

      Connection existing = activeLockConnections.putIfAbsent(lockName, connection);
      if (existing != null) {
        queryBoolean(connection, UNLOCK_SQL, lockKey);
        closeQuietly(connection);
        return false;
      }

      return true;
    } catch (SQLException ex) {
      closeQuietly(connection);
      throw new IllegalStateException("failed to acquire advisory lock: " + lockName, ex);
    }
  }

  @Override
  public void unlock(String lockName) {
    Connection connection = activeLockConnections.remove(lockName);
    if (connection == null) {
      return;
    }

    try {
      queryBoolean(connection, UNLOCK_SQL, toLockKey(lockName));
    } catch (SQLException ex) {
      throw new IllegalStateException("failed to release advisory lock: " + lockName, ex);
    } finally {
      closeQuietly(connection);
    }
  }

  private boolean queryBoolean(Connection connection, String sql, long lockKey)
      throws SQLException {
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setLong(1, lockKey);
      try (ResultSet resultSet = statement.executeQuery()) {
        if (!resultSet.next()) {
          return false;
        }
        return resultSet.getBoolean(1);
      }
    }
  }

  private long toLockKey(String lockName) {
    if (lockName == null || lockName.isBlank()) {
      throw new IllegalArgumentException("lockName must not be blank");
    }
    return Integer.toUnsignedLong(lockName.hashCode());
  }

  private void closeQuietly(Connection connection) {
    if (connection == null) {
      return;
    }
    try {
      connection.close();
    } catch (SQLException ignored) {
      // no-op
    }
  }
}
