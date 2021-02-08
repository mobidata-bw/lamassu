package org.entur.lamassu.util;

import org.entur.lamassu.cache.SpatialIndexId;
import org.entur.lamassu.model.feedprovider.FeedProvider;
import org.entur.lamassu.model.entities.FormFactor;
import org.entur.lamassu.model.entities.PropulsionType;
import org.entur.lamassu.model.entities.Vehicle;
import org.entur.lamassu.model.entities.VehicleType;
import org.entur.lamassu.service.VehicleFilterParameters;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class VehicleFilterTest {

    @Test
    public void testNoFilter() {
        Assert.assertTrue(
                VehicleFilter.filterVehicle(testId(), testFilterParams())
        );
    }

    @Test
    public void testOperatorFilter() {
        var testId = testId();
        var params = testFilterParams();

        params.setOperators(List.of("testprovider"));
        Assert.assertTrue(
                VehicleFilter.filterVehicle(testId, params)
        );

        params.setOperators(List.of("foobar"));
        Assert.assertFalse(
                VehicleFilter.filterVehicle(testId, params)
        );
    }

    @Test
    public void testCodespaceFilter() {
        var testId = testId();
        var params = testFilterParams();

        params.setCodespaces(List.of("TST"));
        Assert.assertTrue(
                VehicleFilter.filterVehicle(testId, params)
        );

        params.setCodespaces(List.of("FOO"));
        Assert.assertFalse(
                VehicleFilter.filterVehicle(testId, params)
        );
    }

    @Test
    public void testFormFactorFilter() {
        var testId = testId();
        var params = testFilterParams();

        params.setFormFactors(List.of(FormFactor.SCOOTER));
        Assert.assertTrue(
                VehicleFilter.filterVehicle(testId, params)
        );

        params.setFormFactors(List.of(FormFactor.BICYCLE));
        Assert.assertFalse(
                VehicleFilter.filterVehicle(testId, params)
        );
    }

    @Test
    public void testPropulsionTypeFilter() {
        var testId = testId();
        var params = testFilterParams();

        params.setPropulsionTypes(List.of(PropulsionType.ELECTRIC));
        Assert.assertTrue(
                VehicleFilter.filterVehicle(testId, params)
        );

        params.setPropulsionTypes(List.of(PropulsionType.COMBUSTION));
        Assert.assertFalse(
                VehicleFilter.filterVehicle(testId, params)
        );
    }

    @Test
    public void testIncludeReservedFilter() {
        var testId = testReservedId();
        var params = testFilterParams();

        params.setIncludeReserved(true);
        Assert.assertTrue(
                VehicleFilter.filterVehicle(testId, params)
        );

        params.setIncludeReserved(false);
        Assert.assertFalse(
                VehicleFilter.filterVehicle(testId, params)
        );
    }

    @Test
    public void testIncludeDisabledFilter() {
        var testId = testDisabledId();
        var params = testFilterParams();

        params.setIncludeDisabled(true);
        Assert.assertTrue(
                VehicleFilter.filterVehicle(testId, params)
        );

        params.setIncludeDisabled(false);
        Assert.assertFalse(
                VehicleFilter.filterVehicle(testId, params)
        );
    }

    private SpatialIndexId testId() {
        return SpatialIndexId.fromString(SpatialIndexIdUtil.createSpatialIndexId(testVehicle(), testProvider()));
    }

    private SpatialIndexId testReservedId() {
        var vehicle = testVehicle();
        vehicle.setReserved(true);
        return SpatialIndexId.fromString(SpatialIndexIdUtil.createSpatialIndexId(vehicle, testProvider()));
    }

    private SpatialIndexId testDisabledId() {
        var vehicle = testVehicle();
        vehicle.setDisabled(true);
        return SpatialIndexId.fromString(SpatialIndexIdUtil.createSpatialIndexId(vehicle, testProvider()));
    }

    private Vehicle testVehicle() {
        var vehicle = new Vehicle();
        vehicle.setId("TST:Vehicle:abc123");
        vehicle.setReserved(false);
        vehicle.setDisabled(false);
        vehicle.setVehicleType(scooterVehicle());
        return vehicle;
    }

    private VehicleType scooterVehicle() {
        var type = new VehicleType();
        type.setId("TST:VehicleType:Scooter");
        type.setFormFactor(FormFactor.SCOOTER);
        type.setPropulsionType(PropulsionType.ELECTRIC);
        return type;
    }

    private FeedProvider testProvider() {
        var provider = new FeedProvider();
        provider.setName("testprovider");
        provider.setCodespace("TST");
        return provider;
    }

    private VehicleFilterParameters testFilterParams() {
        var params = new VehicleFilterParameters();
        params.setIncludeReserved(false);
        params.setIncludeDisabled(false);
        return params;
    }
}
