package repository;

import model.User;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import jakarta.persistence.EntityNotFoundException;

public class InMemoryUserRepository implements UserRepository {

    private final Map<String, User> usersByUsername = new ConcurrentHashMap<>();
    private final Map<String, User> usersByPlayerId = new ConcurrentHashMap<>();
    private final Map<Long, User> usersById = new ConcurrentHashMap<>();
    private long currentId = 1L;

    public void clear() {
        usersByUsername.clear();
        usersByPlayerId.clear();
        usersById.clear();
        currentId = 1L;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return Optional.ofNullable(usersByUsername.get(username));
    }

    @Override
    public Optional<User> findByPlayerId(String playerId) {
        return Optional.ofNullable(usersByPlayerId.get(playerId));
    }

    @Override
    public boolean existsByUsername(String username) {
        return usersByUsername.containsKey(username);
    }

    @Override
    public boolean existsByPlayerId(String playerId) {
        return usersByPlayerId.containsKey(playerId);
    }

    @Override
    public <S extends User> S save(S entity) {
        if (entity.getId() == null) {
            entity.setId(currentId++);
        }
        usersByUsername.put(entity.getUsername(), entity);
        usersByPlayerId.put(entity.getPlayerId(), entity);
        usersById.put(entity.getId(), entity);
        return entity;
    }

    @Override
    public <S extends User> List<S> saveAll(Iterable<S> entities) {
        List<S> savedEntities = new ArrayList<>();
        for (S entity : entities) {
            savedEntities.add(save(entity));
        }
        return savedEntities;
    }

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(usersById.get(id));
    }

    @Override
    public boolean existsById(Long id) {
        return usersById.containsKey(id);
    }

    @Override
    public List<User> findAll() {
        return new ArrayList<>(usersById.values());
    }

    @Override
    public List<User> findAllById(Iterable<Long> longs) {
        List<User> foundUsers = new ArrayList<>();
        for (Long id : longs) {
            findById(id).ifPresent(foundUsers::add);
        }
        return foundUsers;
    }

    @Override
    public long count() {
        return usersById.size();
    }

    @Override
    public void deleteById(Long id) {
        User user = usersById.remove(id);
        if (user != null) {
            usersByUsername.remove(user.getUsername());
            usersByPlayerId.remove(user.getPlayerId());
        }
    }

    @Override
    public void delete(User entity) {
        deleteById(entity.getId());
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> longs) {
        for (Long id : longs) {
            deleteById(id);
        }
    }

    @Override
    public void deleteAll(Iterable<? extends User> entities) {
        for (User entity : entities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAll() {
        clear();
    }

    @Override
    public List<User> findAll(Sort sort) {
        throw new UnsupportedOperationException("Not implemented for in-memory test repository");
    }

    @Override
    public Page<User> findAll(Pageable pageable) {
        throw new UnsupportedOperationException("Not implemented for in-memory test repository");
    }

    @Override
    public <S extends User> Optional<S> findOne(Example<S> example) {
        throw new UnsupportedOperationException("Not implemented for in-memory test repository");
    }

    @Override
    public <S extends User> Page<S> findAll(Example<S> example, Pageable pageable) {
        throw new UnsupportedOperationException("Not implemented for in-memory test repository");
    }

    @Override
    public <S extends User> long count(Example<S> example) {
        throw new UnsupportedOperationException("Not implemented for in-memory test repository");
    }

    @Override
    public <S extends User> boolean exists(Example<S> example) {
        throw new UnsupportedOperationException("Not implemented for in-memory test repository");
    }

    @Override
    public <S extends User, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        throw new UnsupportedOperationException("Not implemented for in-memory test repository");
    }

    @Override
    public <S extends User> List<S> findAll(Example<S> example) {
        throw new UnsupportedOperationException("Not implemented for in-memory test repository");
    }

    @Override
    public <S extends User> List<S> findAll(Example<S> example, Sort sort) {
        throw new UnsupportedOperationException("Not implemented for in-memory test repository");
    }

    @Override
    public User getReferenceById(Long id) {
        return findById(id).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User with id " + id + " not found"));
    }

    @Override
    public User getById(Long id) {
        return findById(id).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User with id " + id + " not found"));
    }

    @Override
    public User getOne(Long id) {
        return findById(id).orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("User with id " + id + " not found"));
    }

    @Override
    public void deleteAllInBatch(Iterable<User> entities) {
        for (User entity : entities) {
            delete(entity);
        }
    }

    @Override
    public void deleteAllInBatch() {
        clear();
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> longs) {
        for (Long id : longs) {
            deleteById(id);
        }
    }

    @Override
    public <S extends User> List<S> saveAllAndFlush(Iterable<S> entities) {
        List<S> savedEntities = new ArrayList<>();
        for (S entity : entities) {
            savedEntities.add(save(entity));
        }
        return savedEntities;
    }

    @Override
    public <S extends User> S saveAndFlush(S entity) {
        return save(entity);
    }

    @Override
    public void flush() {
        // In-memory repository, no-op for flush
    }
}