package vocabulary.app.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vocabulary.app.dto.UserSignupRequestDTO;
import vocabulary.app.dto.UserUpdateRequestDTO;
import vocabulary.app.entity.Role;
import vocabulary.app.entity.User;
import vocabulary.app.repository.*;

@Service
public class UserService {

    // 비밀번호 최소 길이
    public static final int MIN_PASSWORD_LENGTH = 4;

    // 비밀번호 변경 결과. 컨트롤러가 이 값을 HTTP 응답으로 옮긴다.
    public enum PasswordChangeResult {
        SUCCESS,
        INVALID_PASSWORD, // 현재 비밀번호 불일치
        WEAK_PASSWORD     // 새 비밀번호가 정책 미달
    }

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SharedFolderRepository sharedFolderRepository;
    private final WordInFolderRepository wordInFolderRepository;
    private final WordFolderRepository wordFolderRepository;
    private final SentenceRepository sentenceRepository;

    public UserService(UserRepository userRepository,
                       BCryptPasswordEncoder passwordEncoder,
                       RefreshTokenRepository refreshTokenRepository,
                       SharedFolderRepository sharedFolderRepository,
                       WordInFolderRepository wordInFolderRepository,
                       WordFolderRepository wordFolderRepository,
                       SentenceRepository sentenceRepository){
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokenRepository = refreshTokenRepository;
        this.sharedFolderRepository = sharedFolderRepository;
        this.wordInFolderRepository = wordInFolderRepository;
        this.wordFolderRepository = wordFolderRepository;
        this.sentenceRepository = sentenceRepository;
    }

    @Transactional
    public User getUser(Long userId){
        return userRepository.findById(userId).orElseThrow();
    }

    @Transactional
    public User saveUser(UserSignupRequestDTO request) throws RuntimeException{
        String username = request.getName();
        if (username == null || username.isBlank()) {
            throw new RuntimeException("Username is required.");
        }
        if (request.getPassword() == null || request.getPassword().length() < MIN_PASSWORD_LENGTH) {
            throw new RuntimeException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters.");
        }
        if (userRepository.findByName(username).isPresent()) {
            throw new RuntimeException("The Username already exists.");
        }

        User user = new User();
        user.setName(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDescription(request.getDescription());
        // 권한은 클라이언트가 정하지 못한다. 가입은 항상 USER.
        user.setRole(Role.USER);

        return userRepository.save(user);
    }

    // 프로필 수정. 비밀번호/권한/이름은 여기서 건드리지 않는다.
    @Transactional
    public User editUser(Long userId, UserUpdateRequestDTO request) throws RuntimeException{
        User user = getUser(userId);
        user.setDescription(request.getDescription());
        return userRepository.save(user);
    }

    // 비밀번호 변경. 반드시 현재 비밀번호를 확인하고, 새 비밀번호는 해시해서 저장한다.
    @Transactional
    public PasswordChangeResult changePassword(Long userId, String currentPassword, String newPassword){
        User user = getUser(userId);

        if (newPassword == null || newPassword.length() < MIN_PASSWORD_LENGTH) {
            return PasswordChangeResult.WEAK_PASSWORD;
        }
        if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
            return PasswordChangeResult.INVALID_PASSWORD;
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 비밀번호를 바꾸는 이유는 대개 탈취 의심이다.
        // Refresh Token을 남겨두면 공격자가 유효기간(기본 14일) 동안 재발급을 계속할 수 있으므로 전부 폐기한다.
        // (이미 발급된 Access Token은 만료까지 유효하다. 그래서 Access Token 수명을 짧게 둔다)
        refreshTokenRepository.deleteAllByUserId(userId);

        return PasswordChangeResult.SUCCESS;
    }

    // 회원 탈퇴.
    // users 행을 참조하는 자식 행이 남아 있으면 FK 제약으로 삭제가 실패한다.
    // 벌크 삭제로 순서를 강제해서 자식 -> 부모 순으로 지운다. (파생 삭제는 flush 시점에 순서가 보장되지 않는다)
    @Transactional
    public void deleteUser(Long userId){
        getUser(userId); // 없으면 NoSuchElementException -> 404

        // 1. 이 사용자의 폴더를 가리키는 공유 글 (공유한 사람이 남일 수 있다)
        sharedFolderRepository.deleteAllByFolderOwnerId(userId);
        // 2. 이 사용자가 공유한 글
        sharedFolderRepository.deleteAllByUserId(userId);
        // 3. 이 사용자의 폴더에 담긴 단어 매핑 (words 자체는 공용이라 남긴다)
        wordInFolderRepository.deleteAllByFolderOwnerId(userId);
        // 4. 이 사용자의 폴더
        wordFolderRepository.deleteAllByUserId(userId);
        // 5. 이 사용자가 만든 예문
        sentenceRepository.deleteAllByUserId(userId);
        // 6. 이 사용자의 Refresh Token
        refreshTokenRepository.deleteAllByUserId(userId);

        // 위 벌크 삭제들이 영속성 컨텍스트를 비우므로 id로 다시 조회해서 지운다.
        userRepository.deleteById(userId);
        userRepository.flush(); // 실패하면 커밋 시점이 아니라 여기서 예외가 나도록
    }
}
