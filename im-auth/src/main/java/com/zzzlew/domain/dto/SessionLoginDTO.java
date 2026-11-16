package com.zzzlew.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SessionLoginDTO {
    @NotNull
    private Long userId;
}
