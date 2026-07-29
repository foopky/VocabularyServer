package vocabulary.app.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vocabulary.app.entity.Sentence;
import vocabulary.app.entity.User;
import vocabulary.app.entity.WordFolder;
import vocabulary.app.repository.SentenceRepository;
import vocabulary.app.repository.SharedFolderRepository;
import vocabulary.app.repository.UserRepository;
import vocabulary.app.repository.WordFolderRepository;

import java.util.NoSuchElementException;

// 요청한 리소스가 로그인한 사용자의 것인지 확인한다.
//
// 로그인만 하면 URL의 userId / folderId / sentenceId 만 바꿔서 남의 데이터를 조회·수정·삭제할 수 있었다.
// 컨트롤러마다 같은 검사를 반복하지 않도록 한곳에 모은다.
//
// 위반 시 AccessDeniedException을 던진다.
// - 반환 타입을 ResponseEntity로 바꾸지 않아도 되므로 기존 API 응답 형식이 그대로 유지된다.
// - GlobalExceptionHandler가 이 예외를 다시 던져 Spring Security의 기본 처리(403)로 넘긴다.
@Component
@RequiredArgsConstructor
public class OwnershipGuard {

    private final UserRepository userRepository;
    private final WordFolderRepository wordFolderRepository;
    private final SharedFolderRepository sharedFolderRepository;
    private final SentenceRepository sentenceRepository;

    // 현재 로그인한 사용자. JWT의 subject가 사용자 이름이므로 이름으로 조회한다.
    @Transactional(readOnly = true)
    public User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("인증이 필요합니다.");
        }
        return userRepository.findByName(authentication.getName())
                // 토큰은 유효한데 계정이 지워진 경우(탈퇴 직후 남은 Access Token 등)
                .orElseThrow(() -> new AccessDeniedException("존재하지 않는 사용자입니다."));
    }

    // 요청한 userId가 본인인지
    @Transactional(readOnly = true)
    public void checkUser(Long userId, Authentication authentication) {
        if (userId == null || !userId.equals(currentUser(authentication).getId())) {
            throw new AccessDeniedException("본인의 데이터에만 접근할 수 있습니다.");
        }
    }

    // 폴더의 주인인지 (수정 / 삭제 / 단어 추가)
    @Transactional(readOnly = true)
    public void checkFolderOwner(Long folderId, Authentication authentication) {
        if (!isFolderOwner(folderId, authentication)) {
            throw new AccessDeniedException("본인의 폴더에만 접근할 수 있습니다.");
        }
    }

    // 폴더를 열람할 수 있는지.
    // 주인이거나, 공유 게시판에 올라온 폴더면 허용한다. (공유 폴더 미리보기가 막히면 안 된다)
    @Transactional(readOnly = true)
    public void checkFolderReadable(Long folderId, Authentication authentication) {
        if (isFolderOwner(folderId, authentication)) {
            return;
        }
        if (sharedFolderRepository.countByFolderId(folderId) > 0) {
            return;
        }
        throw new AccessDeniedException("공유되지 않은 다른 사용자의 폴더입니다.");
    }

    // 예문의 주인인지
    @Transactional(readOnly = true)
    public void checkSentenceOwner(Long sentenceId, Authentication authentication) {
        Sentence sentence = sentenceRepository.findById(sentenceId)
                .orElseThrow(() -> new NoSuchElementException("예문을 찾을 수 없습니다: " + sentenceId));

        User owner = sentence.getUser();
        if (owner == null || !owner.getId().equals(currentUser(authentication).getId())) {
            throw new AccessDeniedException("본인의 예문에만 접근할 수 있습니다.");
        }
    }

    private boolean isFolderOwner(Long folderId, Authentication authentication) {
        WordFolder folder = wordFolderRepository.findById(folderId)
                .orElseThrow(() -> new NoSuchElementException("폴더를 찾을 수 없습니다: " + folderId));

        User owner = folder.getUser();
        return owner != null && owner.getId().equals(currentUser(authentication).getId());
    }
}
