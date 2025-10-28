package vocabulary.app.controller;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import vocabulary.app.service.WordService;

@WebMvcTest(controllers = {WordController.class})
@ActiveProfiles("locals")
public class WordControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WordService wordService;

//    @Test

}
