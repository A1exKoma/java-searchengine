package searchengine.dto;

import lombok.Data;

@Data
public class GeneralResponse {
    private boolean result;
    private String error;

    public GeneralResponse(boolean result, String error) {
        this.result = result;
        this.error = error;
    }

    public GeneralResponse(boolean result) {
        this.result = result;
    }
}