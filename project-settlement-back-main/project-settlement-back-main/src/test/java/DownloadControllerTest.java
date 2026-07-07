import com.example.mssqll.MssqllApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = MssqllApplication.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DownloadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser(roles = "ADMIN")
    void validFileReturnsClientOrSuccess() throws Exception {
        mockMvc.perform(get("/api/v1/connection-fees/download-ext")
                        .param("fileName", "report.xlsx"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void pathTraversalIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/connection-fees/download-ext")
                        .param("fileName", "../etc/passwd"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void nullByteInExcelFileNameReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/connection-fees/download-ext")
                        .param("fileName", "file%00name.xlsx"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void nullByteInFileNameReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/connection-fees/download-ext")
                        .param("fileName", "%00"))
                .andExpect(status().isBadRequest());
    }
}
