package vocabulary.app.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vocabulary.app.entity.User;
import vocabulary.app.repository.UserRepository;

import java.util.Optional;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    public UserService(UserRepository userRepository, BCryptPasswordEncoder passwordEncoder){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User getUser(Long userId){
        return userRepository.findById(userId).orElseThrow();
    }

    @Transactional
    public Optional<User> saveUser(User user) throws RuntimeException{
        String username = user.getName();
        if (userRepository.findByName(username).isPresent()) {
            throw new RuntimeException("The Username already exists.");
        }
        else {
            String encodedPassword = passwordEncoder.encode(user.getPassword());
            user.setPassword(encodedPassword);
            return Optional.of(userRepository.save(user));
        }
    }

    @Transactional
    public void deleteUser(Long userId){
        userRepository.deleteById(userId);
    }

}
