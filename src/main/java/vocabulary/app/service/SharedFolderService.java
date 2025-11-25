package vocabulary.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vocabulary.app.dto.SharedFolderDTO;
import vocabulary.app.entity.*;
import vocabulary.app.repository.SharedFolderRepository;
import vocabulary.app.repository.UserRepository;
import vocabulary.app.repository.WordFolderRepository;

import java.util.List;

@Service
public class SharedFolderService {
    private final SharedFolderRepository sharedFolderRepository;
    private final UserRepository userRepository;
    private final WordFolderRepository wordFolderRepository;

    public SharedFolderService(SharedFolderRepository sharedFolderRepository, UserRepository userRepository, WordFolderRepository wordFolderRepository){
        this.sharedFolderRepository = sharedFolderRepository;
        this.userRepository = userRepository;
        this.wordFolderRepository = wordFolderRepository;
    }

    @Transactional
    public SharedFolder addSharedFolder(SharedFolderDTO sharedFolderDTO){
        System.out.println(sharedFolderDTO.getClass());
        User user = userRepository.findById(sharedFolderDTO.getUserId()).orElseThrow();
        WordFolder wordFolder = wordFolderRepository.findById(sharedFolderDTO.getFolderId()).orElseThrow();
        return sharedFolderRepository.save(SharedFolder.create(sharedFolderDTO, user, wordFolder));
    }

    @Transactional
    public List<SharedFolder> getSharedFolders(){
        return sharedFolderRepository.findAll();
    }
    @Transactional
    public SharedFolder getDetailed(Long userId, Long folderId){
        return sharedFolderRepository.findById(new SharedFolderId(userId, folderId)).orElseThrow();
    }

    @Transactional
    public void deleteSharedFolder(Long userId, Long folderId){
        sharedFolderRepository.deleteById(new SharedFolderId(userId, folderId));
    }

    @Transactional
    public List<SharedFolder> getSharedFoldersByUser(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return sharedFolderRepository.findByUser(user);
    }
}
