package vocabulary.app.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import vocabulary.app.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByName(String username);
}
