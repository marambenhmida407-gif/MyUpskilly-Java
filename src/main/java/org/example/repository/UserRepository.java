package org.example.repository;

import org.example.entity.User;
import java.util.List;

public interface UserRepository {
    List<User> findAll();
    User findById(int id);
    boolean emailExists(String email, int excludeId);
    void save(User user);
    void update(User user);
    void delete(int id);
}