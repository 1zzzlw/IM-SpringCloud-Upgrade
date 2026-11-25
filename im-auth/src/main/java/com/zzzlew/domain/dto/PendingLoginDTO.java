package com.zzzlew.domain.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PendingLoginDTO {
    @NotNull
    private Long userId;
}
