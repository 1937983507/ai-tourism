package com.example.aitourism.mapper;

import com.example.aitourism.entity.POI;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface POIMapper {

    @Select("SELECT * FROM t_poi WHERE city_name = #{cityName} ORDER BY poi_rankInCity ASC LIMIT #{limit}")
    List<POI> findByCityName(@Param("cityName") String cityName, @Param("limit") Integer limit);
}

