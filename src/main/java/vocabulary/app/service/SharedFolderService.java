package vocabulary.app.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vocabulary.app.dto.SharedFolderDTO;
import vocabulary.app.entity.SharedFolder;
import vocabulary.app.entity.SharedFolderId;
import vocabulary.app.repository.SharedFolderRepository;

@Service
public class SharedFolderService {
    private final SharedFolderRepository sharedFolderRepository;

    public SharedFolderService(SharedFolderRepository sharedFolderRepository){
        this.sharedFolderRepository = sharedFolderRepository;
    }

    @Transactional
    public SharedFolder addSharedFolder(Long userId, Long folderId){
        return sharedFolderRepository.save(SharedFolder.create(userId, folderId));
    }

    @Transactional
    public SharedFolderDTO[] getSharedFolders(){
        return sharedFolderRepository.getALLDetailedFolder();
    }
    @Transactional
    public SharedFolderDTO getDetailed(Long userId, Long folderId){
        sharedFolderRepository.findById(new SharedFolderId(userId, folderId)).orElseThrow();
        return sharedFolderRepository.getDetailedFolder(userId, folderId);
    }

    @Transactional
    public void deleteSharedFolder(Long userId, Long folderId){
        sharedFolderRepository.deleteById(new SharedFolderId(userId, folderId));
    }
}
