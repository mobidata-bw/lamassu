package org.entur.lamassu.model.gbfs.v2_1;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class MultiPolygon {
    @JsonProperty("coordinates") List<List<List<List<Double>>>> coordinates;
}