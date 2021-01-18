package org.entur.lamassu.service.impl;

import org.entur.lamassu.cache.VehicleCache;
import org.entur.lamassu.cache.VehicleSpatialIndex;
import org.entur.lamassu.model.Vehicle;
import org.entur.lamassu.service.VehiclesNearbyService;
import org.redisson.api.GeoOrder;
import org.redisson.api.GeoUnit;
import org.redisson.api.RGeo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class VehiclesNearbyServiceImpl implements VehiclesNearbyService {

    @Autowired
    VehicleSpatialIndex spatialIndex;

    @Autowired
    VehicleCache vehicleCache;

    @Override
    public List<Vehicle> getVehiclesNearby(Double longitude, Double latitude, Double range, Integer count) {
        List<String> vehicleIds = spatialIndex.radius(longitude, latitude, range, GeoUnit.METERS, GeoOrder.ASC, count);
        Set<String> keys = new HashSet<>(vehicleIds);
        return vehicleCache.getAll(keys);
    }
}
