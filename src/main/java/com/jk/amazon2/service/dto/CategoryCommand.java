package com.jk.amazon2.service.dto;

import com.jk.amazon2.controller.dto.CategoryRequest;
import com.jk.amazon2.exception.CategoryErrorCode;
import com.jk.amazon2.exception.RestApiException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CategoryCommand {
    @Getter
    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Create {
        private String code;
        private String name;
        private String description;

        public static Create from(CategoryRequest.CategoryCreateDto dto) {
            return new Create(dto.code(), dto.name(), dto.description());
        }

        public static Create of(String code, String name, String description) {
            return new Create(code, name, description);
        }

        private Create(String code, String name, String description) {
            if(code == null || code.isBlank() || code.length() > 10) {
                log.warn("[VALIDATION_FAILED] CategoryCommand.Create - Invalid code. code={}", code);
                throw new RestApiException(CategoryErrorCode.CATEGORY_CODE_INVALID);
            }

            if(name == null || name.isBlank() || name.length() > 50) {
                log.warn("[VALIDATION_FAILED] CategoryCommand.Create - Invalid name. name={}", name);
                throw new RestApiException(CategoryErrorCode.CATEGORY_NAME_EMPTY);
            }

            if (description.length() > 50) {
                log.warn("[VALIDATION_FAILED] CategoryCommand.Create - Invalid description. description={}", description);
                throw new RestApiException(CategoryErrorCode.CATEGORY_DESCRIPTION_INVALID);
            }

            this.code = code;
            this.name = name;
            this.description = description;
        }
    }
}
