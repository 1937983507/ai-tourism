package com.example.aitourism.entity;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class POI {
    private Long id;
    private String poiName;
    private String cityName;
    private String poiDescription;
    private Float poiLongitude;
    private Float poiLatitude;
    private Integer poiRankInCity;
    private Integer poiRankInChina;
    private LocalDateTime createdTime;
    private LocalDateTime modifyTime;
}

