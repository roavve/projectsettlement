package com.example.mssqll.utiles.resonse;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    private Long warn ;
    private Long ok;
    private Long grandTotal;
    private Long countAll;

    public ApiResponse(boolean success, String message, T data, Long warn, Long ok, Long grandTotal,Long countAll) {
        this.success = success;
        this.message = message;
        this.data = data;
        this.warn = warn;
        this.ok = ok;
        this.grandTotal = grandTotal;
        this.countAll = countAll;
    }

    public ApiResponse(boolean success, String message, T data) {
        this.success = success;
        this.message = message;
        this.data = data;
    }

}