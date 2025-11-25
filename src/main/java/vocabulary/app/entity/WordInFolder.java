package vocabulary.app.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity @Getter @Setter @NoArgsConstructor
public class WordInFolder {
    @EmbeddedId
    private WordInFolderId id;

    @MapsId("wordId")
    @ManyToOne
    @JoinColumn(name = "word_id")
    private Word word;

    @MapsId("folderId")
    @ManyToOne
    @JoinColumn(name = "wordfolder_id")
    private WordFolder wordFolder;

    public static WordInFolder create(Word word, WordFolder wordFolder) {
        WordInFolder wordInFolder = new WordInFolder();
        wordInFolder.id = new WordInFolderId(word.getId(), wordFolder.getId());
        wordInFolder.word = word;
        wordInFolder.wordFolder = wordFolder;
        return wordInFolder;
    }
}
