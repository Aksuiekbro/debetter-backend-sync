package com.heliozz10.debetter.dto.util.media.out;

import lombok.Data;

import java.io.Serializable;

@Data
public class UrlView implements Serializable {
    private Long id;
    private String url;
}
